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

@Slf4j
@Getter
@Setter
public class CommunicationHub {
    private final Node node;

    public CommunicationHub(Node node) {
        this.node = node;
    }

    public NodeCommands getProxy(Address addr) throws RemoteException {
        if (addr.compareTo(node.getAddress()) == 0) {
            return node.getMessageReceiver();
        }
        try {
            Registry registry = LocateRegistry.getRegistry(addr.getHostname(), addr.getPort());
            return (NodeCommands) registry.lookup(Node.COMM_INTERFACE_NAME);
        } catch (NotBoundException e) {
            throw new RemoteException("Node " + addr + " not bound: " + e.getMessage());
        }
    }

    public void sendElectionToBiggerNodes() {
        DSNeighbours ds = node.getNeighbours();
        for (Address a : ds.getKnownNodes()) {
            if (a.compareTo(node.getAddress()) != 0) {
                long theirId = node.computeId(a.getHostname(), a.getPort());
                if (theirId > node.getNodeId() && node.isActive()) {
                    try {
                        NodeCommands proxy = getProxy(a);
                        proxy.startElection(node.getNodeId());
                        System.out.println("Sent startElection to " + a);
                    } catch (RemoteException e) {
                        System.err.println("Error sending startElection to " + a + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    public void sendRespondOk(long candidateId) {
        DSNeighbours ds = node.getNeighbours();
        for (Address a : ds.getKnownNodes()) {
            long theirId = node.computeId(a.getHostname(), a.getPort());
            if (theirId == candidateId) {
                try {
                    NodeCommands proxy = getProxy(a);
                    proxy.respondOk(node.getNodeId());
                    System.out.println("Sent respondOk to " + a);
                } catch (RemoteException e) {
                    System.err.println("Error sending respondOk to " + a + ": " + e.getMessage());
                }
                break;
            }
        }
    }

    public void broadcastLeader() {
        DSNeighbours ds = node.getNeighbours();
        for (Address a : ds.getKnownNodes()) {
            try {
                NodeCommands proxy = getProxy(a);
                proxy.announceLeader(node.getNodeId(), node.getAddress());
                System.out.println("Announced leader to " + a);
            } catch (RemoteException e) {
                System.err.println("Error announcing leader to " + a + ": " + e.getMessage());
            }
        }
    }

    public void notifyLeave(Address leavingNode) {
        DSNeighbours ds = node.getNeighbours();
        for (Address a : ds.getKnownNodes()) {
            if (a.compareTo(leavingNode) != 0) {
                try {
                    NodeCommands proxy = getProxy(a);
                    proxy.leave(leavingNode);
                    System.out.println("Notified " + a + " about leaving node " + leavingNode);
                } catch (RemoteException e) {
                    System.err.println("Error notifying " + a + " about leaving node " + leavingNode + ": " + e.getMessage());
                }
            }
        }
    }

    public void notifyRevive(Address revivedNode) {
        DSNeighbours ds = node.getNeighbours();
        for (Address a : ds.getKnownNodes()) {
            if (a.compareTo(revivedNode) != 0) {
                try {
                    NodeCommands proxy = getProxy(a);
                    proxy.revive(revivedNode);
                    System.out.println("Notified " + a + " about revived node " + revivedNode);
                } catch (RemoteException e) {
                    System.err.println("Error notifying " + a + " about revived node " + revivedNode + ": " + e.getMessage());
                }
            }
        }
    }

    public void sendMessageTo(String toNick, String fromNick, String message) {
        // Implement mapping from nickname to Address if needed
        // For simplicity, send to all or implement as required
        DSNeighbours ds = node.getNeighbours();
        for (Address a : ds.getKnownNodes()) {
            try {
                NodeCommands proxy = getProxy(a);
                proxy.sendMessage(fromNick, toNick, message);
                System.out.println("Sent message to " + a);
            } catch (RemoteException e) {
                System.err.println("Error sending message to " + a + ": " + e.getMessage());
            }
        }
    }
}
