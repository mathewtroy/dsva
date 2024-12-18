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
    private long messageDelay = 0;

    public CommunicationHub(Node node) {
        this.myNode = node;
    }

    public NodeCommands getRMIProxy(Address address) throws RemoteException {
        try {
            if (messageDelay > 0) {
                Thread.sleep(messageDelay);
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
                Thread.sleep(messageDelay);
            }
            NodeCommands remoteNode = getRMIProxy(address);
            remoteNode.sendElectionMsg(senderId);
            log.info("Sent Election message to Node {}", address.getNodeID());
        } catch (RemoteException | InterruptedException e) {
            System.out.println("Node " + address + " is unreachable during election.");
            log.warn("Node {} unreachable during election, exception: {}", address, e.getMessage());
        }
    }

    public void sendOKMessage(long receiverId) {
        Address receiverAddr = null;
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
                System.out.println("Failed to send OK message to Node " + receiverId);
            }
        } else {
            log.warn("Receiver Node {} not found in neighbours.", receiverId);
            System.out.println("Receiver Node " + receiverId + " not found in neighbours.");
        }
    }


    public void notifyLeaderToAll() {
        Address leader = myNode.getNeighbours().getLeaderNode();
        for (Address neighbour : myNode.getNeighbours().getNeighbours()) {
            try {
                if (messageDelay > 0) {
                    Thread.sleep(messageDelay);
                }
                NodeCommands remoteNode = getRMIProxy(neighbour);
                remoteNode.Elected(leader.getNodeID(), leader);
                log.info("Notified Node {} about new leader.", neighbour.getNodeID());
            } catch (RemoteException | InterruptedException e) {
                System.out.println("Failed to notify Node " + neighbour + " about new leader.");
                log.warn("Failed to notify Node {} about new leader: {}", neighbour, e.getMessage());
            }
        }
    }
}
