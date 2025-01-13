#!/bin/bash

# Kill and then revive Node1.

source 02_connect_nodes.sh
SLEEP_TIME=1

echo "=== SCENARIO 5: Kill & No-Arg Revive Node1 ==="

# 1) Kill Node1
echo "=== Killing Node1 ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/kill
sleep ${SLEEP_TIME}

echo "=== Checking Node1 status after kill ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

# 2) Revive Node1 (no-arg)
echo "=== Reviving Node1 (no-arg) ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/revive
sleep ${SLEEP_TIME}

echo "=== Checking Node1 status after revive ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

echo "=== SCENARIO 5 COMPLETE ==="
