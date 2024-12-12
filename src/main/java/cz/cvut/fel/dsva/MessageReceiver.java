package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.DSNeighbours;
import cz.cvut.fel.dsva.base.NodeCommands;

import java.rmi.RemoteException;

public class MessageReceiver implements NodeCommands {
    private Node myNode = null;


    public MessageReceiver(Node node) {
        this.myNode = node;
    }


    @Override
    public DSNeighbours join(Address addr) throws RemoteException {
        System.out.println("JOIN was called ...");
        if (addr.compareTo(myNode.getAddress()) == 0) {
            System.out.println("I am the first and leader");
            return myNode.getNeighbours();
        } else {
            System.out.println("Someone is joining ...");
            DSNeighbours myNeighbours = myNode.getNeighbours();
            Address myInitialNext = new Address(myNeighbours.next);     // because of 2 nodes config
            Address myInitialPrev = new Address(myNeighbours.prev);     // because of 2 nodes config
            DSNeighbours tmpNeighbours = new DSNeighbours(myNeighbours.next,
                                                            myNeighbours.nnext,
                                                            myNode.getAddress(),
                                                            myNeighbours.leader);
            // to my (initial) next send msg ChPrev to addr
            myNode.getCommHub().getNext().ChngPrev(addr);
            // to my (initial) prev send msg ChNNext addr
            myNode.getCommHub().getRMIProxy(myInitialPrev).ChngNNext(addr);
            tmpNeighbours.nnext = myNeighbours.nnext;
            // handle myself
            myNeighbours.nnext = myInitialNext;
            myNeighbours.next = addr;
            return tmpNeighbours;
        }
    }


    @Override
    public void ChngNNext(Address addr) throws RemoteException {
        System.out.println("ChngNNext was called ...");
        myNode.getNeighbours().nnext = addr;
    }


    @Override
    public Address ChngPrev(Address addr) throws RemoteException {
        System.out.println("ChngPrev was called ...");
        myNode.getNeighbours().prev = addr;
        return myNode.getNeighbours().next;
    }


    @Override
    public void NodeMissing(Address addr) throws RemoteException {
        System.out.println("NodeMissing was called with " + addr);
        if (addr.compareTo(myNode.getNeighbours().next) == 0) {
            // its for me
            DSNeighbours myNeighbours = myNode.getNeighbours();
            // to my nnext send msg ChPrev with myaddr -> my nnext = next
            myNeighbours.next = myNeighbours.nnext;
            myNeighbours.nnext = myNode.getCommHub().getNNext().ChngPrev(myNode.getAddress());
            // to my prev send msg ChNNext to my.next
            myNode.getCommHub().getPrev().ChngNNext(myNeighbours.next);
            System.out.println("NodeMissing DONE");
        } else {
            // send to next node
            myNode.getCommHub().getNext().NodeMissing(addr);
        }
    }


    @Override
    public void Election(long id) throws RemoteException {
        System.out.println("Election was called with id " + id);
        if (myNode.getNodeId() < id) {
            myNode.voting = true;
            myNode.getCommHub().getNext().Election(id);
        } else if ((myNode.getNodeId() > id) & (myNode.voting == false)) {
            myNode.voting = true;
            myNode.getCommHub().getNext().Election(myNode.getNodeId());
        } else if (myNode.getNodeId() == id) {
            // same -> I am leader
            myNode.getCommHub().getNext().Elected(id, myNode.getAddress());
        }
    }


    @Override
    public void Elected(long id, Address leaderAddr) throws RemoteException {
        System.out.println("Elected was called with id " + id);
        myNode.getNeighbours().leader = leaderAddr;
        myNode.voting = false;
        if (myNode.getNodeId() != id) {
            myNode.getCommHub().getNext().Elected(id, leaderAddr);
        }
    }


    @Override
    public void SendMsg(String toNickName, String fromNickName, String message) throws RemoteException {

    }


    @Override
    public void Register(String nickName, Address addr) throws RemoteException {

    }


    @Override
    public void Hello() throws RemoteException {
        System.out.println("Hello was called ...");
    }
}
