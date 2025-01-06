package cz.cvut.fel.dsva.base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static cz.cvut.fel.dsva.Color.*;

@Slf4j
@Getter
@Setter
public class DSNeighbours implements Serializable {
    private List<Address> neighbours;
    private Address leaderNode;

    public DSNeighbours() {
        this.neighbours = new CopyOnWriteArrayList<>();
    }

    public Address getAddressById(int id) {
        for (Address address : neighbours) {
            if (address.getNodeID().equals((long) id)) { // Ensure type consistency
                return address;
            }
        }
        return null;
    }

    public void removeNodeById(int nodeId) {
        Address addressToRemove = getAddressById(nodeId);
        if (addressToRemove != null) {
            neighbours.remove(addressToRemove);
            log.info(GREEN + "Node with ID {} has been removed.", nodeId);
            System.out.println("Node with ID " + nodeId + " has been removed from neighbours.");
        } else {
            log.warn(YELLOW + "Attempted to remove Node ID {}, but it was not found.", nodeId);
            System.out.println("Node with ID " + nodeId + " not found in neighbours.");
        }
    }

    public boolean isLeaderPresent() {
        return leaderNode != null;
    }

    public void removeNode(Address address) {
        boolean removed = neighbours.remove(address);
        if (removed) {
            log.info(GREEN + "Node {} removed from neighbours.", address.getNodeID());
            System.out.println("Node " + address.getNodeID() + " removed from neighbours.");
        } else {
            log.warn(YELLOW + "Attempted to remove Node {}, but it was not found.", address.getNodeID());
            System.out.println("Node " + address.getNodeID() + " not found in neighbours.");
        }
    }

    public void addNewNode(Address address) {
        if (!neighbours.contains(address)) {
            neighbours.add(address);
            log.info(GREEN + "Node {} added to neighbours.", address.getNodeID());
            System.out.println("Node added: " + address);
        } else {
            log.debug(YELLOW + "Node {} is already in neighbours.", address.getNodeID());
            System.out.println("Node " + address.getNodeID() + " is already a neighbour.");
        }
    }

    @Override
    public String toString() {
        return "DSNeighbours {" +
                "neighbours=" + neighbours +
                ", leaderNode=" + leaderNode +
                '}';
    }
}
