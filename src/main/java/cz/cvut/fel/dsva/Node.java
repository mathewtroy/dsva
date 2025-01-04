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

import static cz.cvut.fel.dsva.Color.*;

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
        if (args.length >= 1) {
            try {
                nodeId = Long.parseLong(args[0]);
                rmiPort = 50050 + (int) nodeId; // Порты начинаются с 50051
                apiPort = 7000 + (int) nodeId; // API-порты начинаются с 7001
                myNeighbours = new DSNeighbours();
                log.info(GREEN + "Node initialized with ID {} on ports RMI: {}, API: {}", nodeId, rmiPort, apiPort);
            } catch (NumberFormatException e) {
                log.error(RED + "Invalid node ID provided: {}", args[0], e);
                System.exit(1);
            }
        } else {
            log.error(RED + "Node ID is required as the first parameter.");
            System.exit(1);
        }
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
                log.info(GREEN + "Creating new RMI registry on port {}.", rmiPort);
                registry = LocateRegistry.createRegistry(rmiPort);
                registry.rebind(COMM_INTERFACE_NAME, skeleton);
            }
        } catch (Exception e) {
            log.error(RED + "Starting message listener failed: {}", e.getMessage(), e);
        }
        log.info(GREEN + "Message listener started on {}:{}.", address.getHostname(), rmiPort);
    }

    private void stopMessageReceiver() {
        try {
            if (this.myMessageReceiver != null) {
                Registry registry = LocateRegistry.getRegistry(rmiPort);
                try {
                    registry.unbind(COMM_INTERFACE_NAME);
                } catch (NotBoundException e) {
                    log.warn(YELLOW + "RMI object was not bound: {}", e.getMessage());
                }
                UnicastRemoteObject.unexportObject(this.myMessageReceiver, true); // Убедитесь, что объект удаляется
                this.myMessageReceiver = null;
            }
        } catch (Exception e) {
            log.error(RED + "Stopping message listener failed: {}", e.getMessage(), e);
        }
        log.info(GREEN + "Message listener stopped.");
    }


    private void tryConnectToTopology() {
        log.info(GREEN + "Trying to connect to all possible nodes in topology..." + RESET);

        boolean hasNeighbours = false;

        for (int i = 1; i <= 5; i++) {
            if (i == nodeId) continue; // Пропускаем собственный ID

            Address neighbourAddress = new Address(myIP, 50050 + i, (long) i);
            try {
                NodeCommands remoteNode = getCommHub().getRMIProxy(neighbourAddress);
                remoteNode.join(address);
                myNeighbours.addNewNode(neighbourAddress);
                hasNeighbours = true;
                log.info(GREEN + "Connected to node {}." + RESET, i);
            } catch (RemoteException e) {
                log.debug(YELLOW + "Cannot connect to node {}: {}" + RESET, i, e.getMessage());
            }
        }

        logNeighboursInfo();

        if (!hasNeighbours) {
            log.info(GREEN + "No neighbours found. Declaring this node as leader.");
            isCoordinator = true;
            myNeighbours.setLeaderNode(address);
        } else {
            checkLeader(); // Проверяем лидера среди соседей
        }
    }



    private void logNeighboursInfo() {
        log.info(CYAN + "Node {} neighbors info:" + RESET, nodeId);
        for (Address neighbour : myNeighbours.getNeighbours()) {
            log.info(CYAN + "    Neighbor -> ID: {}, Hostname: {}, Port: {}" + RESET,
                    neighbour.getNodeID(), neighbour.getHostname(), neighbour.getPort());
        }
    }


    public void resetTopology() {
        if (myNeighbours != null) {
            myNeighbours.getNeighbours().clear();
            myNeighbours.setLeaderNode(null);
            System.out.println("Topology has been reset.");
            log.info(GREEN + "Topology reset on node {}.", nodeId);
        }
    }


    public String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Node ID: ").append(nodeId).append("\n");
        status.append("Address: ").append(address).append("\n");
        status.append("Neighbours:\n");
        for (Address neighbour : myNeighbours.getNeighbours()) {
            status.append("    ").append(neighbour).append("\n");
        }
        if (myNeighbours.isLeaderPresent()) {
            Address leader = myNeighbours.getLeaderNode();
            status.append("Leader: Node ID ").append(leader.getNodeID()).append(", Address: ").append(leader).append("\n");
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
        address = new Address(myIP, rmiPort, nodeId);
        myNeighbours = new DSNeighbours();
        bully = new BullyAlgorithm(this);
        printStatus();
        startMessageReceiver();
        myCommHub = new CommunicationHub(this);
        myConsoleHandler = new ConsoleHandler(this);
        myAPIHandler = new APIHandler(this, apiPort);

        tryConnectToTopology();

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
            myNeighbours.addNewNode(otherNodeAddr); // Добавляем нового соседа

            log.info(GREEN + "Node {} joined the network via {}", address, otherNodeAddr);

            Address currentLeader = myNeighbours.getLeaderNode();

            // Проверяем, если новый узел имеет более высокий ID, чем текущий лидер
            if (currentLeader == null || otherNodeAddr.getNodeID() > currentLeader.getNodeID()) {
                log.info(YELLOW + "Node {} has a higher ID than the current leader. Starting election...", otherNodeAddr.getNodeID());
                myNeighbours.setLeaderNode(otherNodeAddr); // Назначаем нового узла лидером
                notifyAllNodesAboutNewLeader(otherNodeAddr); // Уведомляем всех соседей
            }
        } catch (RemoteException e) {
            log.error(RED + "Failed to join {}: {}", otherNodeAddr, e.getMessage(), e);
        }
    }



    public void startElection() {
        bully.startElection();
    }

    public void checkLeader() {
        Address leader = myNeighbours.getLeaderNode();
        if (leader != null) {
            try {
                getCommHub().getRMIProxy(leader).checkStatusOfLeader(nodeId);
                System.out.println("Leader is alive: " + leader);
            } catch (RemoteException e) {
                System.out.println("Leader is unreachable, starting election...");
                myNeighbours.setLeaderNode(null); // Сбрасываем лидера
                startElection(); // Запускаем выборы
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
                log.info(GREEN + "Message sent from {} to {}: {}", nodeId, receiverId, content);
                System.out.println("Message sent to Node " + receiverId);
            } catch (RemoteException e) {
                log.error(RED + "Error sending message to node {}: {}", receiverId, e.getMessage(), e);
                System.out.println("Error while sending message. Please try again.");
            }
        } else {
            System.out.println("Receiver not found in neighbours.");
        }
    }

    public void notifyAllNodesAboutNewLeader(Address leaderAddress) {
        if (myNeighbours != null && !myNeighbours.getNeighbours().isEmpty()) {
            for (Address neighbour : myNeighbours.getNeighbours()) {
                try {
                    NodeCommands proxy = getCommHub().getRMIProxy(neighbour);
                    proxy.notifyAboutNewLeader(leaderAddress);
                    log.info(GREEN + "Notified node {} about new leader: {}", neighbour.getNodeID(), leaderAddress);
                } catch (RemoteException e) {
                    log.error(RED + "Failed to notify node {} about new leader: {}", neighbour.getNodeID(), e.getMessage());
                }
            }
        } else {
            log.warn(YELLOW + "No neighbours to notify about the new leader.");
        }
        System.out.println("All neighbours notified about new leader: " + leaderAddress);
    }


    public void leaveNetwork() {
        for (Address neighbour : myNeighbours.getNeighbours()) {
            try {
                NodeCommands remoteNode = myCommHub.getRMIProxy(neighbour);
                remoteNode.notifyAboutLogOut(address); // Уведомляем об отключении
            } catch (RemoteException e) {
                log.warn("Failed to notify node {} about logout.", neighbour.getNodeID(), e);
            }
        }
        myNeighbours.getNeighbours().clear(); // Очищаем список соседей
        myNeighbours.setLeaderNode(null);    // Сбрасываем лидера
    }


    public void stopRMI() {
        stopMessageReceiver();
    }

    public void startRMI() {
        startMessageReceiver();
    }

    public void kill() {
        leaveNetwork();
        stopRMI();
        System.out.println("Node " + nodeId + " killed (no proper logout).");
        log.info(GREEN + "Node {} killed (no proper logout).", nodeId);
    }

    public void revive() {
        System.out.println("Reviving node " + nodeId + "...");
        log.info("Reviving node {}...", nodeId);

        // Восстанавливаем RMI
        startRMI();

        // Повторно подключаемся к топологии
        tryConnectToTopology();

        // Проверяем у соседей, есть ли текущий лидер
        Address currentLeader = null;
        for (Address neighbour : myNeighbours.getNeighbours()) {
            try {
                NodeCommands remoteNode = myCommHub.getRMIProxy(neighbour);
                Address leader = remoteNode.getCurrentLeader();
                if (leader != null) {
                    currentLeader = leader;
                    break; // Если найден лидер, выходим из цикла
                }
            } catch (RemoteException e) {
                log.warn("Failed to get leader information from node {}: {}", neighbour.getNodeID(), e.getMessage());
            }
        }

        // Если лидер найден, обновляем его
        if (currentLeader != null) {
            myNeighbours.setLeaderNode(currentLeader);
            System.out.println("Leader found: Node ID " + currentLeader.getNodeID());
            log.info("Node {} found leader: {}", nodeId, currentLeader);
        } else {
            // Если лидера нет, проверяем свой ID
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
                checkLeader(); // Проверяем текущего лидера
            } else {
                System.out.println("No higher ID nodes detected. Starting election...");
                log.info("Node {} is the highest ID node. Initiating election...", nodeId);
                startElection(); // Запускаем выборы
            }
        }

        log.info("Node {} restored with leader: {}", nodeId, myNeighbours.getLeaderNode());


        // Уведомляем соседей о восстановлении
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





    public void setDelay(long delay) {
        getCommHub().setMessageDelay(delay);
        System.out.println("Delay set to " + delay + " ms for node " + nodeId);
        log.info(GREEN + "Delay set to {} ms on node {}.", delay, nodeId);
    }

    public static void main(String[] args) {
        thisNode = new Node(args);
        thisNode.run();
    }
}
