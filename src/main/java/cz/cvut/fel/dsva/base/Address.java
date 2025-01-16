package cz.cvut.fel.dsva.base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Slf4j
@Getter
@Setter
public class Address implements Comparable<Address>, Serializable {
    private String hostname;
    private Integer port;

    public Address() {
        this("127.0.0.1", 2010);
    }

    public Address(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public Address(Address other) {
        this(other.hostname, other.port);
    }

    @Override
    public String toString() {
        return "Address[" + hostname + ":" + port + "]";
    }

    @Override
    public int compareTo(Address other) {
        int cmp = this.hostname.compareTo(other.hostname);
        if (cmp == 0) {
            cmp = this.port.compareTo(other.port);
        }
        return cmp;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Address)) return false;
        Address other = (Address) obj;
        return this.hostname.equals(other.hostname) && this.port.equals(other.port);
    }

    @Override
    public int hashCode() {
        return hostname.hashCode() * 31 + port.hashCode();
    }
}
