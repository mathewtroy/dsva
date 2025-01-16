package cz.cvut.fel.dsva;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Handles console input for the Node, allowing users to execute various commands
 * to control the node's behavior, such as joining the network, starting elections,
 * sending messages, and managing the node's state.
 *
 * <p>This class implements the {@link Runnable} interface and runs in a separate thread
 * to continuously listen for and process user commands from the console.
 *
 * <p>Supported commands include:
 * <ul>
 *     <li><b>join</b>: Join another node in the network.</li>
 *     <li><b>start_election (se)</b>: Initiate a leader election.</li>
 *     <li><b>check_leader (cl)</b>: Check the current leader of the network.</li>
 *     <li><b>send_message (sm)</b>: Send a message to another node.</li>
 *     <li><b>leave (l)</b>: Leave the network gracefully.</li>
 *     <li><b>kill (k)</b>: Simulate an abrupt node crash.</li>
 *     <li><b>revive (r)</b>: Revive a previously killed node.</li>
 *     <li><b>status (s)</b>: Display the current status of the node.</li>
 *     <li><b>? / help</b>: Display help information.</li>
 * </ul>
 *
 * @author @author Kross Aleksandr
 */
@Slf4j
@Getter
@Setter
public class ConsoleHandler implements Runnable {
    private boolean reading = true;
    private final Node myNode;
    private final BufferedReader reader;

    /**
     * Constructs a ConsoleHandler associated with the specified node.
     *
     * @param myNode The parent Node instance to control.
     */
    public ConsoleHandler(Node myNode) {
        this.myNode = myNode;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    /**
     * Parses and executes a given command line input.
     *
     * @param commandline The command line input entered by the user.
     */
    private void parseCommand(String commandline) {
        String[] parts = commandline.trim().split("\\s+");
        if (parts.length == 0) return;

        String command = parts[0].toLowerCase();

        switch (command) {
            case "join":
                if (parts.length != 3) {
                    log.info("Usage: join <ip> <port>");
                } else {
                    String ip = parts[1];
                    try {
                        int port = Integer.parseInt(parts[2]);
                        myNode.join(ip, port);
                        log.info("Join command executed: {}:{}", ip, port);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid port number: {}", parts[2]);
                    }
                }
                break;
            case "start_election":
            case "se":
                myNode.startElection();
                log.info("Start election command executed.");
                break;
            case "check_leader":
            case "cl":
                myNode.checkLeader();
                break;
            case "send_message":
            case "sm":
                if (parts.length < 3) {
                    log.info("Usage: send_message <toNick> <message>");
                } else {
                    String toNick = parts[1];
                    String message = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length));
                    myNode.sendMessage(toNick, message);
                }
                break;
            case "leave":
            case "l":
                myNode.leaveNetwork();
                break;
            case "kill":
            case "k":
                myNode.killNode();
                break;
            case "revive":
            case "r":
                myNode.reviveNode();
                break;
            case "status":
            case "s":
                myNode.printStatus();
                break;
            case "?":
            case "help":
                printHelp();
                break;
            default:
                log.info("Unrecognized command. Type '?' or 'help' for assistance.");
        }
    }

    /**
     * Displays a list of available commands and their usage.
     */
    private void printHelp() {
        log.info("Available commands:");
        log.info("join <ip> <port>                     - Join another node");
        log.info("start_election (se)                  - Start leader election");
        log.info("check_leader (cl)                    - Check current leader");
        log.info("send_message (sm) <nick> <message>    - Send a message to a user");
        log.info("leave (l)                            - Leave the network gracefully");
        log.info("kill (k)                             - Simulate a node crash (killed)");
        log.info("revive (r)                           - Revive a previously killed node");
        log.info("status (s)                           - Show node status");
        log.info("? / help                             - Show this help message");
    }

    /**
     * The main execution loop for the ConsoleHandler.
     *
     * <p>Continuously reads input from the console, parses commands, and executes them
     * until the reading flag is set to false or an error occurs.
     */
    @Override
    public void run() {
        log.info("ConsoleHandler started. Type '?' or 'help' for commands.");
        while (reading) {
            System.out.print("\ncmd > ");
            try {
                String commandline = reader.readLine();
                if (commandline == null) {
                    reading = false;
                    break;
                }
                parseCommand(commandline);
            } catch (IOException e) {
                log.error("ConsoleHandler - error reading input.", e);
                reading = false;
            }
        }
        log.info("ConsoleHandler stopped.");
    }
}