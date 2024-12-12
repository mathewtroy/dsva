package cz.cvut.fel.dsva.base;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeCommands extends Remote {
        public DSNeighbours join(Address addr) throws RemoteException;
        //      JOINReply(next, prev, nnext, leader)
        public void ChngNNext(Address addr) throws RemoteException;
        public Address ChngPrev(Address addr) throws RemoteException;
        public void NodeMissing(Address addr) throws RemoteException;
        public void Election(long id) throws RemoteException;
        public void Elected(long id, Address leaderAddr) throws RemoteException;
        public void SendMsg(String toNickName, String fromNickName, String message) throws RemoteException;
        public void Register(String nickName, Address addr) throws RemoteException;
        public void Hello() throws RemoteException;
}