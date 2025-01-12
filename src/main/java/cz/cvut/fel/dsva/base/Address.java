package cz.cvut.fel.dsva.base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Objects;

@Slf4j
@Getter
@Setter
public class Address implements Serializable {
    private long nodeID;       // might be 0 if not set
    private String hostname;
    private int port;

    // For printing
    @Override
    public String toString() {
        return "Address: nodeId: " + (nodeID == 0 ? "null" : nodeID) +
                " hostname: " + hostname +
                " port: " + port;
    }

    // *** ADDED ***
    // If you want a convenience constructor that sets nodeID directly
    public Address(long nodeID, String hostname, int port) {
        this.nodeID = nodeID;
        this.hostname = hostname;
        this.port = port;
    }

    // *** CHANGED ***
    // Default constructor (no nodeID)
    public Address(String hostname, int port) {
        this.nodeID = 0;  // or 'null' effectively
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;
        Address that = (Address) o;
        // We typically consider them equal if same hostname & port
        return this.port == that.port && Objects.equals(this.hostname, that.hostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port);
    }
}
