package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.DSNeighbours;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.util.List;


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
    public synchronized void elected(long id, Address leaderAddr) throws RemoteException {
        log.info("Received Elected message from Node {} with Leader Address {}.", id, leaderAddr);
        myNode.getBully().onElectedReceived(id, leaderAddr);
    }

    @Override
    public synchronized void notifyAboutNewLeader(Address leader) throws RemoteException {
        Address currentLeader = myNode.getNeighbours().getLeaderNode();
        log.info("Received notifyAboutNewLeader for Node {}.", leader.getNodeID());

        if (currentLeader == null) {
            // Accept the new leader
            myNode.getNeighbours().setLeaderNode(leader);
            log.info("New leader notified: " + leader);
            log.info("Set new leader to Node {}.", leader.getNodeID());

            // Notify BullyAlgorithm about new leader was chosen
            myNode.getBully().onElectedReceived(leader.getNodeID(), leader);

            // Notify all neighbors about the new leader
            for (Address neighbour : myNode.getNeighbours().getNeighbours()) {
                try {
                    myNode.getCommHub().getRMIProxy(neighbour).updateLeader(leader);
                    log.info("Notified Node {} about new leader Node {}.", neighbour.getNodeID(), leader.getNodeID());
                } catch (RemoteException e) {
                    log.error("Failed to notify Node {} about new leader: {}", neighbour, e.getMessage());
                }
            }
        } else {
            if (currentLeader.equals(leader)) {
                // Leader reconfirmed
                log.info("Leader " + leader + " reconfirmed.");
                log.info("Leader Node {} reconfirmed.", leader.getNodeID());
            } else {
                // Ignore new leader notification
                log.info("Ignoring new leader " + leader.getNodeID() +
                        ", because we already have leader " + currentLeader.getNodeID());
                log.warn("Ignoring new leader Node {} because current leader is Node {}.", leader.getNodeID(), currentLeader.getNodeID());
            }
        }
    }

    @Override
    public synchronized void notifyAboutRevival(Address revivedNode) throws RemoteException {
        if (!myNode.getNeighbours().getNeighbours().contains(revivedNode)) {
            myNode.getNeighbours().addNewNode(revivedNode);
            log.info("Node {} is back online.", revivedNode.getNodeID());
        }
    }

    @Override
    public synchronized void checkStatusOfLeader(long senderId) throws RemoteException {
        if (myNode.getNeighbours().isLeaderPresent()) {
            Address leader = myNode.getNeighbours().getLeaderNode();
            log.info("Leader Node {} is alive.", leader.getNodeID());
        } else {
            log.warn("Leader is missing. Initiating election.");
            myNode.startElection();
        }
    }

    @Override
    public synchronized void sendMessage(long senderId, int receiverID, String content) throws RemoteException {
        log.info("Message from Node " + senderId + " to Node " + receiverID + ": " + content);
    }

    @Override
    public synchronized void receiveMessage(String message, long receiverId) throws RemoteException {
        log.info("Received message at Node " + receiverId + ": " + message);
    }

    @Override
    public void help() throws RemoteException {
        log.info("Available commands: startElection, checkLeader, sendMessage.");
    }

    @Override
    public void printStatus(Node node) throws RemoteException {
        log.info("Node status: " + node.getStatus());
    }

    @Override
    public synchronized Address getCurrentLeader() throws RemoteException {
        Address leader = myNode.getNeighbours().getLeaderNode();
        log.info("Current leader is Node {}.", leader != null ? leader.getNodeID() : "null");
        return leader;
    }

    @Override
    public synchronized void notifyAboutJoin(Address address) throws RemoteException {
        myNode.getNeighbours().addNewNode(address);
        log.info("Notified about a new node join: Node {}.", address.getNodeID());
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
            myNode.startElection();
        } else {
            log.info("Node {} was removed, but it was not the leader.", address.getNodeID());
        }
    }

    @Override
    public synchronized void election(long id) throws RemoteException {
        log.info("Election command received from Node {}.", id);
        myNode.getBully().startElection();
    }

    @Override
    public synchronized void repairTopologyAfterJoin(Address address) throws RemoteException {
        log.info("Repairing topology after join of {}", address);
        if (!myNode.getNeighbours().getNeighbours().contains(address)) {
            myNode.getNeighbours().addNewNode(address);
            log.info("Node {} added to neighbors during topology repair.", address.getNodeID());
        }
    }

    @Override
    public synchronized void repairTopologyAfterLogOut(int nodeID) throws RemoteException {
        log.info("Repairing topology after Node {} left.", nodeID);
        myNode.getNeighbours().removeNodeById(nodeID);
    }

    @Override
    public synchronized void repairTopologyWithNewLeader(List<Address> addresses, Address address) throws RemoteException {
        log.info("Repairing topology with new leader: {}", address);
        myNode.getNeighbours().setLeaderNode(address);
        log.info("Set new leader to Node {} during topology repair.", address.getNodeID());

        for (Address addr : addresses) {
            if (!myNode.getNeighbours().getNeighbours().contains(addr)) {
                myNode.getNeighbours().addNewNode(addr);
                log.info("Node {} added to neighbors during topology repair.", addr.getNodeID());
            }
        }
    }

    @Override
    public synchronized void killNode() throws RemoteException {
        log.info("Kill node command received.");
        myNode.kill();
    }

    @Override
    public synchronized void reviveNode() throws RemoteException {
        log.info("Revive node command received.");
        myNode.revive();
    }

    @Override
    public synchronized void setDelay(long delay) throws RemoteException {
        log.info("Set delay command received. Delay: {}", delay);
        myNode.setDelay(delay);
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
    public synchronized void resetTopology() throws RemoteException {
        log.info("Reset topology command received.");
        myNode.resetTopology();
        log.info("Topology has been reset by request to Node {}.", myNode.getNodeId());
    }
}
