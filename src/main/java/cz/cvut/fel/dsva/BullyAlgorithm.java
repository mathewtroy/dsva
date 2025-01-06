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
    private final long electionTimeout = 5000;  // For waiting OK from higher ID
    private final long electedTimeout = 5000;   // For waiting Coordinator/Elected
    private boolean waitingForOK = false;
    private boolean waitingForElected = false;
    private Timer electionTimer;
    private Timer electedTimer;

    public BullyAlgorithm(Node node) {
        this.node = node;
    }

    public synchronized void startElection() {
        // If already voting, do nothing
        if (node.isVoting()) {
            log.info(GREEN + "Node {} is already participating in an election.", node.getNodeId());
            return;
        }

        // Mark node as voting
        node.setVoting(true);
        node.setReceivedOK(false);
        log.info(GREEN + "Node {} initiates an election.", node.getNodeId());
        System.out.println("Election process started by node: " + node.getNodeId());

        boolean higherNodeExists = false;
        // Send Election message to all neighbors with a higher ID
        for (Address neighbour : node.getNeighbours().getNeighbours()) {
            if (neighbour.getNodeID() > node.getNodeId()) {
                higherNodeExists = true;
                node.getCommHub().sendElectionMessage(neighbour, node.getNodeId());
            }
        }

        // If no higher ID node, declare self as leader
        if (!higherNodeExists) {
            declareLeader(node.getAddress());
        } else {
            // Otherwise, wait for OK from higher node(s)
            waitingForOK = true;
            startElectionTimer();
        }
    }

    // Called when a node with lower ID starts an election
    public synchronized void onElectionMsgFromLower(long senderId) {
        System.out.println("Election message received from Node: " + senderId);
        if (node.getNodeId() > senderId) {
            System.out.println("Node " + node.getNodeId() + " responds OK to " + senderId);
            node.getCommHub().sendOKMessage(senderId);

            // If we are not currently voting, start our own election
            if (!node.isVoting()) {
                startElection();
            }
        }
    }

    // Called when this node receives an OK from a higher node
    public synchronized void onOKReceived(long fromId) {
        node.setReceivedOK(true);
        waitingForOK = false;
        waitingForElected = true;  // we expect a Coordinator/Elected message
        log.info("Node {} received OK from Node {}.", node.getNodeId(), fromId);
        System.out.println("Node " + node.getNodeId() + " received OK from Node " + fromId);
        cancelElectionTimer();     // no need to wait for electionTimeout
        startElectedTimer();       // now wait for the Elected message
    }

    // Called when a node receives an Elected/Coordinator message
    public synchronized void onElectedReceived(long leaderId, Address leaderAddr) {
        System.out.println("Leader elected: Node " + leaderId);
        node.getNeighbours().setLeaderNode(leaderAddr);

        // Stop voting - we have a leader
        node.setVoting(false);

        // Set coordinator flag if this node is the leader
        node.setCoordinator(node.getNodeId() == leaderId);
        log.info(GREEN + "Node {} acknowledges Node {} as leader.", node.getNodeId(), leaderId);
        System.out.println("Node " + node.getNodeId() + " acknowledges Node " + leaderId + " as leader.");

        waitingForElected = false;
        cancelElectedTimer();
    }

    // If no OK arrived in electionTimeout, declare self leader
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

    // If we got OK, we wait for a coordinator/elected message
    // If it doesn't arrive by electedTimeout, also declare self leader
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

    private void declareLeader(Address newLeader) {
        boolean shouldDeclare = false;

        if (node.getAddress().equals(newLeader)) {
            // Declaring itself as leader
            shouldDeclare = true;
        } else {
            // Accepting a new leader only if no leader is set
            if (!node.isCoordinator() && node.getNeighbours().getLeaderNode() == null) {
                shouldDeclare = true;
            }
        }

        if (shouldDeclare) {
            node.setCoordinator(node.getAddress().equals(newLeader));
            node.getNeighbours().setLeaderNode(newLeader);

            node.getNeighbours().getNeighbours().forEach(neighbour -> {
                try {
                    NodeCommands proxy = node.getCommHub().getRMIProxy(neighbour);
                    proxy.notifyAboutNewLeader(newLeader);
                    log.info(GREEN + "Notified Node {} about new leader Node {}.", neighbour.getNodeID(), newLeader.getNodeID());
                } catch (RemoteException e) {
                    log.error(RED + "Failed to notify Node {} about new leader: {}", neighbour, e.getMessage());
                }
            });

            log.info(GREEN + "Leader declared: Node {}.", newLeader.getNodeID());
            System.out.println("Leader declared: Node " + newLeader.getNodeID());

            cancelAllTimers();
            node.setVoting(false);
        } else {
            log.info("Attempted to declare leader Node {}, but leader is already set to Node {}.",
                    newLeader.getNodeID(),
                    node.getNeighbours().getLeaderNode().getNodeID());
            System.out.println("Ignoring declareLeader for Node " + newLeader.getNodeID() +
                    ", because leader is already set to Node " + node.getNeighbours().getLeaderNode().getNodeID());
        }
    }


}
