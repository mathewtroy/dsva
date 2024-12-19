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

@Slf4j
@Getter
@Setter
public class Node implements Runnable {
    public static final String COMM_INTERFACE_NAME = "DSVNode";
    public static Node thisNode = null;

    private boolean isCoordinator = false;
    private boolean receivedOK = false;
    private boolean voting = false;

    private String nickname = "Unknown";
    private String myIP = "127.0.0.1";
    private int rmiPort = 3010; // RMI-порт
    private int apiPort = 7010; // API-порт
    private String otherNodeIP = "127.0.0.1";
    private int otherNodeRMIPort = 3010;

    private long nodeId = 0;
    private Address address;
    private DSNeighbours myNeighbours;
    private MessageReceiver myMessageReceiver;
    private CommunicationHub myCommHub;
    private ConsoleHandler myConsoleHandler;
    private APIHandler myAPIHandler;
    private BullyAlgorithm bully;

    public Node(String[] args) {

        myNeighbours = new DSNeighbours();

        if (args.length >= 4) {
            nickname = args[0];
            myIP = args[1];
            rmiPort = Integer.parseInt(args[2]);
            apiPort = Integer.parseInt(args[3]);

            for (int i = 4; i < args.length; i += 3) {
                String neighbourIP = args[i];
                int neighbourRMIPort = Integer.parseInt(args[i + 1]);
                long neighbourId = Long.parseLong(args[i + 2]);
                myNeighbours.addNewNode(new Address(neighbourIP, neighbourRMIPort, neighbourId));
            }
        } else {
            log.warn("Wrong number of command line parameters - using default values: nickname={}, ip={}, rmiPort={}, apiPort={}",
                    nickname, myIP, rmiPort, apiPort);
        }
    }




    private long generateId(String ip, int port) {
        String[] array = ip.split("\\.");
        long id = 0;
        for (String s : array) {
            long temp = Long.parseLong(s);
            id = id * 1000 + temp;
        }
        if (id == 0) {
            id = 666000666000L;
        }
        id = id + port * 1000000000000L;
        return id;
    }

    private void startMessageReceiver() {
        System.setProperty("java.rmi.server.hostname", address.getHostname());

        try {
            if (this.myMessageReceiver == null) {
                this.myMessageReceiver = new MessageReceiver(this);
            }

            NodeCommands skeleton = (NodeCommands) UnicastRemoteObject.exportObject(
                    this.myMessageReceiver, rmiPort);

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
                registry.unbind(COMM_INTERFACE_NAME);
                UnicastRemoteObject.unexportObject(this.myMessageReceiver, false);
                this.myMessageReceiver = null;
            }
        } catch (Exception e) {
            log.error("Stopping message listener failed: {}", e.getMessage(), e);
        }
        log.info("Message listener stopped.");
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Node[id:'").append(nodeId).append("', ");
        result.append("nick:'").append(nickname).append("', ");
        result.append("myIP:'").append(myIP).append("', ");
        result.append("rmiPort:'").append(rmiPort).append("', ");
        result.append("apiPort:'").append(apiPort).append("'");

        if (!myIP.equals(otherNodeIP) || rmiPort != otherNodeRMIPort) {
            result.append(", otherNodeIP:'").append(otherNodeIP).append("', ");
            result.append("otherNodeRMIPort:'").append(otherNodeRMIPort).append("'");
        }

        result.append("]");
        return result.toString();
    }

    public String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Status: ").append(this).append(" with address ").append(address).append("\n");
        if (myNeighbours != null && !myNeighbours.getNeighbours().isEmpty()) {
            status.append("    with neighbours ").append(myNeighbours);
        } else {
            status.append("    no neighbours yet");
        }
        return status.toString();
    }

    public void printStatus() {
        System.out.println(getStatus());
    }

    @Override
    public void run() {
        nodeId = generateId(myIP, rmiPort);
        address = new Address(myIP, rmiPort, nodeId);
        myNeighbours = new DSNeighbours();
        bully = new BullyAlgorithm(this);
        printStatus();
        startMessageReceiver();
        myCommHub = new CommunicationHub(this);
        myConsoleHandler = new ConsoleHandler(this);
        myAPIHandler = new APIHandler(this, apiPort);

        // If other nodes are specified for joining
        if (!(myIP.equals(otherNodeIP) && (rmiPort == otherNodeRMIPort))) {
            this.join(new Address(otherNodeIP, otherNodeRMIPort));
        }

        myAPIHandler.start();
        new Thread(myConsoleHandler).start();
    }

    public DSNeighbours getNeighbours() {
        return this.myNeighbours;
    }

    public CommunicationHub getCommHub() {
        return this.myCommHub;
    }

    public void join(Address otherNodeAddr) {
        try {
            NodeCommands remoteNode = myCommHub.getRMIProxy(otherNodeAddr);
            myNeighbours = remoteNode.join(address);
            log.info("Node {} joined the network via {}", address, otherNodeAddr);
        } catch (RemoteException e) {
            log.error("Failed to join {}: {}", otherNodeAddr, e.getMessage(), e);
        }
        System.out.println("Neighbours after JOIN: " + myNeighbours);
    }

    public void startElection() {
        bully.startElection();
    }

    public void checkLeader() {
        Address leader = myNeighbours.getLeaderNode();
        if (leader != null) {
            try {
                getCommHub().getRMIProxy(leader).checkStatusOfLeader(nodeId);
            } catch (RemoteException e) {
                System.out.println("Leader is unreachable, starting election...");
                startElection();
            }
        } else {
            System.out.println("No leader present, starting election...");
            startElection();
        }
    }

    public void sendMessageToNode(long receiverId, String content) {
        Address receiverAddr = null;
        for (Address addr : myNeighbours.getNeighbours()) {
            if (addr.getNodeID() == receiverId) {
                receiverAddr = addr;
                break;
            }
        }

        if (receiverAddr != null) {
            try {
                getCommHub().getRMIProxy(receiverAddr).sendMessage(nodeId, (int) receiverId, content);
                log.info("Message sent from {} to {}: {}", nodeId, receiverId, content);
                System.out.println("Message sent to Node " + receiverId);
            } catch (RemoteException e) {
                log.error("Error sending message to node {}: {}", receiverId, e.getMessage(), e);
                System.out.println("Error while sending message. Please try again.");
            }
        } else {
            System.out.println("Receiver not found in neighbours.");
        }
    }

    public void leaveNetwork() {
        for (Address neighbour : myNeighbours.getNeighbours()) {
            try {
                getCommHub().getRMIProxy(neighbour).notifyAboutLogOut(address);
            } catch (RemoteException e) {
                log.warn("Failed to notify node {} about logout.", neighbour, e);
            }
        }
        stopRMI();
        System.exit(0);
    }

    public void stopRMI() {
        stopMessageReceiver();
    }

    public void startRMI() {
        startMessageReceiver();
    }

    public void kill() {
        stopRMI();
        System.out.println("Node " + nodeId + " killed (no proper logout).");
        log.info("Node {} killed (no proper logout).", nodeId);
    }

    public void revive() {
        startRMI();
        System.out.println("Node " + nodeId + " revived.");
        log.info("Node {} revived.", nodeId);
    }

    public void setDelay(long delay) {
        getCommHub().setMessageDelay(delay);
        System.out.println("Delay set to " + delay + " ms for node " + nodeId);
        log.info("Delay set to {} ms on node {}.", delay, nodeId);
    }

    public static void main(String[] args) {
        thisNode = new Node(args);
        thisNode.run();
    }
}
