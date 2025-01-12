#!/bin/bash
# Scenario 7: 5 nodes, leader leaves => forced election among the remaining.

source 02_connect_nodes.sh
SLEEP_TIME=1

echo "=== SCENARIO 7: Forced election after multiple leaves, leaving 2 nodes. ==="

# 1) Node1 leaves
echo "=== Node1 leaves the network ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave
sleep ${SLEEP_TIME}
echo "Node1 status after leaving:"
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

# 2) Node2 leaves
echo "=== Node2 leaves the network ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave
sleep ${SLEEP_TIME}
echo "Node2 status after leaving:"
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}

# Now we have Node3, Node4, Node5. Suppose Node5 is leader

# 5) Node5 (the leader) leaves
echo "=== Node5 (leader) leaves the network ==="
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/leave
sleep ${SLEEP_TIME}
echo "Node5 status after leaving:"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
sleep ${SLEEP_TIME}

# 6) Only Node3 and Node4 remain => forced to do an election
echo "=== Starting election on Node3 and Node4 to see who becomes leader ==="
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/start_election &
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/start_election &
wait
sleep ${SLEEP_TIME}

# 7) Check final statuses for Node3 and Node4
echo "=== Checking final statuses on Node3, Node4 ==="
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}

echo "=== SCENARIO 7 COMPLETE ==="
