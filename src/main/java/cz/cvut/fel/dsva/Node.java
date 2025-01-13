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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a node in a distributed system.
 * Handles RMI communication, leader election (Bully algorithm), and node lifecycle (join, leave, revive, etc.).
 */
@Slf4j
@Getter
@Setter
public class Node implements Runnable {

    public static final String COMM_INTERFACE_NAME = "DSVNode";
    public static Node thisNode = null;

    // Bully / Node states
    private boolean isKilled = false;
    private boolean isLeft = false;
    private boolean isCoordinator = false;
    private boolean receivedOK = false;
    private boolean voting = false;

    // Basic Info
    private String nickname = "Unknown";
    private String myIP;
    private int rmiPort = 3010;
    private int apiPort = 7010;
    private long nodeId = 0;

    // Optional auto-join
    private String otherNodeIP;
    private int otherNodePort = 0;

    // Node Structure
    private Address address;
    private DSNeighbours myNeighbours;
    private MessageReceiver myMessageReceiver;
    private CommunicationHub myCommHub;
    private ConsoleHandler myConsoleHandler;
    private APIHandler myAPIHandler;
    private BullyAlgorithm bully;

    // Save neighbors if we kill or leave, so we can rejoin or notify them upon revival.
    private final List<Address> savedNeighbors = new CopyOnWriteArrayList<>();

    /**
     * Constructs a Node instance based on command-line arguments.
     * Handles different configurations such as auto-joining a network or starting as a standalone node.
     *
     * @param args Command-line arguments:
     *             - 4 arguments: [nickname, myIP, rmiPort, apiPort]
     *             - 6 arguments (auto-join): [nickname, myIP, rmiPort, otherNodeIP, otherNodePort, apiPort]
     */
    public Node(String[] args) {
        if (args.length == 4) {
            log.info("Node constructor with 4 args.");
            nickname = args[0];
            myIP     = args[1];
            rmiPort  = Integer.parseInt(args[2]);
            apiPort  = Integer.parseInt(args[3]);
        } else if (args.length == 6) {
            log.info("Node constructor with 6 args (auto-join).");
            nickname      = args[0];
            myIP          = args[1];
            rmiPort       = Integer.parseInt(args[2]);
            otherNodeIP   = args[3];
            otherNodePort = Integer.parseInt(args[4]);
            apiPort       = Integer.parseInt(args[5]);
        } else {
            log.warn("Wrong number of command-line parameters. Using defaults for Node.");
        }
    }

    /**
     * Generates a unique node ID based on the IP address and port.
     *
     * @param address The IP address of the node.
     * @param port    The port associated with the node.
     * @return A long value representing the unique node ID.
     */
    public long generateId(String address, int port) {
        String[] array = address.split("\\.");
        long id = 0;
        for (String part : array) {
            long temp = Long.parseLong(part);
            id = (id * 1000) + temp;
        }
        if (id == 0) {
            log.warn("Failed to parse IP: {}, defaulting nodeId to 666000666000L.", address);
            id = 666000666000L;
        }
        id += (port * 1000000000000L);
        return id;
    }

    /**
     * Starts the RMI message receiver for the node.
     * Initializes the RMI registry and binds the message receiver to the specified port.
     */
    private void startMessageReceiver() {
        // Create local address & nodeId
        address = new Address(myIP, rmiPort);
        nodeId = generateId(myIP, rmiPort);
        address.setNodeID(nodeId);

        System.setProperty("java.rmi.server.hostname", address.getHostname());
        try {
            if (myMessageReceiver == null) {
                myMessageReceiver = new MessageReceiver(this);
            }

            NodeCommands skeleton = (NodeCommands)
                    UnicastRemoteObject.exportObject(myMessageReceiver, rmiPort);

            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(rmiPort);
                registry.rebind(COMM_INTERFACE_NAME, skeleton);
                log.info("Bound to existing RMI registry on port {}.", rmiPort);
            } catch (RemoteException re) {
                log.info("Creating new RMI registry on port {}.", rmiPort);
                registry = LocateRegistry.createRegistry(rmiPort);
                registry.rebind(COMM_INTERFACE_NAME, skeleton);
            }
        } catch (Exception e) {
            log.error("Failed to start message listener: {}", e.getMessage(), e);
        }

        log.info("Message listener started on {}:{} with nodeId={}.",
                address.getHostname(), rmiPort, nodeId);
    }

    /**
     * Stops the RMI message receiver for the node.
     * Unbinds the RMI service and stops the associated registry.
     */
    private void stopMessageReceiver() {
        try {
            if (myMessageReceiver != null) {
                Registry registry = LocateRegistry.getRegistry(rmiPort);
                try {
                    registry.unbind(COMM_INTERFACE_NAME);
                    log.info("Unbound RMI service {}.", COMM_INTERFACE_NAME);
                } catch (NotBoundException e) {
                    log.warn("RMI service not bound: {}", e.getMessage());
                }
                UnicastRemoteObject.unexportObject(myMessageReceiver, true);
                myMessageReceiver = null;
                log.info("Message listener stopped for Node {}.", nodeId);
            }
        } catch (Exception e) {
            log.error("Failed to stop message listener: {}", e.getMessage(), e);
        }
        log.info("Message listener fully stopped.");
    }

    /**
     * Joins another node in the distributed system.
     *
     * @param otherIP   The IP address of the node to join.
     * @param otherPort The port of the node to join.
     */
    public void join(String otherIP, int otherPort) {
        if (isKilled || isLeft) {
            log.warn("Node {} is inactive, cannot join another node.", nodeId);
            return;
        }
        log.info("Node {} calling join(...) on node at {}:{}.", nodeId, otherIP, otherPort);

        try {
            // 1) Create neighbor Address with consistent ID
            long neighborID = generateId(otherIP, otherPort);
            Address otherAddr = new Address(neighborID, otherIP, otherPort);

            // 2) RMI call
            NodeCommands remoteNode = myCommHub.getRMIProxy(otherAddr);
            remoteNode.join(this.address);

            // 3) Locally add neighbor
            if (myNeighbours != null) {
                myNeighbours.addNewNode(otherAddr);
                log.info("Locally added neighbor with nodeId={} for Node {}.",
                        neighborID, nodeId);
            } else {
                log.warn("myNeighbours is null while trying to add a new neighbor?");
            }
            log.info("Join call done. Node {} told {}:{} to join.",
                    nodeId, otherIP, otherPort);

        } catch (RemoteException e) {
            log.warn("Failed to join node at {}:{} => {}", otherIP, otherPort, e.getMessage());
        }
        log.info("Neighbors after join: {}", myNeighbours);
    }

    /**
     * Retrieves the current status of the node, including its ID, address, and neighbors.
     *
     * @return A string representation of the node's status.
     */
    public String getStatus() {
        if (isKilled) {
            return "Node " + nodeId + " = KILLED (no RMI)\n";
        }
        if (isLeft) {
            return "Node " + nodeId + " = LEFT the network (no RMI)\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Node ID: ").append(nodeId).append("\n");
        sb.append(" ").append(address).append("\n");

        sb.append("Neighbors:\n");
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

    /**
     * Logs and prints the current status of the node, including its ID, address, and neighbors.
     */
    public void printStatus() {
        log.info(getStatus());
    }

    /**
     * Resets the topology of the node by clearing its neighbors and unsetting the leader.
     */
    public void resetTopology() {
        if (myNeighbours != null) {
            myNeighbours.getNeighbours().clear();
            myNeighbours.setLeaderNode(null);
            log.info("Topology reset on Node {}.", nodeId);
        } else {
            log.warn("Topology reset requested, myNeighbours is null on Node {}.", nodeId);
        }
    }

    /**
     * Initiates the Bully leader election algorithm for this node.
     * Skips execution if the node is killed or has left the network.
     */
    public void startElection() {
        if (isKilled || isLeft) {
            log.info("Node {} is inactive, skipping startElection.", nodeId);
            return;
        }
        bully.startElection();
    }

    /**
     * Checks the current leader's status. If the leader is unreachable, starts a new election.
     */
    public void checkLeader() {
        if (isKilled || isLeft) {
            log.info("Node {} is inactive, skipping checkLeader.", nodeId);
            return;
        }
        if (myNeighbours == null) {
            log.warn("checkLeader called but myNeighbours is null, Node {} can't proceed.", nodeId);
            return;
        }
        Address leader = myNeighbours.getLeaderNode();
        if (leader != null) {
            try {
                // Attempt to contact the leader
                myCommHub.getRMIProxy(leader).checkStatusOfLeader(nodeId);

                log.info("Leader is alive: {}", leader);
            } catch (RemoteException e) {
                log.warn("Leader Node {} is unreachable. Starting election for Node {} ...", leader.getNodeID(), nodeId);

                // Remove that leader from neighbors so we don't keep referencing it
                myNeighbours.removeNode(leader);
                myNeighbours.setLeaderNode(null);

                startElection();
            }
        } else {
            log.info("No leader present, Node {} will start election now...", nodeId);
            startElection();
        }
    }

    /**
     * Sends a message to a specified node.
     *
     * @param receiverId The ID of the receiver node.
     * @param content    The content of the message to send.
     * @return {@code true} if the message was sent successfully, {@code false} otherwise.
     */
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
                log.info("Message from Node {} to Node {}: {}", nodeId, receiverId, content);
                messageSent = true;
                break;
            } catch (RemoteException e) {
                log.warn("Failed to send message to Node {}. Attempts left: {}", receiverId, attempts - 1);
                attempts--;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Send message interrupted for Node {}.", nodeId);
                    break;
                }
            }
        }
        if (!messageSent) {
            log.warn("Node {} unreachable after 3 attempts, removing from Node {} neighbors...",
                    receiverId, nodeId);
            myNeighbours.removeNode(receiverAddr);
            return false;
        }
        return true;
    }

    // Accessors
    public DSNeighbours getNeighbours() {
        return this.myNeighbours;
    }

    public CommunicationHub getCommHub() {
        return this.myCommHub;
    }

    /**
     * Stops the RMI message receiver for this node.
     */
    public void stopRMI() {
        stopMessageReceiver();
        log.info("RMI stopped for Node {}.", nodeId);
    }

    /**
     * Starts the RMI message receiver for this node, if the node is active.
     */
    public void startRMI() {
        if (!isKilled && !isLeft) {
            startMessageReceiver();
        } else {
            log.info("Node {} is inactive, ignoring startRMI.", nodeId);
        }
    }

    /**
     * Marks the node as killed, stopping its RMI service and saving its neighbors for potential revival.
     * This action does not notify neighbors.
     */
    public void kill() {
        if (isKilled) {
            log.info("Node {} is already KILLED.", nodeId);
            return;
        }
        log.info("Killing Node {} without notifying neighbors...", nodeId);

        savedNeighbors.clear();
        if (myNeighbours != null) {
            savedNeighbors.addAll(myNeighbours.getNeighbours());
        }

        isKilled = true;
        isLeft   = false;
        stopRMI();
        log.info("Node {} has been killed. Saved neighbors: {}", nodeId, savedNeighbors);
    }

    /**
     * Leaves the network gracefully, notifying neighbors of the departure.
     * If the node is the leader, it will transfer responsibilities before leaving.
     */
    public void leave() {
        if (isKilled || isLeft) {
            log.info("Node {} is already inactive (killed or left).", nodeId);
            return;
        }
        if (isCoordinator) {
            log.warn("Leader Node {} is leaving gracefully.", nodeId);
            leaveNode();
            return;
        }

        log.info("Node {} is leaving the network (non-leader)...", nodeId);

        // 1) Save neighbors
        savedNeighbors.clear();
        if (myNeighbours != null) {
            savedNeighbors.addAll(myNeighbours.getNeighbours());
        }

        // 2) STOP RMI
        stopRMI();

        // 3) Notify each neighbor that we are leaving
        for (Address neighbor : savedNeighbors) {
            try {
                NodeCommands remoteNode = myCommHub.getRMIProxy(neighbor);
                remoteNode.notifyAboutLogOut(address);
                log.info("Node {} told Node {} about leaving.", nodeId, neighbor.getNodeID());
            } catch (RemoteException e) {
                log.warn("Failed to notify Node {} about leaving Node {}: {}",
                        neighbor.getNodeID(), nodeId, e.getMessage());
            }
        }

        // 4) Clean up
        resetTopology();
        isLeft   = true;
        isKilled = false;

        log.info("Node {} left network gracefully. Saved neighbors: {}", nodeId, savedNeighbors);
    }

    /**
     * Handles the departure of the node when it is the leader.
     * Transfers responsibilities and notifies neighbors before exiting the network.
     */
    private void leaveNode() {
        log.info("Leader Node {} is leaving gracefully...", nodeId);

        // 1) Mark not coordinator first
        isCoordinator = false;

        // 2) Save neighbors, so we can do some logic after unbinding from RMI
        savedNeighbors.clear();
        if (myNeighbours != null) {
            savedNeighbors.addAll(myNeighbours.getNeighbours());
        }

        // 3) Stop RMI or unbind local registry, so any callback to this node fails fast
        stopRMI();

        // 4) Now that RMI is stopped, safely notify neighbors about leaving
        if (myNeighbours != null) {
            // We do NOT iterate myNeighbours.getNeighbours() now, because we reset it soon
            for (Address neighbor : savedNeighbors) {
                try {
                    NodeCommands remoteNode = myCommHub.getRMIProxy(neighbor);
                    remoteNode.notifyAboutLogOut(address);
                    log.info("Leader Node {} told Node {} about leaving.", nodeId, neighbor.getNodeID());
                } catch (RemoteException e) {
                    log.warn("Failed to notify Node {} about leader Node {} leaving: {}",
                            neighbor.getNodeID(), nodeId, e.getMessage());
                }
            }
            myNeighbours.setLeaderNode(null);
        }

        // 5) Reset local topology
        resetTopology();

        // 6) Mark states
        isLeft   = true;
        isKilled = false;

        log.info("Leader Node {} has fully left. Saved neighbors: {}", nodeId, savedNeighbors);
    }


    /**
     * Revives a previously killed or left node, restarting its RMI service
     * and notifying saved neighbors about its return.
     */
    public void revive() {
        if (!isKilled && !isLeft) {
            log.info("Node {} is neither killed nor left; no need to revive.", nodeId);
            return;
        }
        log.info("Reviving Node {} (no-arg).", nodeId);

        isKilled = false;
        isLeft   = false;
        startRMI();

        log.info("Node {} revived. Notifying neighbors about revival and optionally rejoin them...", nodeId);

        // *** NEW ***: Notify saved neighbors about revival
        for (Address neighbor : savedNeighbors) {
            try {
                NodeCommands remote = getCommHub().getRMIProxy(neighbor);
                remote.notifyAboutRevival(this.address);
                log.info("Node {} notified neighbor {} about revival.", nodeId, neighbor.getNodeID());
            } catch (RemoteException e) {
                log.warn("Failed to notify neighbor {} about Node {} revival: {}",
                        neighbor.getNodeID(), nodeId, e.getMessage());
            }
        }

        log.info("Revival process completed for Node {}.", nodeId);
    }

    /**
     * The main loop of the node. Initializes components, starts RMI and API services,
     * handles auto-joining if specified, and keeps the node alive until it is killed or leaves.
     */
    @Override
    public void run() {
        // We'll do final ID creation in startMessageReceiver
        log.info("run() called for Node with nickname='{}'.", nickname);

        myNeighbours = new DSNeighbours();
        log.info("myNeighbours created, currently empty for Node.");

        startMessageReceiver();

        myCommHub       = new CommunicationHub(this);
        bully           = new BullyAlgorithm(this);
        myConsoleHandler= new ConsoleHandler(this);
        myAPIHandler    = new APIHandler(this, apiPort);

        myAPIHandler.start();
        new Thread(myConsoleHandler).start();

        // Print initial status
        printStatus();

        // If user gave 6 args => auto-join
        if (otherNodeIP != null && otherNodePort > 0) {
            log.info("Auto-joining neighbor at {}:{} ...", otherNodeIP, otherNodePort);
            join(otherNodeIP, otherNodePort);
        }

        // Keep alive
        while (!isKilled && !isLeft) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Main thread interrupted for Node {}.", nodeId, e);
            }
        }
        log.info("Node {} shutting down from run().", nodeId);
    }

    /**
     * The entry point of the application. Initializes a Node instance and starts its execution.
     *
     * @param args Command-line arguments for node initialization.
     */
    public static void main(String[] args) {
        thisNode = new Node(args);
        thisNode.run();
    }
}
