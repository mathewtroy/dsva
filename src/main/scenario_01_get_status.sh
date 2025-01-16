#!/bin/bash

# Check nodes status.

source connect_nodes.sh
#source bash_variables.sh
SLEEP_TIME=1

# Join commands
echo "=== SCENARIO 1: Get status of the 5 Nodes ==="

# Check statuses
echo "=== Checking node statuses ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
echo "=== SCENARIO 1 COMPLETE ==="
