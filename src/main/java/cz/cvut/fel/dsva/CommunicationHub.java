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

@Slf4j
@Getter
@Setter
public class CommunicationHub {
    private final Node myNode;
    private long messageDelay = 0; // Delay in milliseconds for simulating message transmission delay

    public CommunicationHub(Node node) {
        this.myNode = node;
    }

    public NodeCommands getRMIProxy(Address address) throws RemoteException {
        try {
            if (messageDelay > 0) {
                Thread.sleep(messageDelay); // Simulating message delay
            }
            Registry registry = LocateRegistry.getRegistry(address.getHostname(), address.getPort());
            return (NodeCommands) registry.lookup(Node.COMM_INTERFACE_NAME);
        } catch (NotBoundException | InterruptedException e) {
            throw new RemoteException("Failed to get RMI proxy for: " + address, e);
        }
    }

    public void sendElectionMessage(Address address, long senderId) {
        try {
            if (messageDelay > 0) {
                Thread.sleep(messageDelay); // Simulating message delay
            }
            NodeCommands remoteNode = getRMIProxy(address);
            remoteNode.sendElectionMsg(senderId);
            log.info("Sent Election message to Node {}", address.getNodeID());
        } catch (RemoteException | InterruptedException e) {
            log.warn("Node {} unreachable during election. Exception: {}", address, e.getMessage());
        }
    }

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
