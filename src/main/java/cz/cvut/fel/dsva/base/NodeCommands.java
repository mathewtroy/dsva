package cz.cvut.fel.dsva.base;

import cz.cvut.fel.dsva.Node;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface NodeCommands extends Remote {

        void notifyAboutJoin(Address address) throws RemoteException;
        void notifyAboutLogOut(Address address) throws RemoteException;
        void notifyAboutNewLeader(Address address) throws RemoteException;

        DSNeighbours join(Address addr) throws RemoteException;

        void sendElectionMsg(long senderId) throws RemoteException;
        void Election(long id) throws RemoteException;
        void Elected(long id, Address leaderAddr) throws RemoteException;

        void repairTopologyAfterJoin(Address address) throws RemoteException;
        void repairTopologyAfterLogOut(int nodeID) throws RemoteException;
        void repairTopologyWithNewLeader(List<Address> addresses, Address address) throws RemoteException;

        void sendMessage(long senderId, int receiverID, String content) throws RemoteException;
        void receiveMessage(String message, long receiverId) throws RemoteException;
        void checkStatusOfLeader(long senderId) throws RemoteException;

        void help() throws RemoteException;
        void printStatus(Node node) throws RemoteException;
        Address getCurrentLeader() throws RemoteException;

        void killNode() throws RemoteException;
        void reviveNode() throws RemoteException;
        void setDelay(long delay) throws RemoteException;

        void receiveOK(long fromId) throws RemoteException;
        void updateLeader(Address leaderAddress) throws RemoteException;
        void resetTopology() throws RemoteException;



}