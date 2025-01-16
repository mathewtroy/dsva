package cz.cvut.fel.dsva.base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the collection of known neighboring nodes in the network.
 *
 * <p>This class maintains a set of {@link Address} instances representing known nodes,
 * and tracks the current leader of the network.
 *
 * <p>Key functionalities include:
 * <ul>
 *     <li>Adding and removing nodes from the known neighbors.</li>
 *     <li>Setting and retrieving the current leader.</li>
 * </ul>
 *
 * @author Kross Aleksandr
 */
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
