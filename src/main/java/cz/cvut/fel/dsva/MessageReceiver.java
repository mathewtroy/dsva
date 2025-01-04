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
        myNode.getNeighbours().addNewNode(addr);
        System.out.println("Node " + addr + " joined the network.");
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
        myNode.getNeighbours().setLeaderNode(leader);
        System.out.println("New leader notified: " + leader);

        for (Address neighbour : myNode.getNeighbours().getNeighbours()) {
            System.out.println("Notifying neighbour: " + neighbour);
            myNode.getCommHub().getRMIProxy(neighbour).updateLeader(leader);
        }
    }


    @Override
    public void checkStatusOfLeader(long senderId) throws RemoteException {
        if (myNode.getNeighbours().isLeaderPresent()) {
            System.out.println("Leader is alive: " + myNode.getNeighbours().getLeaderNode());
        } else {
            System.out.println("Leader is missing, starting election...");
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
    }

    @Override
    public void notifyAboutLogOut(Address address) throws RemoteException {
        myNode.getNeighbours().removeNode(address);
        System.out.println("Node " + address + " left the network.");
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
        }
    }

    @Override
    public void repairTopologyAfterLogOut(int nodeID) throws RemoteException {
        myNode.getNeighbours().removeNodeById(nodeID);
        System.out.println("Topology repaired after Node " + nodeID + " left.");
    }

    @Override
    public void repairTopologyWithNewLeader(List<Address> addresses, Address address) throws RemoteException {
        System.out.println("Repairing topology with new leader: " + address);
        myNode.getNeighbours().setLeaderNode(address);

        for (Address addr : addresses) {
            if (!myNode.getNeighbours().getNeighbours().contains(addr)) {
                myNode.getNeighbours().addNewNode(addr);
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
        myNode.getNeighbours().setLeaderNode(leaderAddress);
        log.info(GREEN + "Node {} received new leader information: {}", myNode.getNodeId(), leaderAddress);
        System.out.println("Node " + myNode.getNodeId() + " acknowledges new leader: " + leaderAddress);
    }

    @Override
    public void resetTopology() throws RemoteException {
        myNode.resetTopology();
        log.info(GREEN + "Topology has been reset by request to Node {}.", myNode.getNodeId());
        System.out.println("Topology reset on Node " + myNode.getNodeId());
    }



}
