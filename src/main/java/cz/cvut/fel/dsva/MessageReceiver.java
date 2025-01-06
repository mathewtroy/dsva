package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.DSNeighbours;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.util.List;

import static cz.cvut.fel.dsva.Color.GREEN;
import static cz.cvut.fel.dsva.Color.RED;

@Slf4j
@Getter
@Setter
public class MessageReceiver implements NodeCommands {
    private final Node myNode;

    public MessageReceiver(Node node) {
        this.myNode = node;
    }

    @Override
    public DSNeighbours join(Address addr) throws RemoteException {
        // Add the joining node to your neighbors
        myNode.getNeighbours().addNewNode(addr);
        System.out.println("Node " + addr.getNodeID() + " joined the network.");
        log.info("Node {} added to neighbors.", addr.getNodeID());

        // Retrieve the current leader
        Address currentLeader = myNode.getNeighbours().getLeaderNode();
        log.info("Current leader is Node {}.", currentLeader != null ? currentLeader.getNodeID() : "null");

        if (currentLeader != null) {
            // Notify the joining node about the current leader
            try {
                NodeCommands remoteNode = myNode.getCommHub().getRMIProxy(addr);
                remoteNode.notifyAboutNewLeader(currentLeader);
                log.info(GREEN + "Notified Node {} about current leader: Node {}.", addr.getNodeID(), currentLeader.getNodeID());
            } catch (RemoteException e) {
                log.error(RED + "Failed to notify Node {} about current leader: {}", addr.getNodeID(), e.getMessage());
            }
        } else {
            // If no leader exists, initiate an election
            System.out.println("No leader present. Starting election.");
            log.info("No leader present. Initiating election.");
            myNode.startElection();
        }

        return myNode.getNeighbours();
    }

    @Override
    public void sendElectionMsg(long senderId) throws RemoteException {
        myNode.getBully().onElectionMsgFromLower(senderId);
    }

    @Override
    public void Elected(long id, Address leaderAddr) throws RemoteException {
        myNode.getBully().onElectedReceived(id, leaderAddr);
    }

    @Override
    public void notifyAboutNewLeader(Address leader) throws RemoteException {
        Address currentLeader = myNode.getNeighbours().getLeaderNode();
        log.info("Received notifyAboutNewLeader for Node {}", leader.getNodeID());

        if (currentLeader == null) {
            // Accept the new leader
            myNode.getNeighbours().setLeaderNode(leader);
            System.out.println("New leader notified: " + leader);
            log.info("Set new leader to Node {}.", leader.getNodeID());

            // Notify all neighbors about the new leader
            for (Address neighbour : myNode.getNeighbours().getNeighbours()) {
                try {
                    myNode.getCommHub().getRMIProxy(neighbour).updateLeader(leader);
                    log.info(GREEN + "Notified Node {} about new leader Node {}.", neighbour.getNodeID(), leader.getNodeID());
                } catch (RemoteException e) {
                    log.error("Failed to notify Node {} about new leader: {}", neighbour.getNodeID(), e.getMessage());
                }
            }
        } else {
            if (currentLeader.equals(leader)) {
                // Leader reconfirmed
                System.out.println("Leader " + leader + " reconfirmed.");
                log.info("Leader Node {} reconfirmed.", leader.getNodeID());
            } else {
                // Ignore new leader notification
                System.out.println("Ignoring new leader " + leader.getNodeID() +
                        ", because we already have leader " + currentLeader.getNodeID());
                log.warn("Ignoring new leader Node {} because current leader is Node {}.", leader.getNodeID(), currentLeader.getNodeID());
            }
        }
    }

    @Override
    public void notifyAboutRevival(Address revivedNode) throws RemoteException {
        if (!myNode.getNeighbours().getNeighbours().contains(revivedNode)) {
            myNode.getNeighbours().addNewNode(revivedNode);
            System.out.println("Node " + revivedNode.getNodeID() + " is back online.");
            log.info("Node {} added back to neighbors.", revivedNode.getNodeID());
        }
    }

    @Override
    public void checkStatusOfLeader(long senderId) throws RemoteException {
        if (myNode.getNeighbours().isLeaderPresent()) {
            System.out.println("Leader is alive: " + myNode.getNeighbours().getLeaderNode());
            log.info("Leader Node {} is alive.", myNode.getNeighbours().getLeaderNode().getNodeID());
        } else {
            System.out.println("Leader is missing, starting election...");
            log.warn("Leader is missing. Initiating election.");
            myNode.startElection();
        }
    }

    @Override
    public void sendMessage(long senderId, int receiverID, String content) throws RemoteException {
        System.out.println("Message from Node " + senderId + " to Node " + receiverID + ": " + content);
    }

    @Override
    public void receiveMessage(String message, long receiverId) throws RemoteException {
        System.out.println("Received message at Node " + receiverId + ": " + message);
    }

    @Override
    public void help() throws RemoteException {
        System.out.println("Available commands: startElection, checkLeader, sendMessage.");
    }

    @Override
    public void printStatus(Node node) throws RemoteException {
        System.out.println("Node status: " + node.getStatus());
    }

    @Override
    public Address getCurrentLeader() throws RemoteException {
        return myNode.getNeighbours().getLeaderNode();
    }

    @Override
    public void notifyAboutJoin(Address address) throws RemoteException {
        myNode.getNeighbours().addNewNode(address);
        System.out.println("Notified about a new node join: " + address);
        log.info("Node {} added to neighbors.", address.getNodeID());
    }

    @Override
    public void notifyAboutLogOut(Address address) throws RemoteException {
        log.info("Received notifyAboutLogOut for Node {}", address.getNodeID());
        Address currentLeader = myNode.getNeighbours().getLeaderNode();

        myNode.getNeighbours().removeNode(address);
        System.out.println("Node " + address.getNodeID() + " left the network.");
        log.info("Node {} removed from neighbors.", address.getNodeID());

        // If the logged out node was the leader, initiate election
        if (currentLeader != null && currentLeader.equals(address)) {
            System.out.println("Leader has left. Starting election...");
            log.warn("Leader Node {} has left. Initiating election.", address.getNodeID());
            myNode.getNeighbours().setLeaderNode(null);
            myNode.startElection();
        } else {
            log.info("Node {} was removed, but it was not the leader.", address.getNodeID());
        }
    }


    @Override
    public void Election(long id) throws RemoteException {
        myNode.getBully().startElection();
    }

    @Override
    public void repairTopologyAfterJoin(Address address) throws RemoteException {
        System.out.println("Repairing topology after join of " + address);
        if (!myNode.getNeighbours().getNeighbours().contains(address)) {
            myNode.getNeighbours().addNewNode(address);
            log.info("Node {} added to neighbors during topology repair.", address.getNodeID());
        }
    }

    @Override
    public void repairTopologyAfterLogOut(int nodeID) throws RemoteException {
        myNode.getNeighbours().removeNodeById(nodeID);
        System.out.println("Topology repaired after Node " + nodeID + " left.");
        log.info("Node {} removed from neighbors during topology repair.", nodeID);
    }

    @Override
    public void repairTopologyWithNewLeader(List<Address> addresses, Address address) throws RemoteException {
        System.out.println("Repairing topology with new leader: " + address);
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
    public void killNode() throws RemoteException {
        myNode.kill();
    }

    @Override
    public void reviveNode() throws RemoteException {
        myNode.revive();
    }

    @Override
    public void setDelay(long delay) throws RemoteException {
        myNode.setDelay(delay);
    }

    @Override
    public void receiveOK(long fromId) throws RemoteException {
        myNode.getBully().onOKReceived(fromId);
    }

    @Override
    public void updateLeader(Address leaderAddress) throws RemoteException {
        Address currentLeader = myNode.getNeighbours().getLeaderNode();
        if (currentLeader == null || currentLeader.equals(leaderAddress)) {
            myNode.getNeighbours().setLeaderNode(leaderAddress);
            log.info(GREEN + "Node {} acknowledges new leader: Node {}.", myNode.getNodeId(), leaderAddress.getNodeID());
            System.out.println("Node " + myNode.getNodeId() + " acknowledges new leader: " + leaderAddress);
        } else {
            log.warn("Node {} received conflicting leader information. Current leader: Node {}, Received leader: Node {}.",
                    myNode.getNodeId(),
                    currentLeader.getNodeID(),
                    leaderAddress.getNodeID());
            System.out.println("Node " + myNode.getNodeId() + " received conflicting leader information. Current leader: Node " +
                    currentLeader.getNodeID() + ", Received leader: Node " + leaderAddress.getNodeID());
        }
    }

    @Override
    public void resetTopology() throws RemoteException {
        myNode.resetTopology();
        log.info(GREEN + "Topology has been reset by request to Node {}.", myNode.getNodeId());
        System.out.println("Topology reset on Node " + myNode.getNodeId());
    }
}
