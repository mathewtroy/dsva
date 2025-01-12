#!/bin/bash
# Scenario 3: 5 nodes, test sending messages between two nodes

source bash_variables.sh
SLEEP_TIME=1

echo "=== SCENARIO 3: Sending messages among 5 nodes ==="

# 1) (Optional) ensure they're connected.
# Or we assume scenario_1_connect_5nodes.sh was already run.

# 2) Let's send a message from Node1 -> Node2
echo "=== Node1 sends message 'HelloBob' to Node2 ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/send_message/$((${BASE_PORT}+2*10))/"HelloBob"
sleep ${SLEEP_TIME}

# 3) Node2 sends message to Node3
echo "=== Node2 sends message 'HiCecilia' to Node3 ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/send_message/$((${BASE_PORT}+3*10))/"HiCecilia"
sleep ${SLEEP_TIME}

# 4) Node4 sends message to Node5
echo "=== Node4 sends message 'HeyEthan' to Node5 ==="
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/send_message/$((${BASE_PORT}+5*10))/"HeyEthan"
sleep ${SLEEP_TIME}

# 5) Check statuses
echo "=== Checking statuses after messages ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status

echo "=== SCENARIO 3 COMPLETE ==="
