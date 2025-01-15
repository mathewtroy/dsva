package cz.cvut.fel.dsva.base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Getter
@Setter
public class DSNeighbours implements Serializable {
    private Set<Address> knownNodes = new HashSet<>();
    private Address leader;

    public DSNeighbours(Address self) {
        this.leader = self;
        knownNodes.add(self);
    }

    public void addNode(Address addr) {
        knownNodes.add(addr);
    }

    public void removeNode(Address addr) {
        knownNodes.remove(addr);
    }

    @Override
    public String toString() {
        return "DSNeighbours{ leader=" + leader + ", knownNodes=" + knownNodes + " }";
    }
}
