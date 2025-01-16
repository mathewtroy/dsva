#!/bin/bash
# Print all status information

source bash_variables.sh
SLEEP_TIME=1

echo "Check status of all nodes..."
for i in $(seq 1 $NUM_NODES); do
  curl -s "http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status" ; echo
  sleep $SLEEP_TIME
done