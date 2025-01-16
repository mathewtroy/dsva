package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.DSNeighbours;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;

/**
 * Implements the {@link NodeCommands} interface to handle remote method invocations from other nodes.
 * This class acts as the RMI server-side handler for incoming commands and interactions.
 *
 * <p>Key functionalities include:
 * <ul>
 *     <li>Handling node joins and broadcasting new nodes.</li>
 *     <li>Managing election processes and leader announcements.</li>
 *     <li>Handling message passing between nodes.</li>
 *     <li>Managing node departures and revivals.</li>
 * </ul>
 *
 * @see NodeCommands
 */
@Slf4j
@Getter
@Setter
public class MessageReceiver implements NodeCommands {
    private final Node node;

    /**
     * Constructs a MessageReceiver associated with the given node.
     *
     * @param node The parent Node instance.
     */
    public MessageReceiver(Node node) {
        this.node = node;
    }

    /**
     * Handles the join request from a new node.
     *
     * <p>If the new node is not the current node, it adds the new node to its neighbors
     * and broadcasts the new node to other known nodes to maintain a full mesh topology.
     *
     * @param newNodeAddr The address of the new node attempting to join.
     * @return The updated {@link DSNeighbours} containing known nodes.
     * @throws RemoteException If an RMI error occurs.
     */
    @Override
    public DSNeighbours join(Address newNodeAddr) throws RemoteException {
        if (newNodeAddr.compareTo(node.getAddress()) == 0) {
            return node.getNeighbours();
        }
        node.getNeighbours().addNode(newNodeAddr);
        log.info("Node {} joined. Broadcasting to others...", newNodeAddr);
        node.getCommunicationHub().broadcastNewNode(newNodeAddr);
        node.printStatus();
        return node.getNeighbours();
    }

    /**
     * Broadcasts the addition of a new node to all known neighbors.
     *
     * @param newAddr The address of the new node to broadcast.
     * @throws RemoteException If an RMI error occurs.
     */
    @Override
    public void broadcastNewNode(Address newAddr) throws RemoteException {
        node.getNeighbours().addNode(newAddr);
        log.info("Received broadcast about new node {}. Added to knownNodes.", newAddr);
        node.printStatus();
    }

    /**
     * Initiates an election when a higher-ID node requests it.
     *
     * <p>If the current node has a higher ID, it responds with an OK and starts its own election.
     *
     * @param candidateId The ID of the node that initiated the election.
     * @throws RemoteException If an RMI error occurs.
     */
    @Override
    public void startElection(long candidateId) throws RemoteException {
        log.info("Received startElection from nodeId: {}", candidateId);
        if (node.getNodeId() > candidateId) {
            node.getCommunicationHub().sendRespondOk(candidateId);
            node.internalStartElection();
        }
    }

    /**
     * Receives an OK response from a higher-ID node during an election.
     *
     * @param fromNodeId The ID of the node that responded.
     * @throws RemoteException If an RMI error occurs.
     */
    @Override
    public void respondOk(long fromNodeId) throws RemoteException {
        log.info("Received respondOk from nodeId: {}", fromNodeId);
        node.setElectionInProgress(false);
    }

    /**
     * Announces the new leader to this node.
     *
     * @param leaderId      The ID of the new leader.
     * @param leaderAddress The address of the new leader.
     * @throws RemoteException If an RMI error occurs.
     */
    @Override
    public void announceLeader(long leaderId, Address leaderAddress) throws RemoteException {
        log.info("Received announceLeader from leaderId: {}, address: {}", leaderId, leaderAddress);
        node.getNeighbours().setLeader(leaderAddress);
        node.setElectionInProgress(false);
        node.printStatus();
    }

    /**
     * Receives a message intended for this node.
     *
     * @param fromNick The nickname of the sender.
     * @param toNick   The nickname of the intended recipient.
     * @param message  The message content.
     * @throws RemoteException If an RMI error occurs.
     */
    @Override
    public void sendMessage(String fromNick, String toNick, String message) throws RemoteException {
        log.info("Received message from {} to {}: {}", fromNick, toNick, message);
    }

    /**
     * Handles notifications when a node leaves the network.
     *
     * <p>Removes the leaving node from the neighbors list and initiates an election
     * if the leaving node was the leader.
     *
     * @param leavingNode The address of the node that is leaving.
     * @throws RemoteException If an RMI error occurs.
     */
    @Override
    public void leave(Address leavingNode) throws RemoteException {
        log.info("Received leave notification from {}", leavingNode);
        node.getNeighbours().removeNode(leavingNode);
        if (node.getNeighbours().getLeader() != null &&
                node.getNeighbours().getLeader().equals(leavingNode)) {
            log.info("Leader has left. Starting election...");
            node.startElection();
        }
        node.printStatus();
    }

    /**
     * Handles notifications when a node is killed abruptly.
     *
     * <p>If the killed node is this node, it updates its state to reflect that it is no longer active.
     * Otherwise, it removes the killed node from the neighbors list and initiates an election if necessary.
     *
     * @param killedNode The address of the node that has been killed.
     * @throws RemoteException If an RMI error occurs.
     */
    @Override
    public void killNode(Address killedNode) throws RemoteException {
        log.info("Received kill notification for {}", killedNode);
        if (killedNode.equals(node.getAddress())) {
            node.setKilled(true);
            node.setActive(false);
            node.getNeighbours().getKnownNodes().clear();
            node.getNeighbours().setLeader(null);
            node.stopRMI();
            log.warn("Node {} is now killed/unresponsive. Cleared neighbors and leader.", killedNode);
        } else {
            node.getNeighbours().removeNode(killedNode);
            if (node.getNeighbours().getLeader() != null &&
                    node.getNeighbours().getLeader().equals(killedNode)) {
                log.info("Leader was killed. Starting election...");
                node.startElection();
            }
        }
        node.printStatus();
    }

    /**
     * Handles notifications when a node is revived.
     *
     * <p>If the revived node is this node, it updates its state to become active again.
     * Otherwise, it adds the revived node to the neighbors list.
     *
     * @param revivedNode The address of the node that has been revived.
     * @throws RemoteException If an RMI error occurs.
     */
    @Override
    public void revive(Address revivedNode) throws RemoteException {
        log.info("Received revive notification for {}", revivedNode);
        if (revivedNode.equals(node.getAddress())) {
            node.setKilled(false);
            node.setActive(true);
            node.getNeighbours().getKnownNodes().clear();
            node.getNeighbours().setLeader(null);
            node.startRMI();
            log.warn("Node {} is revived. Cleared neighbors and leader.", revivedNode);
        } else {
            node.getNeighbours().addNode(revivedNode);
        }
        node.printStatus();
    }

    /**
     * Receives a simple hello message from another node.
     *
     * @throws RemoteException If an RMI error occurs.
     */
    @Override
    public void hello() throws RemoteException {
        log.info("Received hello from {}", node.getAddress());
    }
}