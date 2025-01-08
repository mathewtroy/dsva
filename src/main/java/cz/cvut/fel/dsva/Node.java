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
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@Getter
@Setter
public class Node implements Runnable {

    public static final String COMM_INTERFACE_NAME = "DSVNode";
    public static Node thisNode = null;

    // Flags
    private boolean isKilled = false;
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

    // Получение IP для узлов 1..5
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
            log.info("Topology has been reset.");
            log.info("Topology reset on node {}.", nodeId);
        }
    }

    private void startLeaderHeartbeat() {
        Timer heartbeatTimer = new Timer(); // Не-демон таймер
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    log.info("Heartbeat TimerTask executing for Node {}", nodeId);
                    if (isKilled || isLeft) {
                        log.info("Node {} is inactive, skipping heartbeat check.", nodeId);
                        return;
                    }

                    Address leader = myNeighbours.getLeaderNode();
                    if (leader != null) {
                        log.info("Node {} is checking status of leader Node {}", nodeId, leader.getNodeID());
                        try {
                            NodeCommands remoteLeader = myCommHub.getRMIProxy(leader);
                            remoteLeader.checkStatusOfLeader(nodeId);
                            log.info("Checked status of leader Node {}", leader.getNodeID());
                        } catch (RemoteException e) {
                            log.warn("Leader Node {} is unreachable. Initiating election.", leader.getNodeID());
                            myNeighbours.setLeaderNode(null);
                            notifyAllNeighboursOfLeaderDeath();
                            startElection();
                        }
                    } else {
                        log.info("No leader present. Starting election.");
                        startElection();
                    }
                } catch (Exception e) {
                    log.error("Exception in heartbeat TimerTask: {}", e.getMessage(), e);
                }
            }
        }, 0, 10000); // Проверка каждые 10 секунд
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
        log.info(getStatus());
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

        // Start leader heartbeat
        startLeaderHeartbeat();

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
                log.info("Leader is alive: {}", leader);
                log.info("Leader Node {} is alive.", leader.getNodeID());
            } catch (RemoteException e) {
                log.info("Leader is unreachable, starting election...");
                log.warn("Leader Node {} is unreachable. Starting election.", leader.getNodeID());
                myNeighbours.setLeaderNode(null);
                notifyAllNeighboursOfLeaderDeath();
                startElection();
            }
        } else {
            log.info("No leader present, starting election...");
            startElection();
        }
    }

    public void notifyAllNeighboursOfLeaderDeath() {
        log.info("Notifying all neighbors about the leader's death.");
        for (Address neighbour : myNeighbours.getNeighbours()) {
            myCommHub.sendLeaderDeathMessage(neighbour, myNeighbours.getLeaderNode());
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
                log.info("Node {} is unreachable. Removing from neighbours...", receiverId);
                myNeighbours.removeNode(receiverAddr);
            }
        } else {
            log.info("Receiver not found in neighbours.");
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

    public void killLeader() {
        if (!isCoordinator) {
            log.info("Node {} is not the leader. Cannot perform killLeader.", nodeId);
            log.info("You are not the leader. Cannot kill the leader.");
            return;
        }

        log.info("Leader Node {} is being killed.", nodeId);
        log.info("Killing the leader node...");

        // Notify nodes that the leader has been killed
        notifyAllNeighboursOfLeaderDeath();

        // Perform kill operation
        kill();
    }

    public void leaveLeader() {
        if (!isCoordinator) {
            log.info("Node {} is not the leader. Cannot perform leaveLeader.", nodeId);
            log.info("You are not the leader. Cannot leave the leader role.");
            return;
        }

        log.info("Leader Node {} is leaving the leader role.", nodeId);
        log.info("Leaving the leader role...");

        // Notify nodes that the leader has been killed
        notifyAllNeighboursOfLeaderDeath();

        // Step down as leader
        isCoordinator = false;
        myNeighbours.setLeaderNode(null);

        // Start a new election since the leader is stepping down
        startElection();

        log.info("Leader Node {} has left the leader role. Election started.", nodeId);
        log.info("Leader has left. Election started.");
    }

    public void kill() {
        if (isKilled) {
            log.info("Node {} is already KILLED.", nodeId);
            return;
        }

        // Check if the node is the leader before killing
        if (isCoordinator) {
            log.warn("Leader Node {} is being killed. Initiating leader kill process.", nodeId);
            killLeader();
            return;
        }

        // Hard kill
        isKilled = true;
        isLeft = false;
        stopRMI();
        log.info("Node {} killed (no proper logout).", nodeId);
    }

    public void leave() {
        if (isKilled || isLeft) {
            log.info("Node {} is already inactive (killed or left).", nodeId);
            return;
        }

        // Check if the node is the leader before leaving
        if (isCoordinator) {
            log.warn("Leader Node {} is leaving the network gracefully.", nodeId);
            leaveLeader();
            return;
        }

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

        log.info("Node {} has left the network.", nodeId);

        // If the node was the leader, initiate election
        if (isCoordinator) {
            myNeighbours.setLeaderNode(null);
            notifyAllNeighboursOfLeaderDeath();
            startElection();
        }
    }

    public void revive() {
        // If node is neither killed nor left, do nothing
        if (!isKilled && !isLeft) {
            log.info("Node {} is not killed/left, no need to revive.", nodeId);
            return;
        }
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
            log.info("Leader found: Node ID {}", currentLeader.getNodeID());
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
                log.info("Higher ID nodes detected. Connecting as a regular node...");
                log.info("Node {} detected higher ID nodes. Connecting as a regular node.", nodeId);
                checkLeader();
            } else {
                log.info("No higher ID nodes detected. Starting election...");
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

        log.info("Node {} has been revived.", nodeId);
        log.info("Node {} successfully revived.", nodeId);
    }

    public static void main(String[] args) {
        thisNode = new Node(args);
        thisNode.run();
    }
}
