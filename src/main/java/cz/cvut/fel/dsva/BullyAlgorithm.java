package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@Getter
@Setter
public class BullyAlgorithm {
    private final Node node;
    private final long electionTimeout = 5000;  // Timeout for waiting OK from higher ID
    private final long electedTimeout = 5000;   // Timeout for waiting Coordinator/Elected
    private boolean waitingForOK = false;
    private boolean waitingForElected = false;
    private Timer electionTimer;
    private Timer electedTimer;

    public BullyAlgorithm(Node node) {
        this.node = node;
    }

    public synchronized void startElection() {
        // If already participating in an election, do nothing
        if (node.isVoting()) {
            log.info("Node {} is already participating in an election.", node.getNodeId());
            return;
        }

        // Mark the node as voting
        node.setVoting(true);
        node.setReceivedOK(false);
        log.info("Node {} initiates an election.", node.getNodeId());

        boolean higherNodeExists = false;
        // Send election messages to all neighbors with a higher ID
        for (Address neighbour : node.getNeighbours().getNeighbours()) {
            if (neighbour.getNodeID() > node.getNodeId()) {
                higherNodeExists = true;
                node.getCommHub().sendElectionMessage(neighbour, node.getNodeId());
                log.info("Election message sent to higher Node {}.", neighbour.getNodeID());
            }
        }

        // If no higher ID node exists, declare this node as the leader
        if (!higherNodeExists) {
            log.info("No higher node found. Node {} declares itself as leader.", node.getNodeId());
            declareLeader(node.getAddress());
        } else {
            // Wait for OK from higher node(s)
            waitingForOK = true;
            startElectionTimer();
        }
    }

    public synchronized void onElectionMsgFromLower(long senderId) {
        log.info("Election message received from Node {}.", senderId);
        if (node.getNodeId() > senderId) {
            log.info("Node {} responds OK to Node {}.", node.getNodeId(), senderId);
            node.getCommHub().sendOKMessage(senderId);

            // If not already voting, start the election process
            if (!node.isVoting()) {
                startElection();
            }
        }
    }

    public synchronized void onOKReceived(long fromId) {
        node.setReceivedOK(true);
        waitingForOK = false;
        waitingForElected = true;  // Expecting Coordinator/Elected message
        log.info("Node {} received OK from Node {}.", node.getNodeId(), fromId);
        cancelElectionTimer();     // Cancel waiting for election timeout
        startElectedTimer();       // Start waiting for the Elected message
    }

    public synchronized void onElectedReceived(long leaderId, Address leaderAddr) {
        log.info("Leader elected: Node {}.", leaderId);

        node.getNeighbours().setLeaderNode(leaderAddr);

        // Stop voting, as a leader is elected
        node.setVoting(false);

        // Update coordinator status if this node is the leader
        node.setCoordinator(node.getNodeId() == leaderId);
        log.info("Node {} acknowledges Node {} as the leader.", node.getNodeId(), leaderId);

        waitingForElected = false;
        cancelElectedTimer();
    }

    private void startElectionTimer() {
        cancelElectionTimer();
        electionTimer = new Timer();
        log.info("Election timer started for Node {}.", node.getNodeId());
        electionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (BullyAlgorithm.this) {
                    if (waitingForOK && !node.isReceivedOK()) {
                        log.info("Election timeout reached for Node {}. Declaring self as leader.", node.getNodeId());
                        declareLeader(node.getAddress());
                    }
                }
            }
        }, electionTimeout);
    }

    private void startElectedTimer() {
        cancelElectedTimer();
        electedTimer = new Timer();
        log.info("Elected timer started for Node {}.", node.getNodeId());
        electedTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (BullyAlgorithm.this) {
                    if (waitingForElected && !node.getNeighbours().isLeaderPresent()) {
                        log.info("Elected timeout reached for Node {}. Declaring self as leader.", node.getNodeId());
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
            log.info("Election timer cancelled for Node {}.", node.getNodeId());
        }
    }

    private void cancelElectedTimer() {
        if (electedTimer != null) {
            electedTimer.cancel();
            electedTimer = null;
            log.info("Elected timer cancelled for Node {}.", node.getNodeId());
        }
    }

    private void cancelAllTimers() {
        cancelElectionTimer();
        cancelElectedTimer();
    }

    public synchronized void declareLeader(Address newLeader) {
        boolean shouldDeclare = false;

        if (node.getAddress().equals(newLeader)) {
            // Declare itself as the leader
            shouldDeclare = true;
            log.info("Node {} recognizes itself as the new leader.", node.getNodeId());
        } else {
            // Accept a new leader only if no leader is set
            if (!node.isCoordinator() && node.getNeighbours().getLeaderNode() == null) {
                shouldDeclare = true;
                log.info("Node {} accepts Node {} as the new leader.", node.getNodeId(), newLeader.getNodeID());
            }
        }

        if (shouldDeclare) {
            node.setCoordinator(node.getAddress().equals(newLeader));
            node.getNeighbours().setLeaderNode(newLeader);

            log.info("Node {} is declaring itself as the leader.", node.getNodeId());

            // Notify all neighbors about the new leader
            for (Address neighbour : node.getNeighbours().getNeighbours()) {
                try {
                    NodeCommands proxy = node.getCommHub().getRMIProxy(neighbour);
                    proxy.notifyAboutNewLeader(newLeader);
                    log.info("Notified Node {} about the new leader Node {}.", neighbour.getNodeID(), newLeader.getNodeID());
                } catch (RemoteException e) {
                    log.error("Failed to notify Node {} about the new leader: {}", neighbour.getNodeID(), e.getMessage());
                }
            }

            log.info("Leader declared: Node {}.", newLeader.getNodeID());

            // Cancel all timers and reset voting flag
            cancelAllTimers();
            node.setVoting(false);
        } else {
            log.info("Attempted to declare leader Node {}, but leader is already set to Node {}.",
                    newLeader.getNodeID(),
                    node.getNeighbours().getLeaderNode().getNodeID());
        }
    }
}
