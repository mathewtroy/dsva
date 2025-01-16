#!/bin/bash

# This script demonstrates an incremental joining scenario:
# Start from Node1 alone, then gradually join Nodes 2, 3, 4, and 5.
# After each join, check the status of all nodes.

source bash_variables.sh

SLEEP_TIME=1

echo "==== SCENARIO: INCREMENTAL JOIN ===="

# 1) Node2 joins Node1
echo "Node2 joining Node1..."
curl -s "http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[1]}/${NODE_PORT[1]}"
sleep $SLEEP_TIME

# 2) Node3 joins Node1
echo "Node3 joining Node1..."
curl -s "http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[1]}/${NODE_PORT[1]}"
sleep $SLEEP_TIME

# 3) Node4 joins Node1
echo "Node4 joining Node1..."
curl -s "http://${NODE_IP[4]}:${NODE_API_PORT[4]}/join/${NODE_IP[1]}/${NODE_PORT[1]}"
sleep $SLEEP_TIME

# 4) Node5 joins Node1
echo "Node5 joining Node1..."
curl -s "http://${NODE_IP[5]}:${NODE_API_PORT[5]}/join/${NODE_IP[1]}/${NODE_PORT[1]}"
sleep $SLEEP_TIME

echo "All nodes joined. Checking status of each..."
for i in $(seq 1 $NUM_NODES); do
  curl -s "http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status"
  echo
  sleep $SLEEP_TIME
done

echo "==== INCREMENTAL JOIN SCENARIO COMPLETE ===="
