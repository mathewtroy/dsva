package cz.cvut.fel.dsva;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
@Getter
@Setter
public class ConsoleHandler implements Runnable {
    private boolean reading = true;
    private final Node myNode;
    private final BufferedReader reader;

    public ConsoleHandler(Node myNode) {
        this.myNode = myNode;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    private void parseCommand(String commandline) {
        String[] parts = commandline.trim().split("\\s+");
        if (parts.length == 0) return;

        String command = parts[0].toLowerCase();

        switch (command) {
            case "join":
                if (parts.length != 3) {
                    System.out.println("Usage: join <ip> <port>");
                } else {
                    String ip = parts[1];
                    int port;
                    try {
                        port = Integer.parseInt(parts[2]);
                        myNode.join(ip, port);
                        System.out.println("Join command executed: " + ip + ":" + port);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid port number: " + parts[2]);
                    }
                }
                break;
            case "startelection":
            case "se":
                myNode.startElection();
                System.out.println("Start election command executed.");
                break;
            case "checkleader":
            case "cl":
                myNode.checkLeader();
                break;
            case "sendmessage":
            case "sm":
                if (parts.length < 3) {
                    System.out.println("Usage: sendMessage <toNick> <message>");
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
            case "revive":
            case "r":
                myNode.reviveNode();
                break;
            case "kill":
            case "k":
                myNode.killNetwork();
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
                System.out.println("Unrecognized command. Type '?' or 'help' for assistance.");
        }
    }

    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("join <ip> <port>            - Join another node");
        System.out.println("startElection (se)          - Start leader election");
        System.out.println("checkLeader (cl)            - Check current leader");
        System.out.println("sendMessage (sm) <to> <msg> - Send a message to a user");
        System.out.println("leave (l)                   - Leave the network gracefully");
        System.out.println("revive (r)                  - Revive a previously left node");
        System.out.println("kill (k)                    - Abruptly kill this node (no graceful leave)");
        System.out.println("status (s)                  - Show node status");
        System.out.println("? / help                    - Show this help message");
    }

    @Override
    public void run() {
        System.out.println("ConsoleHandler started. Type '?' or 'help' for commands.");
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
                System.out.println("ConsoleHandler - error reading input.");
                e.printStackTrace();
                reading = false;
            }
        }
        System.out.println("ConsoleHandler stopped.");
    }
}
