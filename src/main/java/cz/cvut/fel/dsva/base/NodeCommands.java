package cz.cvut.fel.dsva.base;

import cz.cvut.fel.dsva.Node;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface NodeCommands extends Remote {

        // Уведомления о действиях в сети
        void notifyAboutJoin(Address address) throws RemoteException;
        void notifyAboutLogOut(Address address) throws RemoteException;
        void notifyAboutNewLeader(Address address) throws RemoteException;
        void notifyAboutRevival(Address revivedNode) throws RemoteException;

        // Топология и взаимодействие с узлами
        DSNeighbours join(Address addr) throws RemoteException;
        void repairTopologyAfterJoin(Address address) throws RemoteException;
        void repairTopologyAfterLogOut(int nodeID) throws RemoteException;
        void repairTopologyWithNewLeader(List<Address> addresses, Address address) throws RemoteException;

        // Алгоритм выбора лидера
        void sendElectionMsg(long senderId) throws RemoteException;
        void Election(long id) throws RemoteException;
        void Elected(long id, Address leaderAddr) throws RemoteException;

        void receiveOK(long fromId) throws RemoteException;
        void updateLeader(Address leaderAddress) throws RemoteException;

        // Сообщения между узлами
        void sendMessage(long senderId, int receiverID, String content) throws RemoteException;
        void receiveMessage(String message, long receiverId) throws RemoteException;

        // Проверка состояния узлов
        void checkStatusOfLeader(long senderId) throws RemoteException;

        // Информация о текущем состоянии узла
        void help() throws RemoteException;
        void printStatus(Node node) throws RemoteException;
        Address getCurrentLeader() throws RemoteException;

        // Управление узлом
        void killNode() throws RemoteException;  // "Убийство" узла
        void reviveNode() throws RemoteException; // Возрождение узла
        void resetTopology() throws RemoteException;

        // Управление задержкой сообщений
        void setDelay(long delay) throws RemoteException;
}
