package cz.cvut.fel.dsva;

import cz.cvut.fel.dsva.base.Address;
import io.javalin.Javalin;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class APIHandler {

    private final int port;
    private final Node myNode;
    private Javalin app;

    public APIHandler(Node myNode, int port) {
        this.myNode = myNode;
        this.port = port;
    }

    public APIHandler(Node myNode) {
        this(myNode, 7000);
    }

    /**
     * Starts the REST API server with defined endpoints.
     */
    public void start() {
        this.app = Javalin.create()
                // Start election
                .get("/start_election", ctx -> {
                    System.out.println("Starting election via API.");
                    myNode.startElection();
                    ctx.result("Election started\n");
                })

                // Check leader
                .get("/check_leader", ctx -> {
                    System.out.println("Checking leader status via API.");
                    myNode.checkLeader();
                    ctx.result("Leader status checked\n");
                })

                // Send message to another node
                .get("/send_message/{node_id}/{message}", ctx -> {
                    long recipientId = Long.parseLong(ctx.pathParam("node_id"));
                    String message = ctx.pathParam("message");
                    System.out.println("Sending message to Node " + recipientId + ": " + message);
                    myNode.sendMessageToNode(recipientId, message);
                    ctx.result("Message sent to Node " + recipientId + ": " + message + "\n");
                })

                // Start RMI
                .get("/start_rmi", ctx -> {
                    System.out.println("Starting RMI service via API.");
                    myNode.startRMI();
                    ctx.result("RMI started\n");
                })

                // Stop RMI
                .get("/stop_rmi", ctx -> {
                    System.out.println("Stopping RMI service via API.");
                    myNode.stopRMI();
                    myNode.resetTopology();
                    ctx.result("RMI stopped and topology reset\n");
                })

                // Kill node
                .get("/kill", ctx -> {
                    System.out.println("Killing node via API.");
                    myNode.kill();
                    ctx.result("Node killed\n");
                })

                // Leave network gracefully
                .get("/leave", ctx -> {
                    System.out.println("Leaving network gracefully via API.");
                    myNode.leave();
                    ctx.result("Node left the network gracefully\n");
                })

                // Revive node by providing a node to join
                .get("/revive/{join_node_ip}/{join_node_port}", ctx -> {
                    String joinNodeIP = ctx.pathParam("join_node_ip");
                    int joinNodePort = Integer.parseInt(ctx.pathParam("join_node_port"));
                    System.out.println("Reviving node and joining to Node " + joinNodeIP + ":" + joinNodePort + " via API.");
                    myNode.revive(joinNodeIP, joinNodePort);
                    ctx.result("Node revived and attempted to join Node " + joinNodeIP + ":" + joinNodePort + "\n");
                })

                // Get status of the node
                .get("/get_status", ctx -> {
                    System.out.println("Getting node status via API.");
                    myNode.printStatus();
                    ctx.result(myNode.getStatus() + "\n");
                })

                // Join another node
                .get("/join/{other_node_ip}/{other_node_port}", ctx -> {
                    String otherNodeIP = ctx.pathParam("other_node_ip");
                    int otherNodePort = Integer.parseInt(ctx.pathParam("other_node_port"));
                    System.out.println("Joining node: " + otherNodeIP + ":" + otherNodePort + " via API.");
                    myNode.join(otherNodeIP, otherNodePort);
                    ctx.result("Tried to join Node " + otherNodeIP + ":" + otherNodePort + "\n");
                })

                // Start the API server
                .start(this.port);
    }
}
