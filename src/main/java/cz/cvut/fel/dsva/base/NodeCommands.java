package cz.cvut.fel.dsva.base;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeCommands extends Remote {

        void notifyAboutLogOut(Address address) throws RemoteException;
        void notifyAboutNewLeader(Address address) throws RemoteException;
        void notifyAboutRevival(Address revivedNode) throws RemoteException;

        void join(Address addr) throws RemoteException;

        void sendElectionMsg(long senderId) throws RemoteException;

        void receiveOK(long fromId) throws RemoteException;
        void updateLeader(Address leaderAddress) throws RemoteException;

        void sendMessage(long senderId, int receiverID, String content) throws RemoteException;

        void checkStatusOfLeader(long senderId) throws RemoteException;

        Address getCurrentLeader() throws RemoteException;

        void notifyAboutLeaderDeath(Address deadLeader) throws RemoteException;

        void addNeighbor(Address addr) throws RemoteException;
}
