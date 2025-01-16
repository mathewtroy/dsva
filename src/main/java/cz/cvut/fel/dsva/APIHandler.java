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
                    int portNumber = Integer.parseInt(ctx.pathParam("other_node_port"));
                    log.info("API: Joining node at {}:{}", ip, portNumber);
                    myNode.join(ip, portNumber);
                    ctx.result("Attempted to join node at " + ip + ":" + portNumber + "\n");
                });
            });
            path("/start_election", () -> {
                get("", ctx -> {
                    log.info("API: Start Election request");
                    myNode.startElection();
                    ctx.result("Election started\n");
                });
            });
            path("/check_leader", () -> {
                get("", ctx -> {
                    log.info("API: Check Leader request");
                    myNode.checkLeader();
                    ctx.result("Current leader: " + myNode.getNeighbours().getLeader() + "\n");
                });
            });
            path("/send_message", () -> {
                post("", ctx -> {
                    String toNick = ctx.formParam("toNick");
                    String message = ctx.formParam("message");
                    log.info("API: Send Message to {}: {}", toNick, message);
                    myNode.sendMessage(toNick, message);
                    ctx.result("Message sent to " + toNick + "\n");
                });
            });
            path("/leave", () -> {
                get("", ctx -> {
                    log.info("API: Leave network request");
                    myNode.leaveNetwork();
                    ctx.result("Node has left the network\n");
                });
            });
            path("/kill", () -> {
                get("", ctx -> {
                    log.info("API: Kill node request");
                    myNode.killNode();
                    ctx.result("Node killed (unresponsive)\n");
                });
            });
            path("/revive", () -> {
                get("", ctx -> {
                    log.info("API: Revive node request");
                    myNode.reviveNode();
                    ctx.result("Node has been revived\n");
                });
            });
            path("/get_status", () -> {
                get("", ctx -> {
                    log.info("API: Get Status request");
                    ctx.result(myNode.getStatus());
                });
            });
            path("/stop_rmi", () -> {
                get("", ctx -> {
                    log.info("API: Stop RMI request");
                    myNode.stopRMI();
                    ctx.result("RMI stopped\n");
                });
            });
            path("/start_rmi", () -> {
                get("", ctx -> {
                    log.info("API: Start RMI request");
                    myNode.startRMI();
                    ctx.result("RMI started\n");
                });
            });
        }).start(port);
        log.info("API started on port {}", port);
    }

    @Override
    public void run() {
        start();
    }
}
