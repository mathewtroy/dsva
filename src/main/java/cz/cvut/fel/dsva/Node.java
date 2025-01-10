package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.DSNeighbours;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Node class implementing the Bully Leader Election Algorithm with dynamic IP assignment.
 *
 * Usage:
 *   - If you provide 2 args: nodeId, myIP -> the node starts alone
 *   - If you provide 4 args: nodeId, myIP, otherNodeIP, otherNodePort -> auto-join that neighbor
 */
@Slf4j
@Getter
@Setter
public class Node implements Runnable {

    public static final String COMM_INTERFACE_NAME = "DSVNode";
    public static Node thisNode = null;

    // ----------------------------------------------------------
    // Internal flags & Bully-specific states
    // ----------------------------------------------------------
    private boolean isKilled = false;
    private boolean isLeft = false;
    private boolean isCoordinator = false;
    private boolean receivedOK = false;
    private boolean voting = false;

    // ----------------------------------------------------------
    // Basic info
    // ----------------------------------------------------------
    private String nickname = "Unknown";
    private String myIP;             // Provided as arg
    private int rmiPort = 3010;      // Adjusted from nodeId
    private int apiPort = 7010;      // Adjusted from nodeId
    private long nodeId = 0;         // Unique numeric ID

    // Additional info for optional auto-join
    private String otherNodeIP;      // If you pass extra args
    private int otherNodePort = 0;

    // ----------------------------------------------------------
    // Node structure
    // ----------------------------------------------------------
    private Address address;
    private DSNeighbours myNeighbours;
    private MessageReceiver myMessageReceiver;
    private CommunicationHub myCommHub;
    private ConsoleHandler myConsoleHandler;
    private APIHandler myAPIHandler;
    private BullyAlgorithm bully;

    /**
     * Constructor that expects:
     *   Minimum: args[0] = nodeId, args[1] = myIP
     *   Optional: args[2] = otherNodeIP, args[3] = otherNodePort
     */
    public Node(String[] args) {
        if (args.length >= 2) {
            try {
                this.nodeId = Long.parseLong(args[0]);
                this.myIP   = args[1];

                // Adjust ports
                this.rmiPort = 50050 + (int) nodeId;
                this.apiPort = 7000   + (int) nodeId;

                // If user provided 4 arguments, let's store them for an auto-join
                if (args.length >= 4) {
                    this.otherNodeIP   = args[2];
                    this.otherNodePort = Integer.parseInt(args[3]);
                }

                // Initialize neighbors
                myNeighbours = new DSNeighbours();

                log.info("Node initialized with ID {} on ports RMI: {}, API: {}. My IP = {}",
                        nodeId, rmiPort, apiPort, myIP);

                if (otherNodeIP != null && otherNodePort > 0) {
                    log.info("If run() calls auto-join, we will join {}:{}", otherNodeIP, otherNodePort);
                }

            } catch (NumberFormatException e) {
                log.error("Invalid node ID or port: {}", args[0], e);
                System.exit(1);
            }
        } else {
            log.error("At least 2 arguments are required: <nodeId> <myIP> [otherNodeIP otherNodePort]");
            System.exit(1);
        }
    }

    // ----------------------------------------------------------
    // RMI Setup/Shutdown
    // ----------------------------------------------------------
    private void startMessageReceiver() {
        address = new Address(myIP, rmiPort, nodeId);

        System.setProperty("java.rmi.server.hostname", address.getHostname());

        try {
            if (this.myMessageReceiver == null) {
                this.myMessageReceiver = new MessageReceiver(this);
            }
            NodeCommands skeleton =
                    (NodeCommands) UnicastRemoteObject.exportObject(this.myMessageReceiver, rmiPort);

            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(rmiPort);
                registry.rebind(COMM_INTERFACE_NAME, skeleton);
            } catch (RemoteException re) {
                log.info("Creating new RMI registry on port {}.", rmiPort);
                registry = LocateRegistry.createRegistry(rmiPort);
                registry.rebind(COMM_INTERFACE_NAME, skeleton);
            }
        } catch (Exception e) {
            log.error("Starting message listener failed: {}", e.getMessage(), e);
        }
        log.info("Message listener started on {}:{}.", address.getHostname(), rmiPort);
    }

    private void stopMessageReceiver() {
        try {
            if (this.myMessageReceiver != null) {
                Registry registry = LocateRegistry.getRegistry(rmiPort);
                try {
                    registry.unbind(COMM_INTERFACE_NAME);
                } catch (NotBoundException e) {
                    log.warn("RMI object was not bound: {}", e.getMessage());
                }
                UnicastRemoteObject.unexportObject(this.myMessageReceiver, true);
                this.myMessageReceiver = null;
                log.info("Message listener stopped for Node {}.", nodeId);
            }
        } catch (Exception e) {
            log.error("Stopping message listener failed: {}", e.getMessage(), e);
        }
        log.info("Message listener stopped.");
    }

    // ----------------------------------------------------------
    // NEW: join(...) for dynamic or auto connections
    // ----------------------------------------------------------
    /**
     * Attempt to join another node at (otherIP, otherPort).
     */
    public void join(String otherIP, int otherPort) {
        if (isKilled || isLeft) {
            log.info("Node {} is inactive. Cannot join another node.", nodeId);
            return;
        }
        log.info("Node {} attempting to join node at {}:{}", nodeId, otherIP, otherPort);

        try {
            Address otherAddr = new Address(otherIP, otherPort, -1L);
            // getRMIProxy(...) returns a NodeCommands
            NodeCommands remoteNode = myCommHub.getRMIProxy(otherAddr);

            // If remoteNode.join(...) returns void,
            // we simply call it, no DSNeighbours assignment:
            remoteNode.join(this.address);

            log.info("Join call invoked (void). Node {} asked {}:{} to join.",
                    nodeId, otherIP, otherPort);

            // Optionally, if you want to add that node yourself:
            myNeighbours.addNewNode(otherAddr);

        } catch (RemoteException e) {
            log.warn("Failed to join node at {}:{} => {}", otherIP, otherPort, e.getMessage());
        }
    }


    // ----------------------------------------------------------
    // Misc Utility & Status
    // ----------------------------------------------------------
    public String getStatus() {
        if (isKilled) {
            return "Node " + nodeId + " status: KILLED (no RMI)\n";
        }
        if (isLeft) {
            return "Node " + nodeId + " status: LEFT the network (no RMI)\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Node ID: ").append(nodeId).append("\n");
        sb.append("Address: ").append(address).append("\n");
        sb.append("Neighbours:\n");
        for (Address neighbour : myNeighbours.getNeighbours()) {
            sb.append("    ").append(neighbour).append("\n");
        }
        if (myNeighbours.isLeaderPresent()) {
            Address leader = myNeighbours.getLeaderNode();
            sb.append("Leader: Node ID ").append(leader.getNodeID())
                    .append(", Address: ").append(leader).append("\n");
        } else {
            sb.append("Leader: Not present\n");
        }
        return sb.toString();
    }

    public void printStatus() {
        log.info(getStatus());
    }

    /**
     * Reset the node's topology (clear all neighbors and no leader).
     */
    public void resetTopology() {
        if (myNeighbours != null) {
            myNeighbours.getNeighbours().clear();
            myNeighbours.setLeaderNode(null);
            log.info("Topology has been reset on Node {}.", nodeId);
        }
    }

    // ----------------------------------------------------------
    // Bully-Related Actions
    // ----------------------------------------------------------
    public void startElection() {
        if (isKilled || isLeft) {
            log.info("Node {} is inactive, skip startElection.", nodeId);
            return;
        }
        bully.startElection();
    }

    public void checkLeader() {
        if (isKilled || isLeft) {
            log.info("Node {} is inactive, skip checkLeader.", nodeId);
            return;
        }

        Address leader = myNeighbours.getLeaderNode();
        if (leader != null) {
            try {
                getCommHub().getRMIProxy(leader).checkStatusOfLeader(nodeId);
                log.info("Leader is alive: {}", leader);
            } catch (RemoteException e) {
                log.warn("Leader Node {} is unreachable. Starting election.", leader.getNodeID());
                myNeighbours.setLeaderNode(null);
                startElection();
            }
        } else {
            log.info("No leader present, starting election...");
            startElection();
        }
    }

    // ----------------------------------------------------------
    // Send message to a particular neighbor
    // ----------------------------------------------------------
    public boolean sendMessageToNode(long receiverId, String content) {
        if (isKilled || isLeft) {
            log.info("Node {} is inactive, skip sendMessageToNode.", nodeId);
            return false;
        }

        Address receiverAddr = null;
        for (Address addr : myNeighbours.getNeighbours()) {
            if (addr.getNodeID() == receiverId) {
                receiverAddr = addr;
                break;
            }
        }
        if (receiverAddr != null) {
            int attempts = 3;
            boolean messageSent = false;

            while (attempts > 0) {
                try {
                    getCommHub().getRMIProxy(receiverAddr).sendMessage(nodeId, (int) receiverId, content);
                    log.info("Message sent from Node {} to Node {}: {}", nodeId, receiverId, content);
                    messageSent = true;
                    break;
                } catch (RemoteException e) {
                    log.warn("Failed to send message to Node {}. Attempts left: {}", receiverId, attempts - 1);
                    attempts--;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Message sending interrupted.");
                        break;
                    }
                }
            }
            if (!messageSent) {
                log.warn("Node {} is unreachable after {} attempts. Removing from neighbours...", receiverId, 3);
                myNeighbours.removeNode(receiverAddr);
                return false;
            } else {
                return true;
            }
        } else {
            log.warn("Receiver Node {} not found in neighbours.", receiverId);
            return false;
        }
    }

    public DSNeighbours getNeighbours() {
        return this.myNeighbours;
    }

    public CommunicationHub getCommHub() {
        return this.myCommHub;
    }


    // ----------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------
    public void stopRMI() {
        stopMessageReceiver();
        log.info("RMI stopped for Node {}.", nodeId);
    }

    public void startRMI() {
        if (!isKilled && !isLeft) {
            startMessageReceiver();
        } else {
            log.info("Node {} is inactive, ignoring startRMI.", nodeId);
        }
    }

    public void kill() {
        if (isKilled) {
            log.info("Node {} is already KILLED.", nodeId);
            return;
        }
        log.info("Killing Node {} without notifying neighbors...", nodeId);
        isKilled = true;
        isLeft = false;
        stopRMI();
        log.info("Node {} has been killed (no notification).", nodeId);
    }

    public void leave() {
        if (isKilled || isLeft) {
            log.info("Node {} is already inactive (killed or left).", nodeId);
            return;
        }
        if (isCoordinator) {
            log.warn("Leader Node {} is leaving the network gracefully.", nodeId);
            leaveNode();
            return;
        }

        log.info("Node {} is leaving the network...", nodeId);

        for (Address neighbour : myNeighbours.getNeighbours()) {
            try {
                NodeCommands remoteNode = getCommHub().getRMIProxy(neighbour);
                remoteNode.notifyAboutLogOut(address);
                log.info("Node {} notified Node {} about leaving.", nodeId, neighbour.getNodeID());
            } catch (RemoteException e) {
                log.warn("Failed to notify Node {} about leaving.", neighbour.getNodeID(), e);
            }
        }

        resetTopology();
        stopRMI();

        isLeft = true;
        isKilled = false;
        log.info("Node {} has left the network.", nodeId);
    }

    private void leaveNode() {
        log.info("Leader Node {} is leaving the network gracefully.", nodeId);

        for (Address neighbour : myNeighbours.getNeighbours()) {
            try {
                NodeCommands remoteNode = myCommHub.getRMIProxy(neighbour);
                remoteNode.notifyAboutLogOut(address);
                log.info("Leader Node {} notified Node {} about leaving.", nodeId, neighbour.getNodeID());
            } catch (RemoteException e) {
                log.warn("Failed to notify Node {} about leader leaving.", neighbour.getNodeID(), e);
            }
        }

        isCoordinator = false;
        myNeighbours.setLeaderNode(null);

        resetTopology();
        stopRMI();

        isLeft = true;
        isKilled = false;
        log.info("Leader Node {} has left the network.", nodeId);
    }

    public void revive() {
        if (!isKilled && !isLeft) {
            log.info("Node {} is neither killed nor left; no need to revive.", nodeId);
            return;
        }
        log.info("Reviving node {}...", nodeId);

        isKilled = false;
        isLeft = false;

        startRMI();

        log.info("Node {} revived. You can join neighbors or start an election if needed.", nodeId);
    }

    // ----------------------------------------------------------
    // MAIN + run()
    // ----------------------------------------------------------
    @Override
    public void run() {
        // 1) Start RMI service
        startMessageReceiver();

        // 2) Initialize
        myCommHub = new CommunicationHub(this);
        bully = new BullyAlgorithm(this);

        myConsoleHandler = new ConsoleHandler(this);
        myAPIHandler = new APIHandler(this, apiPort);

        // 3) Start REST and console
        myAPIHandler.start();
        new Thread(myConsoleHandler).start();

        // 4) Print initial status
        printStatus();

        // 5) If user gave 4 args => auto-join at startup
        if (otherNodeIP != null && otherNodePort > 0) {
            log.info("Node {} auto-joining neighbor at {}:{} ...", nodeId, otherNodeIP, otherNodePort);
            join(otherNodeIP, otherNodePort);
        }

        // 6) Keep alive until killed or left
        while (!isKilled && !isLeft) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Main thread interrupted for Node {}.", nodeId);
            }
        }

        log.info("Node {} is shutting down.", nodeId);
    }

    public static void main(String[] args) {
        thisNode = new Node(args);
        thisNode.run();
    }
}
