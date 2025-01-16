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
        node.printStatus();
        node.getCommunicationHub().broadcastNewNode(newNodeAddr);
        System.out.println("Node joined: " + newNodeAddr);
        return node.getNeighbours();
    }

    @Override
    public void broadcastNewNode(Address newAddr) throws RemoteException {
        node.getNeighbours().addNode(newAddr);
        node.printStatus();
    }

    @Override
    public void startElection(long candidateId) throws RemoteException {
        System.out.println("Received startElection from nodeId: " + candidateId);
        if (node.getNodeId() > candidateId) {
            node.getCommunicationHub().sendRespondOk(candidateId);
            node.internalStartElection();
        }
    }

    @Override
    public void respondOk(long fromNodeId) throws RemoteException {
        System.out.println("Received respondOk from nodeId: " + fromNodeId);
        node.setElectionInProgress(false);
    }

    @Override
    public void announceLeader(long leaderId, Address leaderAddress) throws RemoteException {
        System.out.println("Received announceLeader: " + leaderAddress + " with nodeId: " + leaderId);
        node.getNeighbours().setLeader(leaderAddress);
        node.setElectionInProgress(false);
        node.printStatus();
    }

    @Override
    public void sendMessage(String fromNick, String toNick, String message) throws RemoteException {
        System.out.println("Received message from " + fromNick + " to " + toNick + ": " + message);
    }

    @Override
    public void leave(Address leavingNode) throws RemoteException {
        System.out.println("Received leave notification from " + leavingNode);
        node.getNeighbours().removeNode(leavingNode);
        if (node.getNeighbours().getLeader() != null &&
                node.getNeighbours().getLeader().equals(leavingNode)) {
            System.out.println("Leader has left. Starting election.");
            node.startElection();
        }
        node.printStatus();
    }

    @Override
    public void killNode(Address killedNode) throws RemoteException {
        System.out.println("Received kill notification for " + killedNode);
        if (killedNode.equals(node.getAddress())) {
            node.setKilled(true);
            node.setActive(false);
            System.out.println("Node " + killedNode + " is now killed/unresponsive.");
        } else {
            // Remove from known nodes (optional or keep it)
            node.getNeighbours().removeNode(killedNode);
        }
        node.printStatus();
    }

    @Override
    public void revive(Address revivedNode) throws RemoteException {
        System.out.println("Received revive notification for " + revivedNode);
        node.getNeighbours().addNode(revivedNode);
        if (revivedNode.equals(node.getAddress())) {
            node.setKilled(false);
            node.setActive(true);
            System.out.println("Node " + revivedNode + " is revived.");
        }
        node.printStatus();
    }

    @Override
    public void hello() throws RemoteException {
        System.out.println("Received hello from " + node.getAddress());
    }
}
