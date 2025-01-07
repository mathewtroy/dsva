package cz.cvut.fel.dsva.base;

import cz.cvut.fel.dsva.Node;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface NodeCommands extends Remote {

        void notifyAboutLogOut(Address address) throws RemoteException;
        void notifyAboutNewLeader(Address address) throws RemoteException;
        void notifyAboutRevival(Address revivedNode) throws RemoteException;

        DSNeighbours join(Address addr) throws RemoteException;

        void sendElectionMsg(long senderId) throws RemoteException;

        void receiveOK(long fromId) throws RemoteException;
        void updateLeader(Address leaderAddress) throws RemoteException;

        void sendMessage(long senderId, int receiverID, String content) throws RemoteException;

        void checkStatusOfLeader(long senderId) throws RemoteException;

        Address getCurrentLeader() throws RemoteException;

        void notifyAboutLeaderDeath(Address leaderNode) throws RemoteException;
}
