package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

import static cz.cvut.fel.dsva.Color.GREEN;
import static cz.cvut.fel.dsva.Color.RED;

@Slf4j
@Getter
@Setter
public class BullyAlgorithm {
    private final Node node;
    private final long electionTimeout = 5000;
    private final long electedTimeout = 5000;
    private boolean waitingForOK = false;
    private boolean waitingForElected = false;
    private Timer electionTimer;
    private Timer electedTimer;

    public BullyAlgorithm(Node node) {
        this.node = node;
    }

    public synchronized void startElection() {
        if (node.isVoting()) {
            log.info(GREEN + "Node {} is already participating in an election.", node.getNodeId());
            return;
        }

        node.setVoting(true);
        node.setReceivedOK(false);
        log.info(GREEN + "Node {} initiates an election.", node.getNodeId());
        System.out.println("Election process started by node: " + node.getNodeId());

        boolean higherNodeExists = false;
        for (Address neighbour : node.getNeighbours().getNeighbours()) {
            if (neighbour.getNodeID() > node.getNodeId()) {
                higherNodeExists = true;
                node.getCommHub().sendElectionMessage(neighbour, node.getNodeId());
            }
        }

        if (!higherNodeExists) {
            declareLeader(node.getAddress());
        } else {
            waitingForOK = true;
            startElectionTimer();
        }
    }

    public synchronized void onElectionMsgFromLower(long senderId) {
        System.out.println("Election message received from Node: " + senderId);
        if (node.getNodeId() > senderId) {
            System.out.println("Node " + node.getNodeId() + " responds OK to " + senderId);
            node.getCommHub().sendOKMessage(senderId);

            if (!node.isVoting()) {
                startElection();
            }
        }
    }

    public synchronized void onOKReceived(long fromId) {
        node.setReceivedOK(true);
        waitingForOK = false;
        waitingForElected = true;
        log.info("Node {} received OK from Node {}.", node.getNodeId(), fromId);
        System.out.println("Node " + node.getNodeId() + " received OK from Node " + fromId);
        cancelElectionTimer();
        startElectedTimer();
    }

    public synchronized void onElectedReceived(long leaderId, Address leaderAddr) {
        System.out.println("Leader elected: Node " + leaderId);
        node.getNeighbours().setLeaderNode(leaderAddr);
        node.setVoting(false);
        node.setCoordinator(node.getNodeId() == leaderId);
        log.info(GREEN + "Node {} acknowledges Node {} as leader.", node.getNodeId(), leaderId);
        System.out.println("Node " + node.getNodeId() + " acknowledges Node " + leaderId + " as leader.");
        waitingForElected = false;
        cancelElectedTimer();
    }

    private void startElectionTimer() {
        cancelElectionTimer();
        electionTimer = new Timer();
        electionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (BullyAlgorithm.this) {
                    if (waitingForOK && !node.isReceivedOK()) {
                        declareLeader(node.getAddress());
                    }
                }
            }
        }, electionTimeout);
    }

    private void startElectedTimer() {
        cancelElectedTimer();
        electedTimer = new Timer();
        electedTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (BullyAlgorithm.this) {
                    if (waitingForElected && !node.getNeighbours().isLeaderPresent()) {
                        declareLeader(node.getAddress());
                    }
                }
            }
        }, electedTimeout);
    }

    private void cancelElectionTimer() {
        if (electionTimer != null) {
            electionTimer.cancel();
            electionTimer = null;
        }
    }

    private void cancelElectedTimer() {
        if (electedTimer != null) {
            electedTimer.cancel();
            electedTimer = null;
        }
    }

    private void cancelAllTimers() {
        cancelElectionTimer();
        cancelElectedTimer();
    }

    /**
     * Declares a new leader.
     *
     * @param newLeader Address of the new leader
     */
    private void declareLeader(Address newLeader) {
        node.setCoordinator(node.getAddress().equals(newLeader));
        node.getNeighbours().setLeaderNode(newLeader);

        node.getNeighbours().getNeighbours().forEach(neighbour -> {
            try {
                NodeCommands proxy = node.getCommHub().getRMIProxy(neighbour);
                proxy.notifyAboutNewLeader(newLeader);
            } catch (RemoteException e) {
                log.error(RED + "Failed to notify {} about new leader: {}", neighbour, e.getMessage());
            }
        });

        log.info(GREEN + "Leader declared: Node {}.", newLeader.getNodeID());
        System.out.println("Leader declared: Node " + newLeader.getNodeID());
    }
}
