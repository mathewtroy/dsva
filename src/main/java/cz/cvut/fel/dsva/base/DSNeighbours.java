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

    public boolean isLeaderPresent() {
        return leaderNode != null;
    }

    public synchronized void removeNode(Address address) {
        boolean removed = neighbours.remove(address);
        if (removed) {
            log.info("Node {} removed from neighbours.", address.getNodeID());

            if(leaderNode != null && leaderNode.equals(address)) {
                setLeaderNode(null);
            }
        } else {
            log.warn("Attempted to remove Node {}, but it was not found.", address.getNodeID());
            log.info("Node {} not found in neighbours.", address.getNodeID());
        }
    }

    public synchronized void addNewNode(Address address) {
        if (!neighbours.contains(address)) {
            neighbours.add(address);
            log.info("Node {} added to neighbours.", address.getNodeID());
            log.info("Node added: {}", address);
        } else {
            log.debug("Node {} is already in neighbours.", address.getNodeID());
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
