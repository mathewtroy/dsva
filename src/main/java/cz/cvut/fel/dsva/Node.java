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
            System.err.println("Wrong number of parameters - using defaults.");
            apiPort = 5000 + myPort;
        }
    }

    private long generateId(String ip, int port) {
        String[] parts = ip.split("\\.");
        long id = 0;
        for (String part : parts) {
            try {
                long num = Long.parseLong(part);
                id = id * 1000 + num;
            } catch (NumberFormatException e) {
                System.err.println("Error parsing IP part: " + part);
            }
        }
        id += port * 1000000000000L;
        return id;
    }

    public long computeId(String ip, int port) {
        return generateId(ip, port);
    }

    public Address getAddress() {
        return myAddress;
    }

    @Override
    public void run() {
        nodeId = generateId(myIP, myPort);
        myAddress = new Address(myIP, myPort);
        neighbours = new DSNeighbours(myAddress);

        printStatus();
        startRMI();
        communicationHub = new CommunicationHub(this);

        if (!(myIP.equals(otherNodeIP) && myPort == otherNodePort)) {
            join(otherNodeIP, otherNodePort);
        } else {
            neighbours.setLeader(myAddress);
            System.out.println("I am the first node. I become the leader: " + myAddress);
        }

        Thread consoleThread = new Thread(new ConsoleHandler(this));
        consoleThread.start();

        Thread apiThread = new Thread(new APIHandler(this, apiPort));
        apiThread.start();

        while (isActive && !isKilled && !isLeft) {
            try {
                Thread.sleep(8000);
            } catch (InterruptedException e) {
                System.err.println("Main loop interrupted.");
            }
        }
        System.out.println("Node run() method finished.");
    }

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
                System.out.println("No registry found, creating new one on port " + myPort);
                registry = LocateRegistry.createRegistry(myPort);
                registry.rebind(COMM_INTERFACE_NAME, stub);
            }
            System.out.println("RMI started on port " + myPort);
        } catch (Exception e) {
            System.err.println("startRMI error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry(myPort);
            registry.unbind(COMM_INTERFACE_NAME);
            UnicastRemoteObject.unexportObject(messageReceiver, false);
            messageReceiver = null;
            System.out.println("RMI stopped on port " + myPort);
        } catch (Exception e) {
            System.err.println("stopRMI error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void join(String ip, int port) {
        Address other = new Address(ip, port);
        try {
            NodeCommands proxy = communicationHub.getProxy(other);
            DSNeighbours updated = proxy.join(myAddress);
            for (Address a : updated.getKnownNodes()) {
                neighbours.addNode(a);
            }
            neighbours.setLeader(updated.getLeader());
            System.out.println("Joined network. Neighbors: " + neighbours);
            printStatus();
        } catch (RemoteException e) {
            System.err.println("join error: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    public void printStatus() {
        System.out.println(getStatus());
    }

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

    public void startElection() {
        if (!isActive() || isKilled || isLeft) {
            System.out.println("Cannot start election: node is not active.");
            return;
        }
        internalStartElection();
    }

    public void internalStartElection() {
        if (electionInProgress) {
            System.out.println("Election already in progress.");
            return;
        }
        electionInProgress = true;
        System.out.println("Starting Bully election. My ID=" + nodeId);
        communicationHub.sendElectionToBiggerNodes();

        new Thread(() -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                System.err.println("Election wait interrupted.");
            }
            if (electionInProgress) {
                System.out.println("No higher node responded. I am the new leader.");
                neighbours.setLeader(myAddress);
                communicationHub.broadcastLeader();
                electionInProgress = false;
                printStatus();
            }
        }).start();
    }

    public void checkLeader() {
        System.out.println("Current leader: " + neighbours.getLeader());
    }

    public void sendMessage(String toNick, String message) {
        if (isKilled || isLeft) {
            System.out.println("Cannot send message: node is inactive.");
            return;
        }
        System.out.println("Send message: from " + nickname + " to " + toNick + " -> " + message);
        communicationHub.sendMessageTo(toNick, nickname, message);
    }

    public void leaveNetwork() {
        if (isLeft) {
            System.out.println("Already left the network.");
            return;
        }
        isLeft = true;
        setActive(false);
        communicationHub.notifyLeave(myAddress);
        System.out.println("Node " + myAddress + " has left the network.");
    }

    public void killNode() {
        if (isKilled) {
            System.out.println("Node is already killed.");
            return;
        }
        isKilled = true;
        setActive(false);
        communicationHub.notifyKill(myAddress);
        System.out.println("Node " + myAddress + " is killed (unresponsive).");
    }

    public void reviveNode() {
        if (!isKilled) {
            System.out.println("Node is not killed, cannot revive. Use 'leave' or 'join' if needed.");
            return;
        }
        isKilled = false;
        setActive(true);
        communicationHub.notifyRevive(myAddress);
        System.out.println("Node " + myAddress + " has been revived.");
    }

    public boolean isActive() {
        return isActive && !isKilled && !isLeft;
    }



    public static void main(String[] args) {
        Node node = new Node(args);
        node.run();
    }
}
