package cz.cvut.fel.dsva.base;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines the remote methods that can be invoked by other nodes via RMI.
 *
 * <p>This interface extends {@link Remote} and includes methods for joining the network,
 * starting elections, responding to elections, announcing leaders, sending messages,
 * handling node departures and revivals, and receiving simple hello messages.
 *
 * @see java.rmi.Remote
 */
public interface NodeCommands extends Remote {
        DSNeighbours join(Address newNodeAddr) throws RemoteException;
        void broadcastNewNode(Address newAddr) throws RemoteException;

        void startElection(long candidateId) throws RemoteException;
        void respondOk(long fromNodeId) throws RemoteException;
        void announceLeader(long leaderId, Address leaderAddress) throws RemoteException;

        void sendMessage(String fromNick, String toNick, String message) throws RemoteException;

        void leave(Address leavingNode) throws RemoteException;
        void killNode(Address killedNode) throws RemoteException;
        void revive(Address revivedNode) throws RemoteException;
        void hello() throws RemoteException;
}
