package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Manages communication between nodes in a distributed system.
 * Provides methods for sending messages and interacting with nodes via RMI.
 */
@Slf4j
@Getter
@Setter
public class CommunicationHub {
    private final Node myNode;         // Reference to the current node
    private long messageDelay = 0;    // Delay in milliseconds for simulating message transmission delay

    /**
     * Constructs a CommunicationHub for the given node.
     *
     * @param node The current node using this communication hub.
     */
    public CommunicationHub(Node node) {
        this.myNode = node;
    }

    /**
     * Retrieves an RMI proxy for a given node address.
     *
     * @param address The address of the node to connect to.
     * @return A proxy object for the remote node implementing NodeCommands.
     * @throws RemoteException If there is an issue with RMI communication.
     */
    public NodeCommands getRMIProxy(Address address) throws RemoteException {
        try {
            if (messageDelay > 0) {
                Thread.sleep(messageDelay); // Simulate network delay
            }
            Registry registry = LocateRegistry.getRegistry(address.getHostname(), address.getPort());
            return (NodeCommands) registry.lookup(Node.COMM_INTERFACE_NAME);
        } catch (NotBoundException | InterruptedException e) {
            throw new RemoteException("Failed to get RMI proxy for: " + address, e);
        }
    }

    /**
     * Sends an election message to a specified node.
     *
     * @param address  The address of the recipient node.
     * @param senderId The ID of the node initiating the election.
     */
    public void sendElectionMessage(Address address, long senderId) {
        try {
            if (messageDelay > 0) {
                Thread.sleep(messageDelay); // Simulate network delay
            }
            NodeCommands remoteNode = getRMIProxy(address);
            remoteNode.sendElectionMsg(senderId);
            log.info("Sent Election message to Node {}", address.getNodeID());
        } catch (RemoteException | InterruptedException e) {
            log.warn("Node {} unreachable during election. Exception: {}", address, e.getMessage());
        }
    }

    /**
     * Sends an OK message to a specific node in response to an election message.
     *
     * @param receiverId The ID of the node to send the OK message to.
     */
    public void sendOKMessage(long receiverId) {
        Address receiverAddr = null;

        // Find the address of the receiver in the list of neighbors
        for (Address addr : myNode.getNeighbours().getNeighbours()) {
            if (addr.getNodeID() == receiverId) {
                receiverAddr = addr;
                break;
            }
        }

        if (receiverAddr != null) {
            try {
                NodeCommands remoteNode = getRMIProxy(receiverAddr);
                remoteNode.receiveOK(myNode.getNodeId());
                log.info("Sent OK message to Node {}", receiverId);
            } catch (RemoteException e) {
                log.error("Failed to send OK message to Node {}: {}", receiverId, e.getMessage());
            }
        } else {
            log.warn("Receiver Node {} not found in neighbors.", receiverId);
        }
    }
}
