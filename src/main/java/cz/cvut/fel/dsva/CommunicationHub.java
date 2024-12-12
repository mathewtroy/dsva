package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.DSNeighbours;
import cz.cvut.fel.dsva.base.NodeCommands;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CommunicationHub {
    private DSNeighbours actNeighbours = null;
    private Address myAddress = null;
    private NodeCommands myMessageReceiver = null;


    public CommunicationHub (Node node) {
        this.myAddress = node.getAddress();
        this.actNeighbours = node.getNeighbours();
        this.myMessageReceiver = node.getMessageReceiver();
    }


    public NodeCommands getNext() throws RemoteException {
        return getRMIProxy(actNeighbours.next);
    }


    public NodeCommands getNNext() throws RemoteException {
        return getRMIProxy(actNeighbours.nnext);
    }


    public NodeCommands getPrev() throws RemoteException {
        return getRMIProxy(actNeighbours.prev);
    }


    public NodeCommands getLeader() throws RemoteException {
        return getRMIProxy(actNeighbours.leader);
    }


    public NodeCommands getRMIProxy(Address address) throws RemoteException {
        if (address.compareTo(myAddress) == 0 ) return myMessageReceiver;
        else {
            try {
                Registry registry = LocateRegistry.getRegistry(address.hostname, address.port);
                return (NodeCommands) registry.lookup(Node.COMM_INTERFACE_NAME);
            } catch (NotBoundException nbe) {
                // transitive RM exception
                throw new RemoteException(nbe.getMessage());
            }
        }
    }


    public void setActNeighbours(DSNeighbours actNeighbours) {
        this.actNeighbours = actNeighbours;
    }
}