package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.DSNeighbours;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;

/**
 * Implements the NodeCommands interface for handling distributed system messages.
 * Provides methods for leader election, joining/leaving the network, and updating leader information.
 */
@Slf4j
@Getter
@Setter
public class MessageReceiver implements NodeCommands {
    private final Node myNode;

    /**
     * Constructs a MessageReceiver for the given node.
     *
     * @param node The node associated with this MessageReceiver.
     */
    public MessageReceiver(Node node) {
        this.myNode = node;
    }

    /**
     * Handles a join request from a new node.
     *
     * @param addr Address of the joining node.
     * @return The current neighbor list of the node.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public DSNeighbours join(Address addr) throws RemoteException {
        log.info("JOIN called by Node {} (id={}), on local Node {} (id={}).",
                addr.getHostname(), addr.getNodeID(),
                myNode.getAddress().getHostname(), myNode.getNodeId());

        // *** CHANGED ***: If the incoming address doesn't have a nodeID set, generate it
        if (addr.getNodeID() == 0) {
            long newID = myNode.generateId(addr.getHostname(), addr.getPort());
            addr.setNodeID(newID);
            log.info("Assigned nodeId={} to newly joined Address: {}", newID, addr);
        }

        // If it's the same node, we might skip
        if (addr.equals(myNode.getAddress())) {
            log.info("I am the first node and the leader => returning my neighbours.");
            return myNode.getNeighbours();
        } else {
            log.info("Node {} is joining the network of local Node {}.",
                    addr.getNodeID(), myNode.getNodeId());

            synchronized (this) {
                myNode.getNeighbours().addNewNode(addr);
                log.info("Node {} added to Node {}'s neighbors from RMI join().",
                        addr.getNodeID(), myNode.getNodeId());
            }

            Address currentLeader;
            synchronized (this) {
                currentLeader = myNode.getNeighbours().getLeaderNode();
                log.info("Current leader is {} for local Node {}.",
                        currentLeader != null ? currentLeader.getNodeID() : "None",
                        myNode.getNodeId());
            }

            if (currentLeader != null) {
                try {
                    NodeCommands remoteNode = myNode.getCommHub().getRMIProxy(addr);
                    remoteNode.notifyAboutNewLeader(currentLeader);
                    log.info("Notified Node {} about the current leader: Node {}.",
                            addr.getNodeID(), currentLeader.getNodeID());
                } catch (RemoteException e) {
                    log.error("Failed to notify Node {} about the current leader: {}",
                            addr.getNodeID(), e.getMessage());
                }
            } else {
                log.info("No leader present in local Node {}. Initiating election.", myNode.getNodeId());
                myNode.startElection();
            }

            return myNode.getNeighbours();
        }
    }

    /**
     * Processes an election message from another node.
     *
     * @param senderId ID of the node that initiated the election.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public void sendElectionMsg(long senderId) throws RemoteException {
        log.info("Received Election message from Node {} for local Node {}.",
                senderId, myNode.getNodeId());
        myNode.getBully().onElectionMsgFromLower(senderId);
    }

    /**
     * Notifies the node about a newly elected leader.
     *
     * @param leader Address of the new leader.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public void notifyAboutNewLeader(Address leader) throws RemoteException {
        Address currentLeader;
        synchronized (this) {
            currentLeader = myNode.getNeighbours().getLeaderNode();
            log.info("Received notifyAboutNewLeader for Node {} from remote side. Our current leader is {}.",
                    leader.getNodeID(),
                    currentLeader != null ? currentLeader.getNodeID() : "None");
        }

        if (currentLeader == null || leader.getNodeID() > currentLeader.getNodeID()) {
            synchronized (this) {
                myNode.getNeighbours().setLeaderNode(leader);
                log.info("Local Node {} acknowledges new leader: Node {}.",
                        myNode.getNodeId(), leader.getNodeID());
            }
            myNode.getBully().onElectedReceived(leader.getNodeID(), leader);

            for (Address neighbor : myNode.getNeighbours().getNeighbours()) {
                try {
                    NodeCommands remoteNode = myNode.getCommHub().getRMIProxy(neighbor);
                    remoteNode.updateLeader(leader);
                    log.info("We told Node {} about new leader Node {}.",
                            neighbor.getNodeID(), leader.getNodeID());
                } catch (RemoteException e) {
                    log.error("Failed to notify Node {} about new leader: {}",
                            neighbor.getNodeID(), e.getMessage());
                }
            }
        } else if (currentLeader.equals(leader)) {
            log.info("Leader {} reconfirmed for local Node {}.",
                    leader.getNodeID(), myNode.getNodeId());
        } else {
            log.info("Ignoring new leader {} because our current leader is Node {} (which is higher ID?).",
                    leader.getNodeID(), currentLeader.getNodeID());
        }
    }

    /**
     * Adds a neighbor to the node's neighbor list.
     *
     * @param addr Address of the neighbor to add.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public void addNeighbor(Address addr) throws RemoteException {
        // If you want some RMI-based method that forcibly inserts a neighbor
        synchronized (this) {
            long newID = myNode.generateId(addr.getHostname(), addr.getPort());
            addr.setNodeID(newID);
            myNode.getNeighbours().addNewNode(addr);
            log.info("Explicitly added Node {} to neighbor list via addNeighbor() call on local Node {}.",
                    addr.getNodeID(), myNode.getNodeId());
        }
    }

    /**
     * Notifies the node about a revived neighbor.
     *
     * @param revivedNode Address of the revived node.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public void notifyAboutRevival(Address revivedNode) throws RemoteException {
        synchronized (this) {
            if (!myNode.getNeighbours().getNeighbours().contains(revivedNode)) {
                // Generate ID if missing
                if (revivedNode.getNodeID() == 0) {
                    long revivedID = myNode.generateId(revivedNode.getHostname(), revivedNode.getPort());
                    revivedNode.setNodeID(revivedID);
                }
                myNode.getNeighbours().addNewNode(revivedNode);
                log.info("Node {} is back online, added to local Node {} neighbors.",
                        revivedNode.getNodeID(), myNode.getNodeId());
            }
        }
    }

    /**
     * Checks the status of the current leader.
     *
     * @param senderId ID of the node requesting the check.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public void checkStatusOfLeader(long senderId) throws RemoteException {
        Address leader;
        synchronized (this) {
            leader = myNode.getNeighbours().getLeaderNode();
        }
        if (leader != null) {
            log.info("Leader Node {} is alive for local Node {}.", leader.getNodeID(), myNode.getNodeId());
        } else {
            log.warn("No leader for local Node {}. Start election now.", myNode.getNodeId());
            myNode.startElection();
        }
    }

    /**
     * Sends a message between nodes.
     *
     * @param senderId   ID of the sending node.
     * @param receiverID ID of the receiving node.
     * @param content    Content of the message.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public void sendMessage(long senderId, int receiverID, String content) throws RemoteException {
        log.info("Message from Node {} to Node {} (local node ID {} sees this). Content: {}",
                senderId, receiverID, myNode.getNodeId(), content);
    }

    /**
     * Returns the address of the current leader.
     *
     * @return Address of the current leader.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public Address getCurrentLeader() throws RemoteException {
        Address leader;
        synchronized (this) {
            leader = myNode.getNeighbours().getLeaderNode();
            log.info("Local Node {} sees current leader as {}.",
                    myNode.getNodeId(), leader != null ? leader.getNodeID() : "None");
        }
        return leader;
    }

    /**
     * Handles notification about a node logging out.
     *
     * @param address Address of the departing node.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public void notifyAboutLogOut(Address address) throws RemoteException {
        // If the departing node is ourselves, skip
        if (myNode.getNodeId() == address.getNodeID()) {
            log.warn("Ignoring notifyAboutLogOut for ourselves (Node {}).", address.getNodeID());
            return;
        }

        synchronized (this) {
            Address currentLeader = myNode.getNeighbours().getLeaderNode();
            log.info("notifyAboutLogOut called on local Node {} for departing Node {}. Current leader is {}.",
                    myNode.getNodeId(), address.getNodeID(),
                    currentLeader != null ? currentLeader.getNodeID() : "None");

            myNode.getNeighbours().removeNode(address);
            log.info("Removed departing Node {} from local Node {} neighbors.",
                    address.getNodeID(), myNode.getNodeId());

            // If departing node was the leader => trigger election
            if (currentLeader != null && currentLeader.equals(address)) {
                log.warn("Leader Node {} left. Local Node {} starts election.",
                        address.getNodeID(), myNode.getNodeId());
                myNode.getNeighbours().setLeaderNode(null);
                myNode.startElection();
            }
        }
    }

    /**
     * Processes an OK response during an election.
     *
     * @param fromId ID of the node sending the OK response.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public void receiveOK(long fromId) throws RemoteException {
        log.info("Local Node {} received OK from Node {} in Bully election.",
                myNode.getNodeId(), fromId);
        myNode.getBully().onOKReceived(fromId);
    }

    /**
     * Updates the node with new leader information.
     *
     * @param leaderAddress Address of the new leader.
     * @throws RemoteException If an error occurs during RMI communication.
     */
    @Override
    public void updateLeader(Address leaderAddress) throws RemoteException {
        Address currentLeader;
        synchronized (this) {
            currentLeader = myNode.getNeighbours().getLeaderNode();
        }
        if (currentLeader == null || leaderAddress.getNodeID() > currentLeader.getNodeID()) {
            synchronized (this) {
                myNode.getNeighbours().setLeaderNode(leaderAddress);
                log.info("Local Node {} acknowledges new leader Node {} via updateLeader().",
                        myNode.getNodeId(), leaderAddress.getNodeID());
            }
            myNode.getBully().onElectedReceived(leaderAddress.getNodeID(), leaderAddress);
        } else {
            log.warn("Local Node {} sees conflicting leader info: we have leader Node {}, but got Node {}.",
                    myNode.getNodeId(),
                    currentLeader.getNodeID(),
                    leaderAddress.getNodeID());
        }
    }
}
