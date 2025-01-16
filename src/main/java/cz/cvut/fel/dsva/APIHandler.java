package cz.cvut.fel.dsva;

import io.javalin.Javalin;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static io.javalin.apibuilder.ApiBuilder.*;

/**
 * Handles HTTP API requests for the Node, allowing external control and monitoring
 * of the node's functionalities via RESTful endpoints.
 *
 * <p>This class implements the {@link Runnable} interface and runs in a separate thread
 * to handle incoming HTTP requests using the Javalin framework.
 *
 * <p>Supported API endpoints include:
 * <ul>
 *     <li><b>GET /join/{ip}/{port}</b>: Join another node in the network.</li>
 *     <li><b>GET /start_election</b>: Initiate a leader election.</li>
 *     <li><b>GET /check_leader</b>: Retrieve the current leader of the network.</li>
 *     <li><b>POST /send_message</b>: Send a message to another node.</li>
 *     <li><b>GET /leave</b>: Leave the network gracefully.</li>
 *     <li><b>GET /kill</b>: Simulate an abrupt node crash.</li>
 *     <li><b>GET /revive</b>: Revive a previously killed node.</li>
 *     <li><b>GET /get_status</b>: Retrieve the current status of the node.</li>
 *     <li><b>GET /stop_rmi</b>: Stop the RMI registry.</li>
 *     <li><b>GET /start_rmi</b>: Start the RMI registry.</li>
 * </ul>
 *
 * @author Kross Aleksandr
 */
@Slf4j
@Getter
@Setter
public class APIHandler implements Runnable {
    private int port = 7000;
    private final Node myNode;
    /**
     * The Javalin instance handling HTTP requests.
     */
    private Javalin app;

    /**
     * Constructs an APIHandler associated with the specified node and port.
     *
     * @param myNode The parent Node instance to control.
     * @param port   The port number for the HTTP API server.
     */
    public APIHandler(Node myNode, int port) {
        this.myNode = myNode;
        this.port = port;
    }

    /**
     * Constructs an APIHandler associated with the specified node using the default port.
     *
     * @param myNode The parent Node instance to control.
     */
    public APIHandler(Node myNode) {
        this(myNode, 7000);
    }

    /**
     * Initializes and starts the Javalin HTTP server with defined routes and handlers.
     */
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
