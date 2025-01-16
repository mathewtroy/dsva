package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.DSNeighbours;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Represents a node in a distributed system implementing the Bully leader election algorithm.
 * This class manages the node's state, communication via RMI, and interactions with other nodes.
 *
 * <p>Key functionalities include:
 * <ul>
 *     <li>Joining and leaving the network.</li>
 *     <li>Starting and handling elections.</li>
 *     <li>Sending and receiving messages.</li>
 *     <li>Maintaining a list of neighbors in a full mesh topology.</li>
 * </ul>
 *
 * <p>The node can also be controlled via console commands and an HTTP API.
 *
 * @author @author Kross Aleksandr
 */
@Slf4j
@Getter
@Setter
public class Node implements Runnable {
    public static final String COMM_INTERFACE_NAME = "DSVNode";

    private boolean isActive = true;
    private boolean isKilled = false;
    private boolean isLeft = false;
    private boolean electionInProgress = false;

    private String nickname = "Unknown";
    private String myIP = "127.0.0.1";
    private int myPort = 2010;
    private int apiPort = 7000;

    private String otherNodeIP = "127.0.0.1";
    private int otherNodePort = 2010;

    private long nodeId = 0;
    private Address myAddress;
    private DSNeighbours neighbours;
    private NodeCommands messageReceiver;
    private CommunicationHub communicationHub;

    /**
     * Constructs a Node instance with the provided command-line arguments.
     *
     * <p>The expected arguments are:
     * <ul>
     *     <li>If length is 3: <code>nickname</code>, <code>IP</code>, <code>port</code>.</li>
     *     <li>If length is 5: <code>nickname</code>, <code>IP</code>, <code>port</code>, <code>otherNodeIP</code>, <code>otherNodePort</code>.</li>
     * </ul>
     *
     * @param args Command-line arguments for node configuration.
     */
    public Node(String[] args) {
        if (args.length == 3) {
            nickname = args[0];
            myIP = otherNodeIP = args[1];
            myPort = otherNodePort = Integer.parseInt(args[2]);
            apiPort = 5000 + myPort;
        } else if (args.length == 5) {
            nickname = args[0];
            myIP = args[1];
            myPort = Integer.parseInt(args[2]);
            otherNodeIP = args[3];
            otherNodePort = Integer.parseInt(args[4]);
            apiPort = 5000 + myPort;
        } else {
            log.warn("Wrong number of parameters - using defaults (myPort=2010).");
            apiPort = 5000 + myPort;
        }
    }

    /**
     * Generates a unique identifier based on the IP address and port number.
     *
     * @param ip    The IP address of the node.
     * @param port  The port number of the node.
     * @return A unique long identifier.
     */
    private long generateId(String ip, int port) {
        String[] parts = ip.split("\\.");
        long id = 0;
        for (String part : parts) {
            try {
                long num = Long.parseLong(part);
                id = id * 1000 + num;
            } catch (NumberFormatException e) {
                log.error("Error parsing IP part: {}", part);
            }
        }
        id += port * 1000000000000L;
        return id;
    }

    /**
     * Computes the unique identifier for a node based on its IP and port.
     *
     * @param ip    The IP address of the node.
     * @param port  The port number of the node.
     * @return The unique long identifier.
     */
    public long computeId(String ip, int port) {
        return generateId(ip, port);
    }

    /**
     * Retrieves the address of the node.
     *
     * @return The {@link Address} of the node.
     */
    public Address getAddress() {
        return myAddress;
    }

    /**
     * The main execution method for the node.
     *
     * <p>This method initializes the node's ID and address, starts RMI communication,
     * joins the network if necessary, and starts console and API handlers.
     * It then enters a loop to keep the node running until it becomes inactive, killed, or leaves.
     */
    @Override
    public void run() {
        nodeId = generateId(myIP, myPort);
        myAddress = new Address(myIP, myPort);
        neighbours = new DSNeighbours(myAddress);

        log.info("Node {} is starting with ID={}", nickname, nodeId);
        printStatus();
        startRMI();
        communicationHub = new CommunicationHub(this);

        if (!(myIP.equals(otherNodeIP) && myPort == otherNodePort)) {
            join(otherNodeIP, otherNodePort);
        } else {
            neighbours.setLeader(myAddress);
            log.info("I am the first node. I become the leader: {}", myAddress);
        }

        Thread consoleThread = new Thread(new ConsoleHandler(this));
        consoleThread.start();

        Thread apiThread = new Thread(new APIHandler(this, apiPort));
        apiThread.start();

        while (isActive() && !isKilled && !isLeft) {
            try {
                Thread.sleep(8000);
            } catch (InterruptedException e) {
                log.error("Main loop interrupted.", e);
            }
        }
        log.warn("Node run() method finished for {} (ID={}).", nickname, nodeId);
    }

    /**
     * Initializes and starts the RMI registry and binds the message receiver.
     */
    public void startRMI() {
        try {
            System.setProperty("java.rmi.server.hostname", myAddress.getHostname());
            if (messageReceiver == null) {
                messageReceiver = new MessageReceiver(this);
            }
            NodeCommands stub = (NodeCommands) UnicastRemoteObject.exportObject(messageReceiver, 40000 + myPort);
            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(myPort);
                registry.rebind(COMM_INTERFACE_NAME, stub);
            } catch (RemoteException e) {
                log.info("No registry found on port {}. Creating a new one.", myPort);
                registry = LocateRegistry.createRegistry(myPort);
                registry.rebind(COMM_INTERFACE_NAME, stub);
            }
            log.info("RMI started on port {}", myPort);
        } catch (Exception e) {
            log.error("startRMI error: ", e);
        }
    }

    /**
     * Stops the RMI registry and unexports the message receiver.
     */
    public void stopRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry(myPort);
            registry.unbind(COMM_INTERFACE_NAME);
            UnicastRemoteObject.unexportObject(messageReceiver, false);
            messageReceiver = null;
            log.info("RMI stopped on port {}", myPort);
        } catch (Exception e) {
            log.error("stopRMI error: ", e);
        }
    }

    /**
     * Joins the node to an existing network by connecting to another node.
     *
     * @param ip    The IP address of the node to join.
     * @param port  The port number of the node to join.
     */
    public void join(String ip, int port) {
        Address other = new Address(ip, port);
        try {
            NodeCommands proxy = communicationHub.getProxy(other);
            DSNeighbours updated = proxy.join(myAddress);
            for (Address a : updated.getKnownNodes()) {
                neighbours.addNode(a);
            }
            neighbours.setLeader(updated.getLeader());
            log.info("Joined network with node at {}. Neighbors: {}", other, neighbours);
            printStatus();
        } catch (RemoteException e) {
            log.error("join error: ", e);
        }
    }

    /**
     * Initiates the leader election process using the Bully algorithm.
     */
    public void startElection() {
        if (!isActive() || isKilled || isLeft) {
            log.info("Cannot start election: node is not active.");
            return;
        }
        internalStartElection();
    }

    /**
     * Internal method to handle the election process.
     *
     * <p>Notifies higher-ID nodes and waits for responses.
     * If no higher-ID nodes respond, declares itself as the new leader.
     */
    public void internalStartElection() {
        if (electionInProgress) {
            log.info("Election already in progress.");
            return;
        }
        electionInProgress = true;
        log.info("Starting Bully election. My ID={}", nodeId);
        communicationHub.sendElectionToBiggerNodes();

        new Thread(() -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                log.error("Election wait interrupted.", e);
            }
            if (electionInProgress) {
                log.info("No higher node responded. I am the new leader.");
                neighbours.setLeader(myAddress);
                communicationHub.broadcastLeader();
                electionInProgress = false;
                printStatus();
            }
        }).start();
    }

    /**
     * Logs the current leader of the network.
     */
    public void checkLeader() {
        log.info("Current leader: {}", neighbours.getLeader());
    }

    /**
     * Sends a message to another node identified by its nickname.
     *
     * @param toNick   The nickname of the recipient node.
     * @param message  The message content to send.
     */
    public void sendMessage(String toNick, String message) {
        if (isKilled || isLeft) {
            log.warn("Cannot send message: node is inactive (killed or left).");
            return;
        }
        log.info("Send message from {} to {} -> {}", nickname, toNick, message);
        communicationHub.sendMessageTo(toNick, nickname, message);
    }

    /**
     * Gracefully leaves the network by notifying all neighbors and clearing internal state.
     */
    public void leaveNetwork() {
        if (isLeft) {
            log.warn("Already left the network.");
            return;
        }
        isLeft = true;
        setActive(false);
        communicationHub.notifyLeave(myAddress);
        neighbours.getKnownNodes().clear();
        neighbours.setLeader(null);
        log.info("Node {} has left the network. Neighbors cleared, leader set to null.", myAddress);
    }

    /**
     * Abruptly kills the node, making it unresponsive without notifying neighbors.
     */
    public void killNode() {
        if (isKilled) {
            log.warn("Node {} is already killed.", myAddress);
            return;
        }
        isKilled = true;
        setActive(false);
        stopRMI();
        // We do not notify others ourselves, because the node "died" abruptly
        // Clear neighbors & leader in the local node
        neighbours.getKnownNodes().clear();
        neighbours.setLeader(null);
        log.warn("Node {} is killed/unresponsive. Neighbors cleared, leader set to null.", myAddress);
    }

    /**
     * Revives a previously killed node by restarting RMI and notifying neighbors.
     */
    public void reviveNode() {
        if (!isKilled) {
            log.warn("Node {} is not killed, cannot revive. Use 'leave' or 'join' if needed.", myAddress);
            return;
        }
        isKilled = false;
        setActive(true);
        startRMI();
        communicationHub.notifyRevive(myAddress);

        // Also clear neighbors & leader for a fresh start
        neighbours.getKnownNodes().clear();
        neighbours.setLeader(null);
        log.warn("Node {} has been revived. Neighbors cleared, leader set to null.", myAddress);
    }

    /**
     * Checks if the node is currently active.
     *
     * @return {@code true} if the node is active, {@code false} otherwise.
     */
    public boolean isActive() {
        return isActive && !isKilled && !isLeft;
    }

    /**
     * Retrieves the current status of the node, including its configuration and state.
     *
     * @return A string representation of the node's status.
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Node status:\n");
        sb.append(" Nickname: ").append(nickname).append("\n");
        sb.append(" NodeId:   ").append(nodeId).append("\n");
        sb.append(" Address:  ").append(myAddress).append("\n");
        sb.append(" ApiPort:  ").append(apiPort).append("\n");
        sb.append(" RMIPort:  ").append(myPort).append("\n");
        sb.append(" Active:   ").append(isActive()).append("\n");
        sb.append(" Killed:   ").append(isKilled).append("\n");
        sb.append(" Left:     ").append(isLeft).append("\n");
        sb.append(" Leader:   ").append(neighbours.getLeader()).append("\n");
        sb.append(" Neighbors: ");
        for (Address a : neighbours.getKnownNodes()) {
            if (!a.equals(myAddress)) {
                sb.append(a).append(" ");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Logs the current status of the node.
     */
    public void printStatus() {
        log.info("\n{}", getStatus());
    }

    /**
     * Returns a string representation of the node, including its state and configuration.
     *
     * @return A string describing the node.
     */
    @Override
    public String toString() {
        return "Node[" + nickname +
                ", Active=" + isActive() +
                ", Killed=" + isKilled +
                ", Left=" + isLeft +
                ", nodeId=" + nodeId +
                ", address=" + myAddress +
                ", leader=" + neighbours.getLeader() +
                "]";
    }

    /**
     * The entry point of the application. Initializes and runs a Node instance.
     *
     * @param args Command-line arguments for node configuration.
     */
    public static void main(String[] args) {
        Node node = new Node(args);
        node.run();
    }
}
