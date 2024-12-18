package cz.cvut.fel.dsva;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

@Slf4j
@Getter
@Setter
public class ConsoleHandler implements Runnable {
    private boolean reading = true;
    private BufferedReader reader;
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private final Node myNode;

    public ConsoleHandler(Node myNode) {
        this.myNode = myNode;
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    private void parseCommandLine(String commandline) {
        switch (commandline) {
            case "e":
            case "start_election":
                System.out.println("Starting Bully Election Algorithm...");
                myNode.startElection();
                break;

            case "checkLeader":
            case "cl":
                System.out.println("Checking leader status...");
                myNode.checkLeader();
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
                System.out.println("Exiting...");
                reading = false;
                break;

            default:
                System.out.println("Unrecognized command. Type 'help' or '?' for a list of commands.");
        }
    }


    private void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("e or start_election - Start Bully Election");
        System.out.println("cl or checkLeader   - Check status of the current leader");
        System.out.println("sm or sendMsg       - Send a message to another node");
        System.out.println("s or status         - Print the current status of the node");
        System.out.println("q or quit           - Exit the console");
        System.out.println("? or help           - Display this help message");

    }


    private void sendMessageToNode() {
        try {
            System.out.print("Enter recipient Node ID: ");
            String receiverIdInput = reader.readLine();
            int receiverId = Integer.parseInt(receiverIdInput);
            System.out.print("Enter message content: ");
            String messageContent = reader.readLine();
            myNode.sendMessageToNode(receiverId, messageContent);
            System.out.println("Message sent to Node " + receiverId);
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error while sending message. Please try again.");
        }
    }

    @Override
    public void run() {
        String commandline;
        while (reading) {
            System.out.print("\ncmd > ");
            try {
                commandline = reader.readLine();
                if (commandline != null) {
                    parseCommandLine(commandline.trim());
                }
            } catch (IOException e) {
                err.println("ConsoleHandler - Error reading console input.");
                e.printStackTrace();
                reading = false;
            }
        }
        System.out.println("Closing ConsoleHandler.");
    }
}
