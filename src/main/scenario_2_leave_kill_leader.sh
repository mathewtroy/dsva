#!/bin/bash
# Scenario 2: 4 nodes, some leaving, some killing, verifying leader changes

source bash_variables.sh
SLEEP_TIME=1

echo "=== SCENARIO 2: 4 nodes. Checking status and leader. ==="

# 1) Check all statuses
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}

# 2) Check leader (any node can do check_leader)
echo "=== Checking leader from Node1's perspective ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/check_leader
sleep ${SLEEP_TIME}

# 3) Node1 leaves gracefully
echo "=== Node1 leaves network ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave
sleep ${SLEEP_TIME}
echo "=== Node1 status after leaving ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status

# 4) Node2 kills itself (abrupt)
echo "=== Node2 kills itself ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/kill
sleep ${SLEEP_TIME}
echo "=== Node2 status after kill ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status

# 5) Now we have Node3, Node4 left. Suppose Node4 is leader, kill the leader
echo "=== Checking statuses on Node3 and Node4 ==="
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}

echo "=== Let's kill the leader if Node4 is the leader ==="
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/kill
sleep ${SLEEP_TIME}
echo "=== Node4 killed, check Node4 status ==="
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}

# 6) Now only Node3 remains, check if it has started an election or is alone
echo "=== Node3 final status ==="
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status

echo "=== SCENARIO 2 COMPLETE ==="
