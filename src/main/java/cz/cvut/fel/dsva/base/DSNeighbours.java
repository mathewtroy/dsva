package cz.cvut.fel.dsva.base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Getter
@Setter
public class DSNeighbours implements Serializable {
    private List<Address> neighbours;
    private volatile Address leaderNode;

    public DSNeighbours() {
        this.neighbours = new CopyOnWriteArrayList  <>();
    }

    public Address getAddressById(long id) {
        for (Address address : neighbours) {
            if (address.getNodeID().equals(id)) { // Ensure type consistency
                return address;
            }
        }
        return null;
    }

    public synchronized void removeNodeById(int nodeId) {
        Address addressToRemove = getAddressById(nodeId);
        if (addressToRemove != null) {
            neighbours.remove(addressToRemove);
            log.info("Node with ID {} has been removed.", nodeId);
            log.info("Node with ID " + nodeId + " has been removed from neighbours.");
        } else {
            log.warn("Attempted to remove Node ID {}, but it was not found.", nodeId);
            log.info("Node with ID " + nodeId + " not found in neighbours.");
        }
    }

    public boolean isLeaderPresent() {
        return leaderNode != null;
    }

    public synchronized void removeNode(Address address) {
        boolean removed = neighbours.remove(address);
        if (removed) {
            log.info("Node {} removed from neighbours.", address.getNodeID());
            log.info("Node " + address.getNodeID() + " removed from neighbours.");

            if(leaderNode != null && leaderNode.equals(address)) {
                setLeaderNode(null);
            }
        } else {
            log.warn("Attempted to remove Node {}, but it was not found.", address.getNodeID());
            log.info("Node " + address.getNodeID() + " not found in neighbours.");
        }
    }

    public synchronized void addNewNode(Address address) {
        if (!neighbours.contains(address)) {
            neighbours.add(address);
            log.info("Node {} added to neighbours.", address.getNodeID());
            log.info("Node added: " + address);
        } else {
            log.debug("Node {} is already in neighbours.", address.getNodeID());
            log.info("Node " + address.getNodeID() + " is already a neighbour.");
        }
    }

    @Override
    public synchronized String toString() {
        return "DSNeighbours {" +
                "neighbours=" + neighbours +
                ", leaderNode=" + leaderNode +
                '}';
    }
}
