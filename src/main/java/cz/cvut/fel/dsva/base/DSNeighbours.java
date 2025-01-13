package cz.cvut.fel.dsva.base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents the list of neighbours in a distributed system and the leader node.
 * Manages the addition and removal of nodes and tracks the current leader.
 */
@Slf4j
@Getter
@Setter
public class DSNeighbours implements Serializable {
    private List<Address> neighbours;   // List of neighbouring nodes
    private volatile Address leaderNode; // The leader node in the distributed system

    /**
     * Default constructor that initializes the neighbours list as a thread-safe CopyOnWriteArrayList.
     */
    public DSNeighbours() {
        this.neighbours = new CopyOnWriteArrayList<>();
    }

    /**
     * Checks if a leader node is currently present in the system.
     *
     * @return true if the leader node is set, false otherwise.
     */
    public boolean isLeaderPresent() {
        return leaderNode != null;
    }

    /**
     * Removes a node from the neighbours list. If the removed node is the leader,
     * the leader node reference is cleared.
     *
     * @param address The address of the node to remove.
     */
    public synchronized void removeNode(Address address) {
        boolean removed = neighbours.remove(address);
        if (removed) {
            log.info("Node {} removed from neighbours.", address.getNodeID());
            if (leaderNode != null && leaderNode.equals(address)) {
                setLeaderNode(null);
            }
        } else {
            log.warn("Attempted to remove Node {}, but not found in neighbours.", address.getNodeID());
        }
    }

    /**
     * Adds a new node to the neighbours list if it is not already present.
     *
     * @param address The address of the node to add.
     */
    public synchronized void addNewNode(Address address) {
        if (!neighbours.contains(address)) {
            neighbours.add(address);
            log.info("Node {} added to neighbours. Address = {}", address.getNodeID(), address);
        } else {
            log.debug("Node {} is already in neighbours.", address.getNodeID());
        }
    }

    /**
     * Provides a string representation of the DSNeighbours object, including the list
     * of neighbours and the current leader node.
     *
     * @return A string describing the current state of the object.
     */
    @Override
    public synchronized String toString() {
        return "DSNeighbours {" +
                "neighbours=" + neighbours +
                ", leaderNode=" + leaderNode +
                '}';
    }
}
