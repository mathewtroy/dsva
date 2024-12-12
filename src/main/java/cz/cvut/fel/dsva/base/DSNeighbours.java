package cz.cvut.fel.dsva.base;

import java.io.Serializable;

public class DSNeighbours implements Serializable {
    public Address next;
    public Address nnext;
    public Address prev;
    public Address leader;


    public DSNeighbours (Address me) {
        this.next = me;
        this.nnext = me;
        this.prev = me;
        this.leader = me;
    }


    public DSNeighbours (Address next, Address nnext, Address prev, Address leader) {
        this.next = next;
        this.nnext = nnext;
        this.prev = prev;
        this.leader = leader;
    }


    @Override
    public String toString() {
        return("Neigh[next:'"+next+"', " +
                "nnext:'"+nnext+"', " +
                "prev:'"+prev+"', " +
                "leader:'"+leader+"']");
    }
}