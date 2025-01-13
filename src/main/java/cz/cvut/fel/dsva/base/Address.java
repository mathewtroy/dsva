package cz.cvut.fel.dsva.base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an address consisting of a node ID, hostname, and port.
 * Used for identifying and connecting nodes in the system.
 */
@Slf4j
@Getter
@Setter
public class Address implements Serializable {
    private long nodeID;       // Node identifier, might be 0 if not set
    private String hostname;   // Hostname or IP address of the node
    private int port;          // Port number for communication

    /**
     * Returns a string representation of the Address object.
     *
     * @return String containing node ID, hostname, and port.
     */
    @Override
    public String toString() {
        return "Address: nodeId: " + (nodeID == 0 ? "null" : nodeID) +
                " hostname: " + hostname +
                " port: " + port;
    }

    /**
     * Constructs an Address object with a specified node ID, hostname, and port.
     *
     * @param nodeID   Node ID to identify the node.
     * @param hostname Hostname or IP address of the node.
     * @param port     Port number for communication.
     */
    public Address(long nodeID, String hostname, int port) {
        this.nodeID = nodeID;
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Constructs an Address object with a hostname and port,
     * where the node ID is set to 0 by default.
     *
     * @param hostname Hostname or IP address of the node.
     * @param port     Port number for communication.
     */
    public Address(String hostname, int port) {
        this.nodeID = 0; // Default value for node ID
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Compares this Address object with another for equality.
     * Two Address objects are considered equal if they have the same hostname and port.
     *
     * @param o The object to compare with.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;
        Address that = (Address) o;
        return this.port == that.port && Objects.equals(this.hostname, that.hostname);
    }

    /**
     * Generates a hash code for the Address object based on hostname and port.
     *
     * @return Hash code as an integer.
     */
    @Override
    public int hashCode() {
        return Objects.hash(hostname, port);
    }
}
