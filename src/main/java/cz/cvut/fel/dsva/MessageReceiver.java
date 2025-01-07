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
    private final Node myNode;

    public MessageReceiver(Node node) {
        this.myNode = node;
    }

    @Override
    public synchronized DSNeighbours join(Address addr) throws RemoteException {
        // Add the joining node to your neighbors
        myNode.getNeighbours().addNewNode(addr);
        log.info("Node {} joined the network.", addr.getNodeID());

        // Retrieve the current leader
        Address currentLeader = myNode.getNeighbours().getLeaderNode();
        log.info("Current leader is Node {}.", currentLeader != null ? currentLeader.getNodeID() : "null");

        if (currentLeader != null) {
            // Notify the joining node about the current leader
            try {
                NodeCommands remoteNode = myNode.getCommHub().getRMIProxy(addr);
                remoteNode.notifyAboutNewLeader(currentLeader);
                log.info("Notified Node {} about current leader: Node {}.", addr.getNodeID(), currentLeader.getNodeID());
            } catch (RemoteException e) {
                log.error("Failed to notify Node {} about current leader: {}", addr.getNodeID(), e.getMessage());
            }
        } else {
            // If no leader exists, initiate an election
            log.info("No leader present. Initiating election.");
            myNode.startElection();
        }

        return myNode.getNeighbours();
    }

    @Override
    public synchronized void sendElectionMsg(long senderId) throws RemoteException {
        log.info("Received Election message from Node {}.", senderId);
        myNode.getBully().onElectionMsgFromLower(senderId);
    }

    @Override
    public synchronized void notifyAboutNewLeader(Address leader) throws RemoteException {
        Address currentLeader = myNode.getNeighbours().getLeaderNode();
        log.info("Received notifyAboutNewLeader for Node {}.", leader.getNodeID());

        if (currentLeader == null) {
            // Accept the new leader if no leader is set
            myNode.getNeighbours().setLeaderNode(leader);
            log.info("New leader notified: Node {}.", leader.getNodeID());

            // Notify BullyAlgorithm about the new leader
            myNode.getBully().onElectedReceived(leader.getNodeID(), leader);

            // Notify all neighbors about the new leader
            for (Address neighbour : myNode.getNeighbours().getNeighbours()) {
                try {
                    myNode.getCommHub().getRMIProxy(neighbour).updateLeader(leader);
                    log.info("Notified Node {} about new leader Node {}.", neighbour.getNodeID(), leader.getNodeID());
                } catch (RemoteException e) {
                    log.error("Failed to notify Node {} about new leader: {}", neighbour.getNodeID(), e.getMessage());
                }
            }
        } else {
            if (currentLeader.equals(leader)) {
                // Leader reconfirmed
                log.info("Leader {} reconfirmed.", leader.getNodeID());
            } else {
                // Ignore new leader notification if current leader has higher or equal ID
                log.info("Ignoring new leader {} because current leader is Node {}.", leader.getNodeID(), currentLeader.getNodeID());
            }
        }
    }

    @Override
    public synchronized void notifyAboutRevival(Address revivedNode) throws RemoteException {
        if (!myNode.getNeighbours().getNeighbours().contains(revivedNode)) {
            myNode.getNeighbours().addNewNode(revivedNode);
            log.info("Node {} is back online.", revivedNode.getNodeID());
        }
        // No election is initiated here to ensure that leader remains until it dies or leaves
    }

    @Override
    public synchronized void checkStatusOfLeader(long senderId) throws RemoteException {
        if (myNode.getNeighbours().isLeaderPresent()) {
            Address leader = myNode.getNeighbours().getLeaderNode();
            log.info("Leader Node {} is alive.", leader.getNodeID());
            // Optionally, send a response back to confirm the leader is alive
        } else {
            log.warn("Leader is missing. Initiating election.");
            myNode.startElection();
        }
    }

    @Override
    public synchronized void sendMessage(long senderId, int receiverID, String content) throws RemoteException {
        log.info("Message from Node {} to Node {}: {}", senderId, receiverID, content);
        // Implement message handling as needed
    }

    @Override
    public synchronized Address getCurrentLeader() throws RemoteException {
        Address leader = myNode.getNeighbours().getLeaderNode();
        log.info("Current leader is Node {}.", leader != null ? leader.getNodeID() : "null");
        return leader;
    }

    @Override
    public synchronized void notifyAboutLogOut(Address address) throws RemoteException {
        log.info("Received notifyAboutLogOut for Node {}.", address.getNodeID());
        Address currentLeader = myNode.getNeighbours().getLeaderNode();

        myNode.getNeighbours().removeNode(address);
        log.info("Node {} left the network.", address.getNodeID());

        // If the logged out node was the leader, initiate election
        if (currentLeader != null && currentLeader.equals(address)) {
            log.warn("Leader Node {} has left. Initiating election.", address.getNodeID());
            myNode.getNeighbours().setLeaderNode(null);
            myNode.notifyAllNeighboursOfLeaderDeath();
            myNode.startElection();
        } else {
            log.info("Node {} was removed, but it was not the leader.", address.getNodeID());
        }
    }

    @Override
    public synchronized void receiveOK(long fromId) throws RemoteException {
        log.info("Received OK message from Node {}.", fromId);
        myNode.getBully().onOKReceived(fromId);
    }

    @Override
    public synchronized void updateLeader(Address leaderAddress) throws RemoteException {
        Address currentLeader = myNode.getNeighbours().getLeaderNode();
        if (currentLeader == null || leaderAddress.getNodeID() > currentLeader.getNodeID()) {
            myNode.getNeighbours().setLeaderNode(leaderAddress);
            log.info("Node {} acknowledges new leader: Node {}.", myNode.getNodeId(), leaderAddress.getNodeID());

            myNode.getBully().onElectedReceived(leaderAddress.getNodeID(), leaderAddress);
        } else {
            log.warn("Node {} received conflicting leader information. Current leader: Node {}, Received leader: Node {}.",
                    myNode.getNodeId(),
                    currentLeader.getNodeID(),
                    leaderAddress.getNodeID());
        }
    }

    @Override
    public synchronized void notifyAboutLeaderDeath(Address deadLeader) throws RemoteException {
        log.warn("Received notification that Leader Node {} has died.", deadLeader.getNodeID());

        // Remove the dead leader from neighbors
        myNode.getNeighbours().removeNode(deadLeader);

        // If the dead leader was the current leader, initiate election
        if (myNode.getNeighbours().getLeaderNode() != null && myNode.getNeighbours().getLeaderNode().equals(deadLeader)) {
            myNode.getNeighbours().setLeaderNode(null);
            log.warn("Leader Node {} has died. Initiating election.", deadLeader.getNodeID());
            myNode.notifyAllNeighboursOfLeaderDeath();
            myNode.startElection();
        }
    }
}
