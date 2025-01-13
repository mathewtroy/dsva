package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import cz.cvut.fel.dsva.base.NodeCommands;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implementation of the Bully Algorithm for leader election in a distributed system.
 * Handles election initiation, responses, and leader declaration.
 */
@Slf4j
@Getter
@Setter
public class BullyAlgorithm {
    private final Node node; // Reference to the current node
    private final long electionTimeout = 5000;  // Timeout for waiting OK from higher ID
    private final long electedTimeout = 5000;   // Timeout for waiting Coordinator/Elected
    private boolean waitingForOK = false;       // Flag to indicate waiting for OK messages
    private boolean waitingForElected = false;  // Flag to indicate waiting for Elected message
    private Timer electionTimer;               // Timer for election timeout
    private Timer electedTimer;                // Timer for elected timeout

    /**
     * Constructs the BullyAlgorithm instance for the given node.
     *
     * @param node The current node using this algorithm.
     */
    public BullyAlgorithm(Node node) {
        this.node = node;
    }

    /**
     * Initiates the leader election process.
     */
    public synchronized void startElection() {
        if (node.isVoting()) {
            log.info("Node {} is already participating in an election.", node.getNodeId());
            return;
        }

        node.setVoting(true);
        node.setReceivedOK(false);
        log.info("Node {} initiates an election.", node.getNodeId());

        boolean higherNodeExists = false;

        for (Address neighbour : node.getNeighbours().getNeighbours()) {
            if (neighbour.getNodeID() > node.getNodeId()) {
                higherNodeExists = true;
                node.getCommHub().sendElectionMessage(neighbour, node.getNodeId());
                log.info("Election message sent to higher Node {}.", neighbour.getNodeID());
            }
        }

        if (!higherNodeExists) {
            log.info("No higher node found. Node {} declares itself as leader.", node.getNodeId());
            declareLeader(node.getAddress());
        } else {
            waitingForOK = true;
            startElectionTimer();
        }
    }

    /**
     * Handles receiving an election message from a lower node.
     *
     * @param senderId ID of the sender node.
     */
    public synchronized void onElectionMsgFromLower(long senderId) {
        log.info("Election message received from Node {}.", senderId);
        if (node.getNodeId() > senderId) {
            log.info("Node {} responds OK to Node {}.", node.getNodeId(), senderId);
            node.getCommHub().sendOKMessage(senderId);

            if (!node.isVoting()) {
                startElection();
            }
        }
    }

    /**
     * Handles receiving an OK message from a higher node.
     *
     * @param fromId ID of the node that sent the OK message.
     */
    public synchronized void onOKReceived(long fromId) {
        node.setReceivedOK(true);
        waitingForOK = false;
        waitingForElected = true;
        log.info("Node {} received OK from Node {}.", node.getNodeId(), fromId);
        cancelElectionTimer();
        startElectedTimer();
    }

    /**
     * Handles receiving an Elected message indicating the new leader.
     *
     * @param leaderId ID of the elected leader.
     * @param leaderAddr Address of the elected leader.
     */
    public synchronized void onElectedReceived(long leaderId, Address leaderAddr) {
        log.info("Leader elected: Node {}.", leaderId);
        node.getNeighbours().setLeaderNode(leaderAddr);
        node.setVoting(false);
        node.setCoordinator(node.getNodeId() == leaderId);
        log.info("Node {} acknowledges Node {} as the leader.", node.getNodeId(), leaderId);
        waitingForElected = false;
        cancelElectedTimer();
    }

    /**
     * Declares a node as the leader and notifies all neighbors.
     *
     * @param newLeader Address of the node to declare as leader.
     */
    public synchronized void declareLeader(Address newLeader) {
        boolean shouldDeclare = false;

        if (node.getAddress().equals(newLeader)) {
            shouldDeclare = true;
            log.info("Node {} recognizes itself as the new leader.", node.getNodeId());
        } else {
            Address currentLeader = node.getNeighbours().getLeaderNode();
            if (currentLeader == null || newLeader.getNodeID() > currentLeader.getNodeID()) {
                shouldDeclare = true;
                log.info("Node {} accepts Node {} as the new leader.", node.getNodeId(), newLeader.getNodeID());
            }
        }

        if (shouldDeclare) {
            node.setCoordinator(node.getAddress().equals(newLeader));
            node.getNeighbours().setLeaderNode(newLeader);

            log.info("Node {} is declaring Node {} as the leader.", node.getNodeId(), newLeader.getNodeID());

            for (Address neighbour : node.getNeighbours().getNeighbours()) {
                try {
                    NodeCommands proxy = node.getCommHub().getRMIProxy(neighbour);
                    proxy.notifyAboutNewLeader(newLeader);
                    log.info("Notified Node {} about the new leader Node {}.", neighbour.getNodeID(), newLeader.getNodeID());
                } catch (RemoteException e) {
                    log.error("Failed to notify Node {} about the new leader: {}", neighbour.getNodeID(), e.getMessage());
                }
            }

            cancelAllTimers();
            node.setVoting(false);
        } else {
            log.info("Attempted to declare leader Node {}, but leader is already set to Node {}.",
                    newLeader.getNodeID(),
                    node.getNeighbours().getLeaderNode().getNodeID());
        }
    }

    /**
     * Starts the election timeout timer.
     */
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

    /**
     * Starts the elected timeout timer.
     */
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

    /**
     * Cancels the election timer if it's running.
     */
    private void cancelElectionTimer() {
        if (electionTimer != null) {
            electionTimer.cancel();
            electionTimer = null;
            log.info("Election timer cancelled for Node {}.", node.getNodeId());
        }
    }

    /**
     * Cancels the elected timer if it's running.
     */
    private void cancelElectedTimer() {
        if (electedTimer != null) {
            electedTimer.cancel();
            electedTimer = null;
            log.info("Elected timer cancelled for Node {}.", node.getNodeId());
        }
    }

    /**
     * Cancels all active timers.
     */
    private void cancelAllTimers() {
        cancelElectionTimer();
        cancelElectedTimer();
    }
}
