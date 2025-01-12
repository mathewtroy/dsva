#!/bin/bash
# Scenario 4: 5 nodes, removing some (leave), then reviving them

source bash_variables.sh
SLEEP_TIME=1

echo "=== SCENARIO 4: 5 nodes, some leave, then we revive them ==="

# 1) Show status initially
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
sleep ${SLEEP_TIME}

# 2) Node2 leaves
echo "=== Node2 leaves the network ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave
sleep ${SLEEP_TIME}
echo "Check Node2 status"
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}

# 3) Node3 leaves
echo "=== Node3 leaves the network ==="
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/leave
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}

# 4) Now we have Node1, Node4, Node5 presumably. Let's remove Node4 as well
echo "=== Node4 leaves the network ==="
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/leave
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}

# 5) Now we only have Node1, Node5. Suppose Node5 kills itself
echo "=== Node5 kills itself ==="
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/kill
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
sleep ${SLEEP_TIME}

# 6) So presumably Node1 is alone now.
echo "=== Checking Node1 status, presumably alone ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

# 7) Revive Node2 with a known neighbor e.g. Node1
echo "=== Reviving Node2, auto-join Node1 ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/revive/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

# 8) Revive Node5, but do not auto-join => We'll manually do join
echo "=== Reviving Node5, no-arg => it won't auto-join ==="
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/revive/127.0.0.1/0  # or any dummy
# If your code can't handle that, you can do no-arg or pass real IP
sleep ${SLEEP_TIME}
echo "Now manually do join for Node5 -> Node1"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}

# 9) Check statuses
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
sleep ${SLEEP_TIME}

echo "=== SCENARIO 4 COMPLETE ==="
