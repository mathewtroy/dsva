package cz.cvut.fel.dsva;

import io.javalin.Javalin;

public class APIHandler {

    private int port = 7000;
    private Node myNode = null;
    private Javalin app = null;


    public APIHandler(Node myNode, int port) {
        this.myNode = myNode;
        this.port = port;
    }

    public APIHandler(Node myNode) {
        this(myNode, 7000);
    }


    public void start() {
        this.app = Javalin.create()
                .get("/join/{other_node_ip}/{other_node_port}", ctx -> {
                    System.out.println("Joining node: " + ctx.pathParam("other_node_ip") + ":" + ctx.pathParam("other_node_port"));
                    myNode.join(ctx.pathParam("other_node_ip"), Integer.parseInt(ctx.pathParam("other_node_port")));
                    ctx.result("Tried to join to: " + ctx.pathParam("other_node_ip") + " " + ctx.pathParam("other_node_port") + "\n");
                })
                .get("/send_hello_next", ctx -> {
                    System.out.println("Sending hello to next node");
                    myNode.sendHelloToNext();
                    ctx.result("Hello sent\n");
                })
                .get("/send_hello_leader", ctx -> {
                    System.out.println("Sending hello to leader");
                    myNode.sendHelloToLeader();
                    ctx.result("Hello sent\n");
                })
                .get("/get_status", ctx -> {
                    System.out.println("Getting status of this node");
                    myNode.printStatus();
                    ctx.result(myNode.getStatus() + "\n");
//                    ctx.json(myNode);
                })
                .get("/start_rmi", ctx -> {
                    System.out.println("Starting RMI part.");
                    myNode.startRMI();
                    ctx.result("RMI started\n");
                })
                .get("/stop_rmi", ctx -> {
                    System.out.println("Stopping RMI part.");
                    myNode.stopRMI();
                    myNode.resetTopology();
                    ctx.result("RMI stopped + topology info reset\n");
                })
                // try to add command for starting election
                .start(this.port);
    }
}

