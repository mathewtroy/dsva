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
                // Join a new node by its IP and port
                .get("/join/{node_id}/{node_ip}/{node_port}", ctx -> {
                    long nodeId = Long.parseLong(ctx.pathParam("node_id"));
                    String nodeIp = ctx.pathParam("node_ip");
                    int nodePort = Integer.parseInt(ctx.pathParam("node_port"));
                    Address newNode = new Address(nodeIp, nodePort, nodeId);

                    System.out.println("Joining new node: " + newNode);
                    myNode.join(newNode);
                    ctx.result("Node " + newNode + " successfully joined the network.");
                })
                // Start Bully election
                .get("/start_election", ctx -> {
                    System.out.println("Starting Bully Algorithm Election...");
                    myNode.startElection();
                    ctx.result("Bully election started.");
                })
                // Check leader status
                .get("/check_leader", ctx -> {
                    System.out.println("Checking leader status...");
                    myNode.checkLeader();
                    ctx.result("Current leader: " + (myNode.getNeighbours().getLeaderNode() != null ?
                            myNode.getNeighbours().getLeaderNode().toString() :
                            "No leader"));
                })
                // Send a message to the leader
                .get("/send_message_leader/{message}", ctx -> {
                    String messageContent = ctx.pathParam("message");
                    Address leader = myNode.getNeighbours().getLeaderNode();
                    if (leader != null) {
                        System.out.println("Sending message to leader " + leader + ": " + messageContent);
                        myNode.sendMessageToNode(leader.getNodeID(), messageContent);
                        ctx.result("Message sent to leader: " + leader);
                    } else {
                        ctx.result("No leader is currently present.");
                    }
                })
                // Send a message to a specific node by ID
                .get("/send_message/{receiver_id}/{message}", ctx -> {
                    long receiverId = Long.parseLong(ctx.pathParam("receiver_id"));
                    String messageContent = ctx.pathParam("message");
                    System.out.println("Sending message to Node " + receiverId + ": " + messageContent);
                    myNode.sendMessageToNode(receiverId, messageContent);
                    ctx.result("Message sent to Node " + receiverId);
                })
                // Get the status of the node
                .get("/get_status", ctx -> {
                    System.out.println("Getting current node status...");
                    ctx.result(myNode.getStatus());
                })
                // Leave the network
                .get("/leave_network", ctx -> {
                    System.out.println("Node is leaving the network...");
                    myNode.leaveNetwork();
                    ctx.result("Node has left the network.");
                })
                // Start the RMI component
                .get("/start_rmi", ctx -> {
                    System.out.println("Starting RMI part.");
                    myNode.startRMI();
                    ctx.result("RMI started.");
                })
                // Stop the RMI component
                .get("/stop_rmi", ctx -> {
                    System.out.println("Stopping RMI part.");
                    myNode.stopRMI();
                    myNode.resetTopology();
                    ctx.result("RMI stopped and topology reset.");
                })
                // Notify all nodes of a new leader
                .get("/notify_new_leader", ctx -> {
                    Address leader = myNode.getNeighbours().getLeaderNode();
                    if (leader != null) {
                        System.out.println("Notifying all nodes about new leader: " + leader);
                        myNode.notifyAllNodesAboutNewLeader(leader);
                        ctx.result("All nodes notified about new leader: " + leader);
                    } else {
                        ctx.result("No leader to notify about.");
                    }
                })
                // Kill the node
                .get("/kill", ctx -> {
                    myNode.kill();
                    ctx.result("Node killed (no proper logout).");
                })
                // Revive the node
                .get("/revive", ctx -> {
                    myNode.revive();
                    ctx.result("Node revived.");
                })
                // Set message delay for simulation purposes
                .get("/set_delay/{delay}", ctx -> {
                    long delay = Long.parseLong(ctx.pathParam("delay"));
                    myNode.setDelay(delay);
                    ctx.result("Delay set to " + delay + " ms.");
                })
                .start(this.port);

        System.out.println("API started on port: " + port);
    }
}
