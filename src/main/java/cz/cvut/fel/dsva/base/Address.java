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
    public String hostname;
    public Integer port;
    public Long nodeID;
    private boolean isOnline = true;

    public Address(String hostname, int port){
        this.hostname = hostname;
        this.port = port;
    }

    public Address(String hostname, int port, Long nodeID){
        this.hostname = hostname;
        this.port = port;
        this.nodeID = nodeID;
    }

    @Override
    public String toString(){
        return ("Address: " + "nodeId: " + nodeID + " " + "hostname: " + hostname + " " + "port: " + port);
    }

    @Override
    public boolean equals(Object object){
        if (object instanceof Address){
            Address address = (Address) object;
            return Objects.equals(address.getHostname(), hostname) &&
                    Objects.equals(address.getPort(), port) &&
                    Objects.equals(address.getNodeID(), nodeID);
        }
        return false;
    }

    @Override
    public int compareTo(Address address) {
        int retval;
        if ((retval = hostname.compareTo(address.getHostname())) == 0 ) {
            if ((retval = port.compareTo(address.getPort())) == 0 ) {
                return 0;
            }
            else
                return retval;
        }
        else
            return retval;
    }

}