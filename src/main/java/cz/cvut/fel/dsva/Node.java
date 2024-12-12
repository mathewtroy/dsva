package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.DSNeighbours;
import cz.cvut.fel.dsva.base.NodeCommands;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Node implements Runnable {
    // Using logger is strongly recommended (log4j, ...)

    // Name of our RMI "service"
    public static final String COMM_INTERFACE_NAME = "DSVNode";

    // This Node
    public static Node thisNode = null;

    // Initial configuration from commandline
    private String nickname = "Unknown";
    private String myIP = "127.0.0.1";
    private int myPort = 2010;
    private String otherNodeIP = "127.0.0.1";
    private int otherNodePort = 2010;

    // Node Id
    private long nodeId = 0;
    private Address myAddress;
    private DSNeighbours myNeighbours;
    private NodeCommands myMessageReceiver;
    private CommunicationHub myCommHub;
    private ConsoleHandler myConsoleHandler;
    private APIHandler myAPIHandler;

    boolean repairInProgress = false;
    boolean voting = false;


    public Node(String[] args) {
        // handle commandline arguments
        if (args.length == 3) {
            nickname = args[0];
            myIP = otherNodeIP = args[1];
            myPort = otherNodePort = Integer.parseInt(args[2]);
        } else if (args.length == 5) {
            nickname = args[0];
            myIP = args[1];
            myPort = Integer.parseInt(args[2]);
            otherNodeIP = args[3];
            otherNodePort = Integer.parseInt(args[4]);
        } else {
            // something is wrong - use default values
            System.err.println("Wrong number of commandline parameters - using default values.");
        }
    }


    private long generateId(String address, int port) {
        // generates  <port><IPv4_dec1><IPv4_dec2><IPv4_dec3><IPv4_dec4>
        String[] array = myIP.split("\\.");
        long id = 0;
        long shift = 0, temp = 0;
        for(int i = 0 ; i < array.length; i++){
            temp = Long.parseLong(array[i]);
            id = (long) (id * 1000);
            id += temp;
        }
        if (id == 0) {
            // TODO problem with parsing address - handle it
            id = 666000666000l;
        }
        id = id + port*1000000000000l;
        return id;
    }


    private void startMessageReceiver() {
        System.setProperty("java.rmi.server.hostname", myAddress.hostname);

        try {
            if (this.myMessageReceiver == null) {
                this.myMessageReceiver = new MessageReceiver(this);
            }

            // Create instance of remote object and its skeleton
            NodeCommands skeleton = (NodeCommands) UnicastRemoteObject.exportObject(this.myMessageReceiver, 40000+myAddress.port);

            // Create registry and (re)register object name and skeleton in it
            Registry registry = null;
            try {
                registry = LocateRegistry.getRegistry(myAddress.port);
                registry.rebind(COMM_INTERFACE_NAME, skeleton);
            } catch (RemoteException re) {
                // there is no RMI registry - create on
                System.out.println("Creating new RMI registry.");
                registry = LocateRegistry.createRegistry(myAddress.port);
                registry.rebind(COMM_INTERFACE_NAME, skeleton);
            }
        } catch (Exception e) {
            // Something is wrong ...
            System.err.println("Starting message listener - something is wrong: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Message listener is started ...");
    }

    private void stopMessageReceiver() {
        try {
            if (this.myMessageReceiver != null) {
                Registry registry = LocateRegistry.getRegistry(myAddress.port);
                registry.unbind(COMM_INTERFACE_NAME);
                UnicastRemoteObject.unexportObject(this.myMessageReceiver,false);
                this.myMessageReceiver = null;
            }
        } catch (Exception e) {
            // Something is wrong ...
            System.err.println("Stopping message listener - something is wrong: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Message listener is stopped ...");
    }


    @Override
    public String toString() {
        return "Node[id:'"+nodeId+"', " +
                "nick:'"+nickname+"', " +
                "myIP:'"+myIP+"', " +
                "myPort:'"+myPort+"', " +
                "otherNodeIP:'"+otherNodeIP+"', " +
                "otherNodePort:'"+otherNodePort+"']";
    }


    public String getStatus() {
        return "Status: " + this + " with addres " + myAddress + "\n    with neighbours " + myNeighbours;
    }


    public void printStatus() {
        System.out.println(getStatus());
    }


    @Override
    public void run() {
        nodeId = generateId(myIP, myPort);
        myAddress = new Address(myIP, myPort);
        myNeighbours = new DSNeighbours(myAddress);
        printStatus();
        startMessageReceiver();     // TODO null -> exit
        myCommHub = new CommunicationHub(this);   // TODO null -> exit
        myConsoleHandler = new ConsoleHandler(this);
        myAPIHandler = new APIHandler(this, 5000 + myAddress.port);
        if (! ((myIP == otherNodeIP) && (myPort == otherNodePort)) ) {
            // all 5 parameters were filled
            this.join(otherNodeIP, otherNodePort);
        }
        myAPIHandler.start();
        new Thread(myConsoleHandler).run();
    }


    public void join(String otherNodeIP, int otherNodePort) {
        try {
            NodeCommands tmpNode = myCommHub.getRMIProxy(new Address(otherNodeIP, otherNodePort));
            myNeighbours = tmpNode.join(myAddress);
            myCommHub.setActNeighbours(myNeighbours);
        } catch (RemoteException e) {
            e.printStackTrace();
            // TODO Exception -> exit
        }
        System.out.println("Neighbours after JOIN " + myNeighbours);
    }

    public void stopRMI() {
        stopMessageReceiver();
    }

    public void startRMI() {
        startMessageReceiver();
    }

    public void repairTolopogy(Address missingNode) {
        if (repairInProgress == false) {
            repairInProgress = true;
            {
                try {
                    myMessageReceiver.NodeMissing(missingNode);
                } catch (RemoteException e) {
                    // this should not happen
                    e.printStackTrace();
                }
                System.out.println("Topology was repaired " + myNeighbours );
            }
            repairInProgress = false;

            // test leader and if missing start election
            try {
                myCommHub.getLeader().Hello();
            } catch (RemoteException e) {
                // Leader is dead -> start Election
                try {
                    myMessageReceiver.Election(-1);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    public void startElection() {
        try {
            myMessageReceiver.Election(-1);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }


    public void sendHelloToNext() {
        System.out.println("Sending Hello to my Next ...");
        try {
            myCommHub.getNext().Hello();
        } catch (RemoteException e) {
            repairTolopogy(myNeighbours.next);
        }
    }


    public void sendHelloToLeader() {
        System.out.println("Sending Hello to my Leader ...");
        try {
            myCommHub.getLeader().Hello();
        } catch (RemoteException e) {
            repairTolopogy(myNeighbours.leader);
        }
    }


    public void resetTopology() {
        // reset info - start as I am only node
        myNeighbours = new DSNeighbours(myAddress);
    }


    public Address getAddress() {
        return myAddress;
    }


    public DSNeighbours getNeighbours() {
        return myNeighbours;
    }


    public NodeCommands getMessageReceiver() {
        return myMessageReceiver;
    }


    public CommunicationHub getCommHub() {
        return myCommHub;
    }


    public long getNodeId() {
        return nodeId;
    }


    public static void main(String[] args) {
        thisNode = new Node(args);
        thisNode.run();
    }
}
