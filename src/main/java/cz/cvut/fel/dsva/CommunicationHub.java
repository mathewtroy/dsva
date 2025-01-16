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

/**
 * Manages all communication between nodes, including sending and receiving messages,
 * handling elections, and notifying nodes of changes in the network.
 *
 * <p>This class abstracts the complexity of RMI communication, providing methods
 * to interact with other nodes seamlessly.
 *
 * <p>Key functionalities include:
 * <ul>
 *     <li>Sending election messages to higher-ID nodes.</li>
 *     <li>Responding to election requests.</li>
 *     <li>Broadcasting leader announcements.</li>
 *     <li>Notifying nodes when a node leaves or is revived.</li>
 *     <li>Handling message passing between nodes.</li>
 * </ul>
 *
 * @author @author Kross Aleksandr
 */
@Slf4j
@Getter
@Setter
public class CommunicationHub {
    private final Node node;

    /**
     * Constructs a CommunicationHub associated with the given node.
     *
     * @param node The parent Node instance.
     */
    public CommunicationHub(Node node) {
        this.node = node;
    }

    /**
     * Retrieves the RMI proxy for a given node address.
     *
     * @param addr The address of the node to connect to.
     * @return The {@link NodeCommands} proxy for the specified node.
     * @throws RemoteException If an RMI error occurs or the node is not bound.
     */
    public NodeCommands getProxy(Address addr) throws RemoteException {
        // If attempting to connect to self
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

    /**
     * Broadcasts the addition of a new node to all known neighbors.
     *
     * @param newAddr The address of the new node to broadcast.
     */
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

    /**
     * Sends an election request to all nodes with a higher ID.
     */
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

    /**
     * Sends an OK response to the candidate initiating an election.
     *
     * @param candidateId The ID of the node that initiated the election.
     */
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

    /**
     * Broadcasts the announcement of a new leader to all known neighbors.
     */
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

    /**
     * Notifies all neighbors about this node leaving the network.
     *
     * @param leavingNode The address of the node that is leaving.
     */
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

    /**
     * Notifies all neighbors about this node being revived.
     *
     * @param revivedNode The address of the node that has been revived.
     */
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

    /**
     * Sends a message from one node to another identified by their nicknames.
     *
     * <p>Note: This method currently sends the message to all known nodes.
     * To target a specific node, implement a mapping from nickname to {@link Address}.
     *
     * @param toNick   The nickname of the recipient node.
     * @param fromNick The nickname of the sender node.
     * @param message  The message content to send.
     */
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

    /**
     * Handles the scenario when a node is considered dead/unresponsive.
     *
     * <p>This method removes the dead node from the known neighbors, notifies other nodes
     * about the dead node, and initiates a leader election if necessary.
     *
     * @param deadAddr The address of the dead node.
     */
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

        // If the dead node was the leader, initiate an election
        if (deadAddr.equals(node.getNeighbours().getLeader())) {
            log.info("Dead node was our leader => starting election");
            node.startElection();
        }
    }
}