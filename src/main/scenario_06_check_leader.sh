#!/bin/bash
# Simple demonstration of checking the leader via API from Node1 and Node5.

# We assume 5 nodes, with Node5 possibly the leader, etc.
source 02_connect_nodes.sh
SLEEP_TIME=1

echo "=== SCENARIO 6: Checking leader from Node1 and Node5 ==="

# 1) Check leader from Node1
echo "=== Checking leader via Node1 ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/check_leader
sleep ${SLEEP_TIME}
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

# 2) Check leader from Node5
echo "=== Checking leader via Node5 ==="
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/check_leader
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
sleep ${SLEEP_TIME}

echo "=== SCENARIO 6 COMPLETE ==="
