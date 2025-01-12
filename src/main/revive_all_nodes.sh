#!/bin/bash
# A script that calls /revive (no-arg) on every node 1..5

source bash_variables.sh
SLEEP_TIME=1

echo "=== REVIVE ALL NODES (no-arg) ==="

for i in 1 2 3 4 5
do
  echo "Reviving Node $i..."
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/revive
  sleep ${SLEEP_TIME}
done

echo "=== Checking statuses after reviving all nodes ==="
for i in 1 2 3 4 5
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

echo "=== REVIVE ALL NODES COMPLETE ==="
