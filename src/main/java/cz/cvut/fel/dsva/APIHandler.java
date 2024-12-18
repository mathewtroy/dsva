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

    private int port = 7000;
    private Node myNode;
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
                .get("/join/{node_id}/{node_ip}/{node_port}", ctx -> {
                    long nodeId = Long.parseLong(ctx.pathParam("node_id"));
                    String nodeIp = ctx.pathParam("node_ip");
                    int nodePort = Integer.parseInt(ctx.pathParam("node_port"));
                    Address newNode = new Address(nodeIp, nodePort, nodeId);

                    System.out.println("Joining new node: " + newNode);
                    myNode.join(newNode);
                    ctx.result("Node " + newNode + " successfully joined the network.");
                })
                .get("/start_election", ctx -> {
                    System.out.println("Starting Bully Algorithm Election...");
                    myNode.startElection();
                    ctx.result("Bully election started.");
                })
                .get("/check_leader", ctx -> {
                    System.out.println("Checking leader status...");
                    myNode.checkLeader();
                    ctx.result("Current leader: " + (myNode.getNeighbours().getLeaderNode() != null ?
                            myNode.getNeighbours().getLeaderNode().toString() :
                            "No leader"));
                })
                .get("/send_message/{receiver_id}/{message}", ctx -> {
                    long receiverId = Long.parseLong(ctx.pathParam("receiver_id"));
                    String messageContent = ctx.pathParam("message");
                    System.out.println("Sending message to Node " + receiverId + ": " + messageContent);
                    myNode.sendMessageToNode(receiverId, messageContent);
                    ctx.result("Message sent to Node " + receiverId);
                })
                .get("/get_status", ctx -> {
                    System.out.println("Getting current node status...");
                    ctx.result(myNode.getStatus());
                })
                .get("/leave_network", ctx -> {
                    System.out.println("Node is leaving the network...");
                    myNode.leaveNetwork();
                    ctx.result("Node has left the network.");
                })
                .get("/kill", ctx -> {
                    myNode.kill();
                    ctx.result("Node killed (no proper logout).");
                })
                .get("/revive", ctx -> {
                    myNode.revive();
                    ctx.result("Node revived.");
                })
                .get("/set_delay/{delay}", ctx -> {
                    long delay = Long.parseLong(ctx.pathParam("delay"));
                    myNode.setDelay(delay);
                    ctx.result("Delay set to " + delay + " ms.");
                })
                .start(this.port);

        System.out.println("API started on port: " + port);
    }
}
