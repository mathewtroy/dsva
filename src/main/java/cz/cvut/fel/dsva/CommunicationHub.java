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
import java.util.HashSet;
import java.util.Set;

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

    public void broadcastNewNode(Address newAddr) {
        DSNeighbours ds = node.getNeighbours();
        for (Address a : ds.getKnownNodes()) {
            if (!a.equals(newAddr)) {
                try {
                    NodeCommands proxy = getProxy(a);
                    proxy.broadcastNewNode(newAddr);
                    log.info("Broadcasted new node {} to {}", newAddr, a);
                } catch (RemoteException e) {
                    log.error("Error broadcasting new node {} to {}", newAddr, a, e);
                }
            }
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
                        log.info("Sent startElection to {}", a);
                    } catch (RemoteException e) {
                        log.error("Error sending startElection to {}: {}", a, e.getMessage());
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
                    log.info("Sent respondOk to {}", a);
                } catch (RemoteException e) {
                    log.error("Error sending respondOk to {}: {}", a, e.getMessage());
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
                log.info("Announced leader to {}", a);
            } catch (RemoteException e) {
                log.error("Error announcing leader to {}: {}", a, e.getMessage());
            }
        }
    }

    public void notifyLeave(Address leavingNode) {
        DSNeighbours ds = node.getNeighbours();
        for (Address a : ds.getKnownNodes()) {
            if (!a.equals(leavingNode)) {
                try {
                    NodeCommands proxy = getProxy(a);
                    proxy.leave(leavingNode);
                    log.info("Notified {} about leaving node {}", a, leavingNode);
                } catch (RemoteException e) {
                    log.error("Error notifying {} about leaving node {}: {}", a, leavingNode, e.getMessage());
                }
            }
        }
    }

    public void notifyKill(Address killedNode) {
        DSNeighbours ds = node.getNeighbours();
        for (Address a : ds.getKnownNodes()) {
            if (!a.equals(killedNode)) {
                try {
                    NodeCommands proxy = getProxy(a);
                    proxy.killNode(killedNode);
                    log.info("Notified {} that node was killed {}", a, killedNode);
                } catch (RemoteException e) {
                    log.error("Error notifying kill to {} about {}: {}", a, killedNode, e.getMessage());
                }
            }
        }
    }

    public void notifyRevive(Address revivedNode) {
        DSNeighbours ds = node.getNeighbours();
        for (Address a : ds.getKnownNodes()) {
            if (!a.equals(revivedNode)) {
                try {
                    NodeCommands proxy = getProxy(a);
                    proxy.revive(revivedNode);
                    log.info("Notified {} about revived node {}", a, revivedNode);
                } catch (RemoteException e) {
                    log.error("Error notifying {} about revived node {}: {}", a, revivedNode, e.getMessage());
                }
            }
        }
    }

    public void sendMessageTo(String toNick, String fromNick, String message) {
        // Snapshot of current neighbors to avoid changes inside a loop
        Set<Address> snapshot = new HashSet<>(node.getNeighbours().getKnownNodes());

        for (Address a : snapshot) {
            if (a.equals(node.getAddress())) {
                // Process locally
                continue;
            }
            int MAX_ATTEMPTS = 3;
            boolean success = false;
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                try {
                    NodeCommands proxy = getProxy(a);
                    proxy.sendMessage(fromNick, toNick, message);
                    log.info("Sent message from {} to {} via {}", fromNick, toNick, a);
                    success = true;
                    break;
                } catch (RemoteException e) {
                    log.error("Attempt {}/{} to send message to {} failed: {}",
                            attempt, MAX_ATTEMPTS, a, e.getMessage());
                }
            }
            if (!success) {
                log.warn("All attempts to contact {} failed. Assuming it's dead.", a);
                handleDeadNode(a);
            }
        }
    }


    private void handleDeadNode(Address deadAddr) {
        // Removing from list
        node.getNeighbours().removeNode(deadAddr);

        // Notifying others
        Set<Address> snapshot = new HashSet<>(node.getNeighbours().getKnownNodes());
        for (Address other : snapshot) {
            if (!other.equals(deadAddr)) {
                try {
                    NodeCommands proxy = getProxy(other);
                    proxy.killNode(deadAddr);
                    log.info("Notified {} that node was killed {}", other, deadAddr);
                } catch (RemoteException e) {
                    log.error("Error notifying {} about dead {}: {}",
                            other, deadAddr, e.getMessage());
                }
            }
        }

        // If the dead person was a leader - elections
        if (deadAddr.equals(node.getNeighbours().getLeader())) {
            log.info("Dead node was our leader => starting election");
            node.startElection();
        }
    }



}
