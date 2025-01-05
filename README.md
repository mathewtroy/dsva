
# Running and Testing via API Handler

To test and interact with the API Handler on different nodes, follow these steps. Each node will listen on a unique API port corresponding to its `nodeId`.

## 1. Starting Nodes

Suppose we have multiple nodes (Node 1, Node 2, Node 3, etc.), each running on a unique API port:

1. **Node 1**:
    - API Port: 7001
    - Command to start:
      `java -jar node.jar 1`

2. **Node 2**:
    - API Port: 7002
    - Command to start:
      `java -jar node.jar 2`

3. **Node 3**:
    - API Port: 7003
    - Command to start:
      `java -jar node.jar 3`

   Continue the same pattern for other nodes.

## 2. API Commands for Testing

Each node exposes several commands via the HTTP API, which can be tested using `curl`. Here are examples of how to use the API:

**2.1. Start Election**

To start an election via the API:

For Node 1:
`curl http://localhost:7001/start_election`

For Node 2:
`curl http://localhost:7002/start_election`

For Node 3:
`curl http://localhost:7003/start_election`

**2.2. Check Leader Status**

To check the leader's status via the API:

For Node 1:
`curl http://localhost:7001/check_leader`

For Node 2:
`curl http://localhost:7002/check_leader`

For Node 3:
`curl http://localhost:7003/check_leader`

**2.3. Send Message to Another Node**

To send a message to another node via the API:

For Node 1 (send message to Node 2):
`curl http://localhost:7001/send_message/2/hello`

For Node 2 (send message to Node 1):
`curl http://localhost:7002/send_message/1/hi`

**2.4. Get Node Status**

To retrieve the status of a node via the API:

For Node 1:
`curl http://localhost:7001/status`

For Node 2:
`curl http://localhost:7002/status`

For Node 3:
`curl http://localhost:7003/status`

## 3. Additional Commands

**Start RMI (for all nodes):**

`curl http://localhost:7001/start_rmi`

`curl http://localhost:7002/start_rmi`

`curl http://localhost:7003/start_rmi`


**Stop RMI (for all nodes):**

`curl http://localhost:7001/stop_rmi`

`curl http://localhost:7002/stop_rmi`

`curl http://localhost:7003/stop_rmi`


**Kill Node (for all nodes):**

`curl http://localhost:7001/kill`

`curl http://localhost:7002/kill`

`curl http://localhost:7003/kill`


**Leave Network Gracefully (for all nodes):**

`curl http://localhost:7001/leave`

`curl http://localhost:7002/leave`

`curl http://localhost:7003/leave`


**Revive Node (for all nodes):**

`curl http://localhost:7001/revive`

`curl http://localhost:7002/revive`

`curl http://localhost:7003/revive`

## 4. Testing API via Browser

Besides using curl, you can also test the API by simply opening the URLs in your browser:

**Start Election:**

`http://localhost:7001/start_election`

**Check Leader:**

`http://localhost:7001/check_leader`

**Send Message:**

`http://localhost:7001/send_message/2/hello`

**Get Node Status:**

`http://localhost:7001/status`

## 5. Notes

Make sure the nodes are correctly configured and can connect to each other via the specified IPs and ports.
If you are using a remote server, replace localhost with the appropriate node IP.