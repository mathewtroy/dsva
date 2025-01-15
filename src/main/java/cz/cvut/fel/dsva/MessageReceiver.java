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
        System.out.println("Node joined: " + newNodeAddr);
        return node.getNeighbours();
    }

    @Override
    public void startElection(long candidateId) throws RemoteException {
        System.out.println("Received startElection from nodeId: " + candidateId);
        if (node.getNodeId() > candidateId) {
            node.getCommunicationHub().sendRespondOk(candidateId);
            node.internalStartElection();
        }
        // If nodeId <= candidateId, do nothing
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
        // Implement chat message handling as needed
    }

    @Override
    public void leave(Address leavingNode) throws RemoteException {
        System.out.println("Received leave notification from " + leavingNode);
        node.getNeighbours().removeNode(leavingNode);
        if (node.getNeighbours().getLeader().compareTo(leavingNode) == 0) {
            System.out.println("Leader has left. Starting election.");
            node.startElection();
        }
    }

    @Override
    public void revive(Address revivedNode) throws RemoteException {
        System.out.println("Received revive notification for " + revivedNode);
        node.getNeighbours().addNode(revivedNode);
    }

    @Override
    public void hello() throws RemoteException {
        System.out.println("Received hello from " + node.getAddress());
    }
}
