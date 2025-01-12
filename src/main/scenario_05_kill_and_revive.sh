#!/bin/bash
# Kill/revive Node1, leave/revive Node2.

source 02_connect_nodes.sh
SLEEP_TIME=1

echo "=== SCENARIO 5: Kill/Revive on Node1, then Leave/Revive on Node2 ==="

# 1) Kill Node1
echo "=== Killing Node1 ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/kill
sleep ${SLEEP_TIME}

echo "=== Checking Node1 status after kill ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

# 2) Revive Node1
echo "=== Reviving Node1 (no-arg or with join IP/port if you want) ==="
# If your REST has `revive/{ip}/{port}`, you can do that. Otherwise a no-arg:
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/revive/${NODE_IP[4]}/${NODE_PORT[4]}
# or simply => /revive (depends on your API)
sleep ${SLEEP_TIME}

echo "=== Checking Node1 status after revive ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

# 3) Node2 leaves gracefully
echo "=== Node2 leaves the network ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave
sleep ${SLEEP_TIME}

echo "=== Checking Node2 status after leaving ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}

# 4) Revive Node2
# Possibly do /revive without arguments, or pass known neighbor
echo "=== Reviving Node2 ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/revive/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}

echo "=== Checking Node2 status after revive ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}


echo "=== SCENARIO 5 COMPLETE ==="
