package cz.cvut.fel.dsva.base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static cz.cvut.fel.dsva.Color.*;

@Slf4j
@Getter
@Setter
public class DSNeighbours implements Serializable {
    private List<Address> neighbours;
    private Address leaderNode;

    public DSNeighbours() {
        this.neighbours = new ArrayList<>();
    }


    public Address getAddressById(int id) {
        for (Address address : neighbours) {
            if (address.getNodeID() == id) {
                return address;
            }
        }
        return null;
    }


    public Long getNodeIdFromAddress(Address address) {
        for (Address neighbour : neighbours) {
            if (neighbour.equals(address)) {
                return neighbour.getNodeID();
            }
        }
        return -1L;
    }


    public void removeNodeById(int nodeId) {
        Iterator<Address> iterator = neighbours.iterator();
        while (iterator.hasNext()) {
            Address address = iterator.next();
            if (getNodeIdFromAddress(address) == nodeId) {
                iterator.remove();
                log.info(GREEN + "Node with ID {} has been removed.", nodeId);
                break;
            }
        }
    }


    public boolean isLeaderPresent() {
        return leaderNode != null;
    }


    public void addNewNode(Address address) {
        if (address.getPort() > 0 && !address.getHostname().isEmpty()) {
            if (!neighbours.contains(address)) {
                neighbours.add(address);
                log.info(GREEN + "Node {} has been added as a neighbour.", address);
            } else {
                log.warn(YELLOW + "Node {} is already in neighbours list.", address);
            }
        } else {
            log.error(RED + "Invalid neighbour address: {}", address);
        }
    }



    public void removeNode(Address address) {
        if (neighbours.contains(address)) {
            neighbours.remove(address);
            log.info(GREEN + "Node {} has been removed from neighbours.", address);
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