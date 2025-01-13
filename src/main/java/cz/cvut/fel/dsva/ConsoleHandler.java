package cz.cvut.fel.dsva;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Handles user commands entered through the console for interacting with a node in a distributed system.
 * Supports operations like starting elections, checking leader status, sending messages, and more.
 */
@Slf4j
@Getter
@Setter
public class ConsoleHandler implements Runnable {
    private volatile boolean reading = true; // Flag for console input loop
    private BufferedReader reader;          // Reader for console input
    private final Node myNode;              // Reference to the current node

    /**
     * Constructs a ConsoleHandler for managing console interactions with the given node.
     *
     * @param myNode The node managed by this ConsoleHandler.
     */
    public ConsoleHandler(Node myNode) {
        this.myNode = myNode;
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    /**
     * Parses a command entered in the console and executes the corresponding action.
     *
     * @param commandline The command entered by the user.
     */
    private void parseCommandLine(String commandline) {
        switch (commandline) {
            case "e":
            case "startElection":
                log.info("Starting Bully Election Algorithm...");
                myNode.startElection();
                break;

            case "checkLeader":
            case "cl":
                log.info("Checking leader status...");
                myNode.checkLeader();
                break;

            case "killNode":
            case "k":
                log.info("Killing this node...");
                myNode.kill();
                break;

            case "leaveNode":
            case "l":
                log.info("Leaving the network...");
                myNode.leave();
                break;

            case "reviveNode":
            case "r":
                log.info("Reviving this node...");
                myNode.revive();
                break;

            case "sendMsg":
            case "sm":
                sendMessageToNode();
                break;

            case "status":
            case "s":
                myNode.printStatus();
                break;

            case "help":
            case "?":
                printHelp();
                break;

            case "quit":
            case "q":
                log.info("Exiting...");
                reading = false;
                break;

            default:
                log.warn("Unrecognized command. Type 'help' or '?' for a list of commands.");
        }
    }

    /**
     * Displays a list of available commands to the user.
     */
    private void printHelp() {
        log.info("\nAvailable Commands:");
        log.info("e or startElection  - Start Bully Election");
        log.info("cl or checkLeader   - Check status of the current leader");
        log.info("k or killNode       - Kill the node");
        log.info("l or leaveNode      - Leave the network gracefully");
        log.info("r or reviveNode     - Revive the node");
        log.info("sm or sendMsg       - Send a message to another node");
        log.info("s or status         - Print the current status of the node");
        log.info("q or quit           - Exit the console");
        log.info("? or help           - Display this help message");
    }

    /**
     * Prompts the user to input a recipient node ID and message content,
     * then sends the message to the specified node.
     */
    private void sendMessageToNode() {
        try {
            System.out.println("Enter recipient Node ID: ");
            String receiverIdInput = reader.readLine();
            long receiverId = Long.parseLong(receiverIdInput);
            System.out.println("Enter message content: ");
            String messageContent = reader.readLine();
            boolean success = myNode.sendMessageToNode(receiverId, messageContent);
            if (success) {
                log.info("Message successfully sent to Node {}", receiverId);
            } else {
                log.warn("Failed to send message to Node {}", receiverId);
            }
        } catch (IOException | NumberFormatException e) {
            log.error("Error while sending message. Please try again.", e);
        }
    }

    /**
     * Restarts the console input loop if the node is active.
     */
    public void restartConsole() {
        if (myNode.isKilled() || myNode.isLeft()) {
            log.info("Node is inactive. Console will not be restarted.");
            return;
        }
        reading = true;
        new Thread(this).start();
        log.info("ConsoleHandler restarted.");
    }

    /**
     * Continuously reads and processes user input from the console until the loop is terminated.
     */
    @Override
    public void run() {
        while (reading) {
            try {
                System.out.print("\ncmd > ");
                String commandline = reader.readLine();
                if (commandline != null) {
                    parseCommandLine(commandline);
                }
            } catch (IOException e) {
                log.error("ConsoleHandler - Error reading console input.", e);
                reading = false;
                restartConsole(); // Automatically restart console on error
            } catch (Exception ex) {
                log.error("Unexpected error: {}", ex.getMessage(), ex);
                reading = false;
                restartConsole(); // Automatically restart console on unexpected error
            }
        }
        log.info("Closing ConsoleHandler.");
    }
}
