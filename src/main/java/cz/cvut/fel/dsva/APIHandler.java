package cz.cvut.fel.dsva;

import io.javalin.Javalin;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static io.javalin.apibuilder.ApiBuilder.*;

@Slf4j
@Getter
@Setter
public class APIHandler implements Runnable {
    private int port = 7000;
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
        app = Javalin.create().routes(() -> {
            path("/join", () -> {
                get("/{other_node_ip}/{other_node_port}", ctx -> {
                    String ip = ctx.pathParam("other_node_ip");
                    int port = Integer.parseInt(ctx.pathParam("other_node_port"));
                    System.out.println("API: Joining node at " + ip + ":" + port);
                    myNode.join(ip, port);
                    ctx.result("Attempted to join node at " + ip + ":" + port + "\n");
                });
            });
            path("/start_election", () -> {
                get("", ctx -> {
                    System.out.println("API: Start Election request");
                    myNode.startElection();
                    ctx.result("Election started\n");
                });
            });
            path("/check_leader", () -> {
                get("", ctx -> {
                    System.out.println("API: Check Leader request");
                    myNode.checkLeader();
                    ctx.result("Current leader: " + myNode.getNeighbours().getLeader() + "\n");
                });
            });
            path("/send_message", () -> {
                post("", ctx -> {
                    String toNick = ctx.formParam("toNick");
                    String message = ctx.formParam("message");
                    System.out.println("API: Send Message to " + toNick + ": " + message);
                    myNode.sendMessage(toNick, message);
                    ctx.result("Message sent to " + toNick + "\n");
                });
            });
            path("/leave", () -> {
                get("", ctx -> {
                    System.out.println("API: Leave network request");
                    myNode.leaveNetwork();
                    ctx.result("Node has left the network\n");
                });
            });
            path("/revive", () -> {
                get("", ctx -> {
                    System.out.println("API: Revive node request");
                    myNode.reviveNode();
                    ctx.result("Node has been revived\n");
                });
            });
            path("/get_status", () -> {
                get("", ctx -> {
                    System.out.println("API: Get Status request");
                    ctx.result(myNode.getStatus() + "\n");
                });
            });
            path("/stop_rmi", () -> {
                get("", ctx -> {
                    System.out.println("API: Stop RMI request");
                    myNode.stopRMI();
                    ctx.result("RMI stopped\n");
                });
            });
            path("/start_rmi", () -> {
                get("", ctx -> {
                    System.out.println("API: Start RMI request");
                    myNode.startRMI();
                    ctx.result("RMI started\n");
                });
            });
        }).start(port);
        System.out.println("API started on port " + port);
    }

    @Override
    public void run() {
        start();
    }
}
