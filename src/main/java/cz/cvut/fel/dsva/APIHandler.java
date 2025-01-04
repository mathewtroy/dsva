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
                    System.out.println("Starting RMI part via API.");
                    myNode.startRMI();
                    ctx.result("RMI started\n");
                })

                // Stop RMI
                .get("/stop_rmi", ctx -> {
                    System.out.println("Stopping RMI part via API.");
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

                // Revive node
                .get("/revive", ctx -> {
                    System.out.println("Reviving node via API.");
                    myNode.revive();
                    ctx.result("Node revived\n");
                })

                // Get status of the node
                .get("/status", ctx -> {
                    System.out.println("Getting node status via API.");
                    ctx.result(myNode.getStatus() + "\n");
                })

                // Start the API server
                .start(this.port);
    }
}
