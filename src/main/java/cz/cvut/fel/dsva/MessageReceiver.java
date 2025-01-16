package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.DSNeighbours;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;

@Slf4j
@Getter
@Setter
public class MessageReceiver implements NodeCommands {
    private final Node node;

    public MessageReceiver(Node node) {
        this.node = node;
    }

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

    @Override
    public void broadcastNewNode(Address newAddr) throws RemoteException {
        node.getNeighbours().addNode(newAddr);
        log.info("Received broadcast about new node {}. Added to knownNodes.", newAddr);
        node.printStatus();
    }

    @Override
    public void startElection(long candidateId) throws RemoteException {
        log.info("Received startElection from nodeId: {}", candidateId);
        if (node.getNodeId() > candidateId) {
            node.getCommunicationHub().sendRespondOk(candidateId);
            node.internalStartElection();
        }
    }

    @Override
    public void respondOk(long fromNodeId) throws RemoteException {
        log.info("Received respondOk from nodeId: {}", fromNodeId);
        node.setElectionInProgress(false);
    }

    @Override
    public void announceLeader(long leaderId, Address leaderAddress) throws RemoteException {
        log.info("Received announceLeader from leaderId: {}, address: {}", leaderId, leaderAddress);
        node.getNeighbours().setLeader(leaderAddress);
        node.setElectionInProgress(false);
        node.printStatus();
    }

    @Override
    public void sendMessage(String fromNick, String toNick, String message) throws RemoteException {
        log.info("Received message from {} to {}: {}", fromNick, toNick, message);
    }

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

    @Override
    public void hello() throws RemoteException {
        log.info("Received hello from {}", node.getAddress());
    }
}
