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

@Slf4j
@Getter
@Setter
public class Node implements Runnable {

    public static final String COMM_INTERFACE_NAME = "DSVNode";
    public static Node thisNode = null;

    // Flag indicating that the node was killed (hard stop)
    private boolean isKilled = false;

    // Flag indicating that the node has gracefully left the network
    private boolean isLeft = false;

    private boolean isCoordinator = false;
    private boolean receivedOK = false;
    private boolean voting = false;

    private String nickname = "Unknown";
    private String myIP;
    private int rmiPort = 3010;
    private int apiPort = 7010;

    private long nodeId = 0;
    private Address address;
    private DSNeighbours myNeighbours;
    private MessageReceiver myMessageReceiver;
    private CommunicationHub myCommHub;
    private ConsoleHandler myConsoleHandler;
    private APIHandler myAPIHandler;
    private BullyAlgorithm bully;

    // Returns IP for nodes 1..5
    private String getIpForNodeId(long id) {
        switch ((int) id) {
            case 1: return "192.168.56.105";
            case 2: return "192.168.56.154";
            case 3: return "192.168.56.106";
            case 4: return "192.168.56.107";
            case 5: return "192.168.56.108";
            default:
                return "127.0.0.1";
        }
    }

    public Node(String[] args) {
        if (args.length >= 1) {
            try {
                nodeId = Long.parseLong(args[0]);
                rmiPort = 50050 + (int) nodeId;
                apiPort = 7000 + (int) nodeId;
                this.myIP = getIpForNodeId(nodeId);

                myNeighbours = new DSNeighbours();

                log.info("Node initialized with ID {} on ports RMI: {}, API: {}. My IP = {}",
                        nodeId, rmiPort, apiPort, myIP);
            } catch (NumberFormatException e) {
                log.error("Invalid node ID provided: {}", args[0], e);
                System.exit(1);
            }
        } else {
            log.error("Node ID is required as the first parameter.");
            System.exit(1);
        }
    }

    private void startMessageReceiver() {
        address = new Address(myIP, rmiPort, nodeId);
        System.setProperty("java.rmi.server.hostname", address.getHostname());

        try {
            if (this.myMessageReceiver == null) {
                this.myMessageReceiver = new MessageReceiver(this);
            }

            NodeCommands skeleton = (NodeCommands) UnicastRemoteObject.exportObject(this.myMessageReceiver, rmiPort);

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
            }
        } catch (Exception e) {
            log.error("Stopping message listener failed: {}", e.getMessage(), e);
        }
        log.info("Message listener stopped.");
    }

    private void tryConnectToTopology() {
        if (isKilled || isLeft) {
            log.info("Node {} is not active (killed or left), skip tryConnectToTopology.", nodeId);
            return;
        }

        log.info("Trying to connect to all possible nodes in topology...");

        boolean hasNeighbours = false;
        for (int i = 1; i <= 5; i++) {
            if (i == nodeId) continue;

            String neighbourIp = getIpForNodeId(i);
            int neighbourPort = 50050 + i;
            Address neighbourAddress = new Address(neighbourIp, neighbourPort, (long)i);

            try {
                NodeCommands remoteNode = getCommHub().getRMIProxy(neighbourAddress);
                remoteNode.join(address);
                myNeighbours.addNewNode(neighbourAddress);
                hasNeighbours = true;
                log.info("Connected to node {} at IP {}", i, neighbourIp);
            } catch (RemoteException e) {
                log.debug("Cannot connect to node {}: {}", i, e.getMessage());
            }
        }

        logNeighboursInfo();

        // Add a delay to ensure leader notifications are processed
        try {
            Thread.sleep(1000); // Delay 1 second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Sleep interrupted: {}", e.getMessage());
        }

        if (!hasNeighbours) {
            log.info("No neighbours found. Starting election for node {}...", nodeId);
            startElection();
        } else {
            checkLeader();
        }
    }

    private void logNeighboursInfo() {
        log.info("Node {} neighbors info:", nodeId);
        for (Address neighbour : myNeighbours.getNeighbours()) {
            log.info("    Neighbor -> ID: {}, Hostname: {}, Port: {}",
                    neighbour.getNodeID(), neighbour.getHostname(), neighbour.getPort());
        }
    }

    public void resetTopology() {
        if (myNeighbours != null) {
            myNeighbours.getNeighbours().clear();
            myNeighbours.setLeaderNode(null);
            System.out.println("Topology has been reset.");
            log.info("Topology reset on node {}.", nodeId);
        }
    }

    public String getStatus() {
        // If the node was killed, show that info
        if (isKilled) {
            return "Node " + nodeId + " status: KILLED (no RMI)\n";
        }
        // If the node has left the network, show that info
        if (isLeft) {
            return "Node " + nodeId + " status: LEFT the network (no RMI)\n";
        }

        // Otherwise, show full status
        StringBuilder status = new StringBuilder();
        status.append("Node ID: ").append(nodeId).append("\n");
        status.append("Address: ").append(address).append("\n");
        status.append("Neighbours:\n");
        for (Address neighbour : myNeighbours.getNeighbours()) {
            status.append("    ").append(neighbour).append("\n");
        }
        if (myNeighbours.isLeaderPresent()) {
            Address leader = myNeighbours.getLeaderNode();
            status.append("Leader: Node ID ").append(leader.getNodeID())
                    .append(", Address: ").append(leader).append("\n");
        } else {
            status.append("Leader: Not present\n");
        }
        return status.toString();
    }

    public void printStatus() {
        System.out.println(getStatus());
    }

    @Override
    public void run() {
        // Start RMI
        startMessageReceiver();

        myCommHub = new CommunicationHub(this);
        bully = new BullyAlgorithm(this);

        myConsoleHandler = new ConsoleHandler(this);
        myAPIHandler = new APIHandler(this, apiPort);

        // Connect to other nodes
        tryConnectToTopology();

        // Start HTTP server
        myAPIHandler.start();
        // Start console
        new Thread(myConsoleHandler).start();

        // Print initial status
        printStatus();
    }

    public DSNeighbours getNeighbours() {
        return this.myNeighbours;
    }

    public CommunicationHub getCommHub() {
        return this.myCommHub;
    }

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
                System.out.println("Leader is alive: " + leader);
                log.info("Leader Node {} is alive.", leader.getNodeID());
            } catch (RemoteException e) {
                System.out.println("Leader is unreachable, starting election...");
                log.warn("Leader Node {} is unreachable. Starting election.", leader.getNodeID());
                myNeighbours.setLeaderNode(null);
                startElection();
            }
        } else {
            System.out.println("No leader present, starting election...");
            log.info("No leader present. Starting election.");
            startElection();
        }
    }


    public void sendMessageToNode(long receiverId, String content) {
        if (isKilled || isLeft) {
            log.info("Node {} is inactive, skip sendMessageToNode.", nodeId);
            return;
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
                    log.info("Message sent from {} to {}: {}", nodeId, receiverId, content);
                    System.out.println("Message sent to Node " + receiverId);
                    messageSent = true;
                    break;
                } catch (RemoteException e) {
                    log.warn("Failed to send message to node {}. Attempts left: {}", receiverId, attempts - 1);
                    attempts--;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        log.error("Message sending interrupted.");
                        break;
                    }
                }
            }
            if (!messageSent) {
                System.out.println("Node " + receiverId + " is unreachable. Removing from neighbours...");
                log.warn("Node {} is unreachable. Removing from neighbours.", receiverId);
                myNeighbours.removeNode(receiverAddr);
            }
        } else {
            System.out.println("Receiver not found in neighbours.");
        }
    }

    public void stopRMI() {
        stopMessageReceiver();
    }

    public void startRMI() {
        // Only start RMI if the node is not killed/left
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
        // Hard kill
        isKilled = true;
        isLeft = false;
        stopRMI();
        System.out.println("Node " + nodeId + " killed (no proper logout).");
        log.info("Node {} killed (no proper logout).", nodeId);
    }

    public void leave() {
        if (isKilled || isLeft) {
            log.info("Node {} is already inactive (killed or left).", nodeId);
            return;
        }
        System.out.println("Node " + nodeId + " is leaving the network...");
        log.info("Node {} is leaving the network...", nodeId);

        for (Address neighbour : myNeighbours.getNeighbours()) {
            try {
                NodeCommands remoteNode = myCommHub.getRMIProxy(neighbour);
                remoteNode.notifyAboutLogOut(address);
                log.info("Node {} notified node {} about leaving.", nodeId, neighbour.getNodeID());
            } catch (RemoteException e) {
                log.warn("Failed to notify node {} about leaving.", neighbour.getNodeID(), e);
            }
        }

        resetTopology();
        stopRMI();

        // Graceful exit
        isLeft = true;
        isKilled = false;

        System.out.println("Node " + nodeId + " has left the network.");
        log.info("Node {} successfully left the network.", nodeId);
    }

    public void revive() {
        // If node is neither killed nor left, do nothing
        if (!isKilled && !isLeft) {
            log.info("Node {} is not killed/left, no need to revive.", nodeId);
            return;
        }
        System.out.println("Reviving node " + nodeId + "...");
        log.info("Reviving node {}...", nodeId);

        // Reset both flags
        isKilled = false;
        isLeft = false;

        startRMI();
        tryConnectToTopology();

        Address currentLeader = null;
        for (Address neighbour : myNeighbours.getNeighbours()) {
            try {
                NodeCommands remoteNode = myCommHub.getRMIProxy(neighbour);
                Address leader = remoteNode.getCurrentLeader();
                if (leader != null) {
                    currentLeader = leader;
                    break;
                }
            } catch (RemoteException e) {
                log.warn("Failed to get leader information from node {}: {}", neighbour.getNodeID(), e.getMessage());
            }
        }

        if (currentLeader != null) {
            myNeighbours.setLeaderNode(currentLeader);
            System.out.println("Leader found: Node ID " + currentLeader.getNodeID());
            log.info("Node {} found leader: {}", nodeId, currentLeader);
        } else {
            boolean hasHigherId = false;
            for (Address neighbour : myNeighbours.getNeighbours()) {
                if (neighbour.getNodeID() > nodeId) {
                    hasHigherId = true;
                    break;
                }
            }

            if (hasHigherId) {
                System.out.println("Higher ID nodes detected. Connecting as a regular node...");
                log.info("Node {} detected higher ID nodes. Connecting as a regular node.", nodeId);
                checkLeader();
            } else {
                System.out.println("No higher ID nodes detected. Starting election...");
                log.info("Node {} is the highest ID node. Initiating election...", nodeId);
                startElection();
            }
        }

        log.info("Node {} restored with leader: {}", nodeId, myNeighbours.getLeaderNode());

        for (Address neighbour : myNeighbours.getNeighbours()) {
            try {
                NodeCommands remoteNode = myCommHub.getRMIProxy(neighbour);
                remoteNode.notifyAboutRevival(address);
                log.info("Notified node {} about revival of node {}.", neighbour.getNodeID(), nodeId);
            } catch (RemoteException e) {
                log.warn("Failed to notify node {} about revival.", neighbour.getNodeID(), e);
            }
        }

        System.out.println("Node " + nodeId + " has been revived.");
        log.info("Node {} successfully revived.", nodeId);
    }

    public static void main(String[] args) {
        thisNode = new Node(args);
        thisNode.run();
    }
}
