package cz.cvut.fel.dsva.base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Objects;

@Slf4j
@Getter
@Setter
public class Address implements Comparable<Address>, Serializable {
    private String hostname;
    private Integer port;
    private Long nodeID;
    private boolean isOnline = true;

    // Constructor without nodeID
    public Address(String hostname, int port){
        this.hostname = hostname;
        this.port = port;
    }

    // Constructor with nodeID
    public Address(String hostname, int port, Long nodeID){
        this.hostname = hostname;
        this.port = port;
        this.nodeID = nodeID;
    }

    @Override
    public String toString(){
        return "Address: nodeId: " + nodeID + " hostname: " + hostname + " port: " + port;
    }

    @Override
    public boolean equals(Object object){
        if (this == object) return true;
        if (!(object instanceof Address)) return false;
        Address address = (Address) object;
        return Objects.equals(nodeID, address.nodeID);
    }

    @Override
    public int hashCode(){
        return Objects.hash(nodeID);
    }

    @Override
    public int compareTo(Address address) {
        return this.nodeID.compareTo(address.nodeID);
    }
}
