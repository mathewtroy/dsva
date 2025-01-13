package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import io.javalin.Javalin;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the REST API for managing and interacting with nodes in a distributed system.
 * Provides endpoints for election, messaging, RMI management, and node status operations.
 */
@Slf4j
@Getter
@Setter
public class APIHandler {

    private final int port;    // Port on which the API server runs
    private final Node myNode; // Reference to the current node
    private Javalin app;       // Javalin instance for managing API endpoints

    /**
     * Constructs an APIHandler with a specified node and port.
     *
     * @param myNode The current node.
     * @param port   The port for the API server.
     */
    public APIHandler(Node myNode, int port) {
        this.myNode = myNode;
        this.port = port;
    }

    /**
     * Constructs an APIHandler with a specified node and a default port (7000).
     *
     * @param myNode The current node.
     */
    public APIHandler(Node myNode) {
        this(myNode, 7000);
    }

    /**
     * Starts the REST API server and defines its endpoints.
     */
    public void start() {
        this.app = Javalin.create()

                // Start election
                .get("/start_election", ctx -> {
                    log.info("Starting election via API.");
                    myNode.startElection();
                    ctx.result("Election started\n");
                })

                // Check leader status
                .get("/check_leader", ctx -> {
                    log.info("Checking leader status via API.");
                    myNode.checkLeader();
                    ctx.result("Leader status checked\n");
                })

                // Send a message to another node
                .get("/send_message/{node_id}/{message}", ctx -> {
                    long recipientId = Long.parseLong(ctx.pathParam("node_id"));
                    String message = ctx.pathParam("message");
                    log.info("Sending message to Node {}: {}", recipientId, message);
                    myNode.sendMessageToNode(recipientId, message);
                    ctx.result("Message sent to Node " + recipientId + ": " + message + "\n");
                })

                // Start RMI service
                .get("/start_rmi", ctx -> {
                    log.info("Starting RMI service via API.");
                    myNode.startRMI();
                    ctx.result("RMI started\n");
                })

                // Stop RMI service
                .get("/stop_rmi", ctx -> {
                    log.info("Stopping RMI service via API.");
                    myNode.stopRMI();
                    myNode.resetTopology();
                    ctx.result("RMI stopped and topology reset\n");
                })

                // Kill the node
                .get("/kill", ctx -> {
                    log.info("Killing node via API.");
                    myNode.kill();
                    ctx.result("Node killed\n");
                })

                // Leave the network gracefully
                .get("/leave", ctx -> {
                    log.info("Leaving network gracefully via API.");
                    myNode.leave();
                    ctx.result("Node left the network gracefully\n");
                })

                // Revive the node
                .get("/revive", ctx -> {
                    log.info("Reviving node via API.");
                    myNode.revive();
                    ctx.result("Node revived successfully\n");
                })

                // Get the status of the node
                .get("/get_status", ctx -> {
                    log.info("Getting node status via API.");
                    myNode.printStatus();
                    ctx.result(myNode.getStatus() + "\n");
                })

                // Join another node in the network
                .get("/join/{other_node_ip}/{other_node_port}", ctx -> {
                    String otherNodeIP = ctx.pathParam("other_node_ip");
                    int otherNodePort = Integer.parseInt(ctx.pathParam("other_node_port"));
                    log.info("Joining node: {}:{} via API.", otherNodeIP, otherNodePort);
                    myNode.join(otherNodeIP, otherNodePort);
                    ctx.result("Tried to join Node " + otherNodeIP + ":" + otherNodePort + "\n");
                })

                // Start the API server
                .start(this.port);
    }
}
