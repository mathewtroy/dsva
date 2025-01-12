#!/bin/bash
# Scenario 2 (updated for 5 nodes): multiple nodes leave the network, verifying statuses and leadership changes.

source bash_variables.sh
SLEEP_TIME=1

echo "=== SCENARIO 2: 5 nodes. Multiple leaves, final node becomes leader. ==="

### 1) Check initial status for all 5 nodes
echo "=== Checking initial statuses for all 5 nodes ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
sleep ${SLEEP_TIME}

### 2) Node1 leaves gracefully
echo "=== Node1 leaves network (Node1: IP=${NODE_IP[1]}, PORT=${NODE_PORT[1]}, API_PORT=${NODE_API_PORT[1]}) ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave
sleep ${SLEEP_TIME}
echo "=== Node1 status after leaving ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

### 3) Node2 leaves gracefully
echo "=== Node2 leaves network (Node2: IP=${NODE_IP[2]}, PORT=${NODE_PORT[2]}, API_PORT=${NODE_API_PORT[2]}) ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave
sleep ${SLEEP_TIME}
echo "=== Node2 status after leaving ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}

### 4) Node3 leaves gracefully
echo "=== Node3 leaves network (Node3: IP=${NODE_IP[3]}, PORT=${NODE_PORT[3]}, API_PORT=${NODE_API_PORT[3]}) ==="
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/leave
sleep ${SLEEP_TIME}
echo "=== Node3 status after leaving ==="
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}

### 5) Node5 (the leader) leaves gracefully
echo "=== Node5 (leader) leaves network (Node5: IP=${NODE_IP[5]}, PORT=${NODE_PORT[5]}, API_PORT=${NODE_API_PORT[5]}) ==="
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/leave
sleep ${SLEEP_TIME}
echo "=== Node5 (leader) status after leaving ==="
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
sleep ${SLEEP_TIME}

### 6) Now only Node4 remains (192.168.56.109). It should be the sole node => becomes leader
echo "=== Checking Node4 final status (it should be alone and thus leader). ==="
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}

echo "=== SCENARIO 2 COMPLETE ==="
