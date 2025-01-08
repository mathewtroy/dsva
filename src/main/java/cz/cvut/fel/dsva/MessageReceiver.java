package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
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
    public void join(Address addr) throws RemoteException {

        synchronized (this) {
            myNode.getNeighbours().addNewNode(addr);
            log.info("Node {} joined the network.", addr.getNodeID());
        }

        try {
            NodeCommands remoteNode = myNode.getCommHub().getRMIProxy(addr);
            remoteNode.addNeighbor(myNode.getAddress());
            log.info("Added Node {} to Node {}'s neighbor list.", myNode.getNodeId(), addr.getNodeID());
        } catch (RemoteException e) {
            log.error("Failed to add Node {} to Node {}'s neighbor list: {}", myNode.getNodeId(), addr.getNodeID(), e.getMessage());
        }

        Address currentLeader;
        synchronized (this) {
            currentLeader = myNode.getNeighbours().getLeaderNode();
            log.info("The current leader is Node {}.", currentLeader != null ? currentLeader.getNodeID() : "null");
        }

        if (currentLeader != null) {
            try {
                NodeCommands remoteNode = myNode.getCommHub().getRMIProxy(addr);
                remoteNode.notifyAboutNewLeader(currentLeader);
                log.info("Notified Node {} about current leader: Node {}.", addr.getNodeID(), currentLeader.getNodeID());
            } catch (RemoteException e) {
                log.error("Failed to notify Node {} about current leader: {}", addr.getNodeID(), e.getMessage());
            }
        } else {
            log.info("No leader present. Initiating election.");
            myNode.startElection();
        }

    }

    @Override
    public void sendElectionMsg(long senderId) throws RemoteException {
        log.info("Received Election message from Node {}.", senderId);
        myNode.getBully().onElectionMsgFromLower(senderId);
    }

    @Override
    public void notifyAboutNewLeader(Address leader) throws RemoteException {
        Address currentLeader;
        synchronized (this) {
            currentLeader = myNode.getNeighbours().getLeaderNode();
            log.info("Received notifyAboutNewLeader for Node {}.", leader.getNodeID());
        }

        // Accept the new leader if:
        // 1. There is no current leader
        // 2. The new leader has a higher node ID than the current leader
        if (currentLeader == null || leader.getNodeID() > currentLeader.getNodeID()) {
            synchronized (this) {
                myNode.getNeighbours().setLeaderNode(leader);
                log.info("New leader notified: Node {}.", leader.getNodeID());
            }

            myNode.getBully().onElectedReceived(leader.getNodeID(), leader);

            for (Address neighbour : myNode.getNeighbours().getNeighbours()) {
                try {
                    NodeCommands remoteNode = myNode.getCommHub().getRMIProxy(neighbour);
                    remoteNode.updateLeader(leader);
                    log.info("Notified Node {} about new leader Node {}.", neighbour.getNodeID(), leader.getNodeID());
                } catch (RemoteException e) {
                    log.error("Failed to notify Node {} about new leader: {}", neighbour.getNodeID(), e.getMessage());
                }
            }
        } else if (currentLeader.equals(leader)) {
            log.info("Leader {} reconfirmed.", leader.getNodeID());
        } else {
            log.info("Ignoring new leader {} because current leader is Node {}.", leader.getNodeID(), currentLeader.getNodeID());
        }
    }

    @Override
    public void addNeighbor(Address addr) throws RemoteException {
        synchronized (this) {
            myNode.getNeighbours().addNewNode(addr);
            log.info("Added Node {} to neighbor list.", addr.getNodeID());
        }
    }

    @Override
    public void notifyAboutRevival(Address revivedNode) throws RemoteException {
        synchronized (this) {
            if (!myNode.getNeighbours().getNeighbours().contains(revivedNode)) {
                myNode.getNeighbours().addNewNode(revivedNode);
                log.info("Node {} is back online.", revivedNode.getNodeID());
            }
        }
    }

    @Override
    public void checkStatusOfLeader(long senderId) throws RemoteException {
        Address leader;
        synchronized (this) {
            leader = myNode.getNeighbours().getLeaderNode();
        }

        if (leader != null) {
            log.info("Leader Node {} is alive.", leader.getNodeID());

        } else {
            log.warn("Leader is missing. Initiating election.");
            myNode.startElection();
        }
    }

    @Override
    public void sendMessage(long senderId, int receiverID, String content) throws RemoteException {
        log.info("Message from Node {} to Node {}: {}", senderId, receiverID, content);
    }

    @Override
    public Address getCurrentLeader() throws RemoteException {
        Address leader;
        synchronized (this) {
            leader = myNode.getNeighbours().getLeaderNode();
            log.info("Current leader is Node {}.", leader != null ? leader.getNodeID() : "null");
        }
        return leader;
    }

    @Override
    public void notifyAboutLogOut(Address address) throws RemoteException {
        Address currentLeader;
        synchronized (this) {
            currentLeader = myNode.getNeighbours().getLeaderNode();
            log.info("Received notifyAboutLogOut for Node {}.", address.getNodeID());
            myNode.getNeighbours().removeNode(address);
            log.info("Node {} left the network.", address.getNodeID());
        }

        if (currentLeader != null && currentLeader.equals(address)) {
            log.warn("Leader Node {} has left. Initiating election.", address.getNodeID());
            myNode.getNeighbours().setLeaderNode(null);
            myNode.startElection();
        } else {
            log.info("Node {} was removed, but it was not the leader.", address.getNodeID());
        }
    }

    @Override
    public void receiveOK(long fromId) throws RemoteException {
        log.info("Received OK message from Node {}.", fromId);
        myNode.getBully().onOKReceived(fromId);
    }

    @Override
    public void updateLeader(Address leaderAddress) throws RemoteException {
        Address currentLeader;
        synchronized (this) {
            currentLeader = myNode.getNeighbours().getLeaderNode();
        }

        if (currentLeader == null || leaderAddress.getNodeID() > currentLeader.getNodeID()) {
            synchronized (this) {
                myNode.getNeighbours().setLeaderNode(leaderAddress);
                log.info("Node {} acknowledges new leader: Node {}.", myNode.getNodeId(), leaderAddress.getNodeID());
            }

            myNode.getBully().onElectedReceived(leaderAddress.getNodeID(), leaderAddress);
        } else {
            log.warn("Node {} received conflicting leader information. Current leader: Node {}, Received leader: Node {}.",
                    myNode.getNodeId(),
                    currentLeader.getNodeID(),
                    leaderAddress.getNodeID());
        }
    }
}
