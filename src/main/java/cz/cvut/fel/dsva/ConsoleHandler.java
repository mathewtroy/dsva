package cz.cvut.fel.dsva;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class ConsoleHandler implements Runnable {

    private boolean reading = true;
    private BufferedReader reader = null;
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private Node myNode;


    public ConsoleHandler(Node myNode) {
        this.myNode = myNode;
        reader = new BufferedReader(new InputStreamReader(System.in));
    }


    private void parse_commandline(String commandline) {
        if (commandline.equals("hn")) {
            myNode.sendHelloToNext();
        } else if (commandline.equals("hl")) {
            myNode.sendHelloToLeader();
        } else if (commandline.equals("s")) {
            myNode.printStatus();
        } else if (commandline.equals("e")) {
            myNode.startElection();
        } else if (commandline.equals("?")) {
            System.out.print("?  - this help");
            System.out.print("hn - send Hello message to Next neighbour");
            System.out.print("hl - send Hello message to Leader");
            System.out.print("e  - start election");
            System.out.print("s  - print node status");
        } else {
            // do nothing
            System.out.print("Unrecognized command.");
        }
    }


    @Override
    public void run() {
        String commandline = "";
        while (reading == true) {
            commandline = "";
            System.out.print("\ncmd > ");
            try {
                commandline = reader.readLine();
                parse_commandline(commandline);
            } catch (IOException e) {
                err.println("ConsoleHandler - error in rading console input.");
                e.printStackTrace();
                reading = false;
            }
        }
        System.out.println("Closing ConsoleHandler.");
    }
}