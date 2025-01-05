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

    // !!! Убираем захардкоженный localhost:
    private String myIP;              // Будет определяться по nodeId (см. getIpForNodeId)
    private int rmiPort = 3010;       // RMI-порт (будет переопределён)
    private int apiPort = 7010;       // API-порт (будет переопределён)

    private String otherNodeIP = "127.0.0.1";  // (необязательно использовать)
    private int otherNodeRMIPort = 3010;       // (необязательно использовать)

    private long nodeId = 0;
    private Address address;
    private DSNeighbours myNeighbours;
    private MessageReceiver myMessageReceiver;
    private CommunicationHub myCommHub;
    private ConsoleHandler myConsoleHandler;
    private APIHandler myAPIHandler;
    private BullyAlgorithm bully;

    // -------------------------------------------
    // Метод, возвращающий IP для каждого узла ID=1..5
    // -------------------------------------------
    private String getIpForNodeId(long id) {
        switch ((int) id) {
            case 1: return "192.168.56.105";
            case 2: return "192.168.56.154";
            case 3: return "192.168.56.106";
            case 4: return "192.168.56.107"; // TODO need to create virtual box
            case 5: return "192.168.56.108"; // TODO need to create virtual box
            default:
                // Если вдруг ID вне 1..5, пусть будет localhost
                return "127.0.0.1";
        }
    }

    public Node(String[] args) {
        if (args.length >= 1) {
            try {
                nodeId = Long.parseLong(args[0]);
                // Формируем RMI-порт и API-порт на базе nodeId
                rmiPort = 50050 + (int) nodeId;
                apiPort = 7000 + (int) nodeId;

                // Определяем наш IP на основе nodeId
                this.myIP = getIpForNodeId(nodeId);

                // Инициализируем список соседей
                myNeighbours = new DSNeighbours();

                log.info(GREEN + "Node initialized with ID {} on ports RMI: {}, API: {}. My IP = {}",
                        nodeId, rmiPort, apiPort, myIP);
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
        // Ставим RMI hostname = наш IP
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
                UnicastRemoteObject.unexportObject(this.myMessageReceiver, true);
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

        // Пытаемся подключиться к узлам 1..5 (кроме себя)
        for (int i = 1; i <= 5; i++) {
            if (i == nodeId) continue;

            // Берём IP этого узла i
            String neighbourIp = getIpForNodeId(i);
            int neighbourPort = 50050 + i;
            Address neighbourAddress = new Address(neighbourIp, neighbourPort, (long) i);

            try {
                NodeCommands remoteNode = getCommHub().getRMIProxy(neighbourAddress);
                // Вызываем join(...) на том узле
                remoteNode.join(address);
                // Добавляем соседа в локальный список
                myNeighbours.addNewNode(neighbourAddress);
                hasNeighbours = true;
                log.info(GREEN + "Connected to node {} at IP {}" + RESET, i, neighbourIp);
            } catch (RemoteException e) {
                log.debug(YELLOW + "Cannot connect to node {}: {}" + RESET, i, e.getMessage());
            }
        }

        logNeighboursInfo();

        if (!hasNeighbours) {
            log.info("No neighbours found. Starting election for node {}...", nodeId);
            bully.startElection();
        } else {
            checkLeader();
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
        // Вызываем startMessageReceiver() — он уже создаст RMI-сервер
        startMessageReceiver();

        // Создаём CommunicationHub, консоль, API и т.д.
        myCommHub = new CommunicationHub(this);
        bully = new BullyAlgorithm(this);

        myConsoleHandler = new ConsoleHandler(this);
        myAPIHandler = new APIHandler(this, apiPort);

        // Подключаемся к другим узлам (по IP)
        tryConnectToTopology();

        // Стартуем HTTP-сервер (Javalin)
        myAPIHandler.start();
        // Стартуем консоль в отдельном потоке
        new Thread(myConsoleHandler).start();

        // Печатаем статус для наглядности
        printStatus();
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
            myNeighbours.addNewNode(otherNodeAddr);

            log.info(GREEN + "Node {} joined the network via {}", address, otherNodeAddr);

            Address currentLeader = myNeighbours.getLeaderNode();
            if (currentLeader == null || otherNodeAddr.getNodeID() > currentLeader.getNodeID()) {
                log.info("Node {} might supersede current leader ({}). Starting Bully election...",
                        otherNodeAddr.getNodeID(),
                        currentLeader == null ? "null" : currentLeader.getNodeID());
                bully.startElection();
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
                myNeighbours.setLeaderNode(null);
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
            int attempts = 3;
            boolean messageSent = false;

            while (attempts > 0) {
                try {
                    getCommHub().getRMIProxy(receiverAddr).sendMessage(nodeId, (int) receiverId, content);
                    log.info(GREEN + "Message sent from {} to {}: {}", nodeId, receiverId, content);
                    System.out.println("Message sent to Node " + receiverId);
                    messageSent = true;
                    break;
                } catch (RemoteException e) {
                    log.warn(YELLOW + "Failed to send message to node {}. Attempts left: {}", receiverId, attempts - 1);
                    attempts--;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        log.error(RED + "Message sending interrupted.");
                        break;
                    }
                }
            }

            if (!messageSent) {
                System.out.println("Node " + receiverId + " is unreachable. Removing from neighbours...");
                log.warn(RED + "Node {} is unreachable. Removing from neighbours.", receiverId);
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
        startMessageReceiver();
    }

    public void kill() {
        stopRMI();
        System.out.println("Node " + nodeId + " killed (no proper logout).");
        log.info(GREEN + "Node {} killed (no proper logout).", nodeId);
    }

    public void leave() {
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
        System.out.println("Node " + nodeId + " has left the network.");
        log.info("Node {} successfully left the network.", nodeId);
    }

    public void revive() {
        System.out.println("Reviving node " + nodeId + "...");
        log.info("Reviving node {}...", nodeId);

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