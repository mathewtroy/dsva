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
 * Node class implementing the Bully Leader Election algorithm with dynamic IP assignment.
 *
 * Usage:
 *   - If you provide 3 args:  nickname, myIP, rmiPort        (old style)
 *   - If you provide 5 args:  nickname, myIP, rmiPort, otherNodeIP, otherNodePort
 *       => it will auto-join the specified neighbor.
 *
 * Note: In your older demo, 3 or 5 arguments were used. Adjust as needed for your exact logic.
 */
@Slf4j
@Getter
@Setter
public class Node implements Runnable {

    public static final String COMM_INTERFACE_NAME = "DSVNode";
    public static Node thisNode = null;

    // ----------------------------------------------------------
    // Internal flags (Bully-related)
    // ----------------------------------------------------------
    private boolean isKilled = false;
    private boolean isLeft = false;
    private boolean isCoordinator = false;
    private boolean receivedOK = false;
    private boolean voting = false;

    // ----------------------------------------------------------
    // Basic info
    // ----------------------------------------------------------
    private String nickname = "Unknown";  // first arg
    private String myIP;                 // second arg
    private int rmiPort = 3010;          // third arg
    private int apiPort = 7010;          // auto-calc or leftover
    private long nodeId = 0;            // optional numeric ID if needed

    // Additional optional info for auto-join
    private String otherNodeIP;   // 4th arg
    private int otherNodePort = 0; // 5th arg

    // ----------------------------------------------------------
    // Node structure
    // ----------------------------------------------------------
    private Address address;
    private DSNeighbours myNeighbours;         // Must be initialized to avoid NPE
    private MessageReceiver myMessageReceiver;
    private CommunicationHub myCommHub;
    private ConsoleHandler myConsoleHandler;
    private APIHandler myAPIHandler;
    private BullyAlgorithm bully;

    /**
     * Constructor that expects:
     *   - 3 args: nickname, myIP, rmiPort
     *   - 5 args: nickname, myIP, rmiPort, otherNodeIP, otherNodePort
     * If the argument count doesn't match, we use some default logic.
     */
    public Node(String[] args) {
        if (args.length == 3) {
            log.info("Node constructor invoked with 3 args (nickname, IP, rmiPort).");
            nickname = args[0];
            myIP     = args[1];
            rmiPort  = Integer.parseInt(args[2]);
        } else if (args.length == 5) {
            log.info("Node constructor invoked with 5 args (nickname, IP, rmiPort, otherIP, otherPort).");
            nickname      = args[0];
            myIP          = args[1];
            rmiPort       = Integer.parseInt(args[2]);
            otherNodeIP   = args[3];
            otherNodePort = Integer.parseInt(args[4]);
        } else {
            log.warn("Wrong number of command-line parameters. Using default values for Node.");
        }
    }

    private long generateId(String address, int port) {
        // address is typically myIP
        // for example: "192.168.56.105"
        String[] array = address.split("\\.");
        long id = 0;
        for (String part : array) {
            long temp = Long.parseLong(part); // caution if parse fails
            id = (id * 1000) + temp;
        }
        if (id == 0) {
            log.warn("Failed to parse IP: {}, defaulting nodeId to 666000666000L.", address);
            id = 666000666000L;
        }
        // Then add port * 1000000000000
        id += (port * 1000000000000L);

        log.info("generateId({}, {}) => {}", address, port, id);
        return id;
    }

    // ----------------------------------------------------------
    // RMI
    // ----------------------------------------------------------
    private void startMessageReceiver() {
        // We create the address after we parse arguments,
        // but before we run the logic that needs it.
        address = new Address(myIP, rmiPort, nodeId);

        // Let RMI know the hostname
        System.setProperty("java.rmi.server.hostname", address.getHostname());

        try {
            if (this.myMessageReceiver == null) {
                this.myMessageReceiver = new MessageReceiver(this);
            }

            NodeCommands skeleton = (NodeCommands) UnicastRemoteObject.exportObject(this.myMessageReceiver, rmiPort);

            Registry registry;
            try {
                // Try to get a registry on the chosen port
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
    // Join logic
    // ----------------------------------------------------------
    public void join(String otherIP, int otherPort) {
        if (isKilled || isLeft) {
            log.warn("Node {} is inactive, cannot join another node.", nodeId);
            return;
        }
        log.info("Node {} attempting to join node at {}:{}", nodeId, otherIP, otherPort);

        try {
            Address otherAddr = new Address(otherIP, otherPort, -1L);
            NodeCommands remoteNode = myCommHub.getRMIProxy(otherAddr);

            // remoteNode.join(...) presumably returns void in your scenario
            remoteNode.join(this.address);

            log.info("Join call invoked (void). Node {} told {}:{} to join.", nodeId, otherIP, otherPort);

            // Optionally add that node into my own neighbors list
            if (myNeighbours != null) {
                myNeighbours.addNewNode(otherAddr);
            } else {
                log.warn("myNeighbours is null while trying to add a new neighbor?");
            }

        } catch (RemoteException e) {
            log.warn("Failed to join node at {}:{} => {}", otherIP, otherPort, e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // Utility & status
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
        if (myNeighbours != null) {
            for (Address neighbor : myNeighbours.getNeighbours()) {
                sb.append("    ").append(neighbor).append("\n");
            }
            if (myNeighbours.isLeaderPresent()) {
                Address leader = myNeighbours.getLeaderNode();
                sb.append("Leader: Node ID ").append(leader.getNodeID())
                        .append(", Address: ").append(leader).append("\n");
            } else {
                sb.append("Leader: Not present\n");
            }
        } else {
            sb.append("No DSNeighbours instance (null)\n");
        }

        return sb.toString();
    }

    public void printStatus() {
        log.info(getStatus());
    }

    public void resetTopology() {
        if (myNeighbours != null) {
            myNeighbours.getNeighbours().clear();
            myNeighbours.setLeaderNode(null);
            log.info("Topology has been reset on Node {}.", nodeId);
        } else {
            log.warn("Topology reset requested, but myNeighbours is null on Node {}.", nodeId);
        }
    }

    // ----------------------------------------------------------
    // Bully Methods (startElection, checkLeader, etc.)
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

        if (myNeighbours == null) {
            log.warn("checkLeader called but myNeighbours is null, Node {} can't proceed.", nodeId);
            return;
        }

        Address leader = myNeighbours.getLeaderNode();
        if (leader != null) {
            try {
                getCommHub().getRMIProxy(leader).checkStatusOfLeader(nodeId);
                log.info("Leader is alive: {}", leader);
            } catch (RemoteException e) {
                log.warn("Leader Node {} is unreachable. Starting election...", leader.getNodeID());
                myNeighbours.setLeaderNode(null);
                startElection();
            }
        } else {
            log.info("No leader present, Node {} will start election...", nodeId);
            startElection();
        }
    }

    // ----------------------------------------------------------
    // Sending messages
    // ----------------------------------------------------------
    public boolean sendMessageToNode(long receiverId, String content) {
        if (isKilled || isLeft) {
            log.info("Node {} is inactive, skip sendMessageToNode.", nodeId);
            return false;
        }
        if (myNeighbours == null) {
            log.warn("myNeighbours is null, cannot send message on Node {}.", nodeId);
            return false;
        }

        Address receiverAddr = null;
        for (Address addr : myNeighbours.getNeighbours()) {
            if (addr.getNodeID() == receiverId) {
                receiverAddr = addr;
                break;
            }
        }
        if (receiverAddr == null) {
            log.warn("Receiver Node {} not found in neighbors of Node {}.", receiverId, nodeId);
            return false;
        }

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
                    log.error("Message sending interrupted for Node {}.", nodeId);
                    break;
                }
            }
        }
        if (!messageSent) {
            log.warn("Node {} is unreachable after 3 attempts. Removing from neighbors of Node {}...", receiverId, nodeId);
            myNeighbours.removeNode(receiverAddr);
            return false;
        }
        return true;
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
        log.info("Node {} has been killed (no neighbor notification).", nodeId);
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
        if (myNeighbours != null) {
            for (Address neighbor : myNeighbours.getNeighbours()) {
                try {
                    NodeCommands remoteNode = myCommHub.getRMIProxy(neighbor);
                    remoteNode.notifyAboutLogOut(address);
                    log.info("Node {} notified Node {} about leaving.", nodeId, neighbor.getNodeID());
                } catch (RemoteException e) {
                    log.warn("Failed to notify Node {} about leaving.", neighbor.getNodeID(), e);
                }
            }
        } else {
            log.warn("myNeighbours is null, Node {} has no neighbors to inform about leaving.", nodeId);
        }

        resetTopology();
        stopRMI();

        isLeft = true;
        isKilled = false;
        log.info("Node {} has left the network.", nodeId);
    }

    private void leaveNode() {
        log.info("Leader Node {} is leaving the network gracefully.", nodeId);

        if (myNeighbours != null) {
            for (Address neighbor : myNeighbours.getNeighbours()) {
                try {
                    NodeCommands remoteNode = myCommHub.getRMIProxy(neighbor);
                    remoteNode.notifyAboutLogOut(address);
                    log.info("Leader Node {} notified Node {} about leaving.", nodeId, neighbor.getNodeID());
                } catch (RemoteException e) {
                    log.warn("Failed to notify Node {} about leader leaving.", neighbor.getNodeID(), e);
                }
            }
            isCoordinator = false;
            myNeighbours.setLeaderNode(null);
        } else {
            log.warn("myNeighbours is null while Leader Node {} tries to leave gracefully.", nodeId);
        }

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
        // 1) Generate ID from IP, port
        this.nodeId = generateId(myIP, rmiPort);
        log.info("Node ID generated as {} based on IP={} and rmiPort={}.", nodeId, myIP, rmiPort);

        // 2) Create Address
        address = new Address(myIP, rmiPort, nodeId);

        // 3) Initialize DSNeighbours (no-arg constructor)
        myNeighbours = new DSNeighbours();
        log.info("myNeighbours created for Node {}, currently empty.", nodeId);

        // 4) Start RMI
        startMessageReceiver();

        // 5) Initialize auxiliary components
        myCommHub = new CommunicationHub(this);
        bully = new BullyAlgorithm(this);
        myConsoleHandler = new ConsoleHandler(this);
        myAPIHandler = new APIHandler(this, apiPort);

        // 6) Start the REST API and the console
        myAPIHandler.start();
        new Thread(myConsoleHandler).start();

        // 7) Print initial status
        printStatus();

        // 8) If user gave 5 arguments => auto-join at startup
        if (otherNodeIP != null && otherNodePort > 0) {
            log.info("Node {} auto-joining neighbor at {}:{} ...", nodeId, otherNodeIP, otherNodePort);
            join(otherNodeIP, otherNodePort);
        }

        // 9) Keep alive until killed or left
        while (!isKilled && !isLeft) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Main thread interrupted for Node {}.", nodeId, e);
            }
        }

        log.info("Node {} is shutting down.", nodeId);
    }

    public static void main(String[] args) {
        thisNode = new Node(args);
        thisNode.run();
    }
}
