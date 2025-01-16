#!/bin/bash

# This script tries to start elections on multiple nodes simultaneously
# to test concurrency in the Bully algorithm.
# We assume nodes 1..4 are running and have joined the same network.

source bash_variables.sh

SLEEP_TIME=1

echo "==== SCENARIO: SIMULTANEOUS ELECTIONS ===="

# Start elections in parallel on Node2, Node3, Node4
echo "Triggering elections on Node2, Node3, Node4 simultaneously..."
curl -s "http://${NODE_IP[2]}:${NODE_API_PORT[2]}/start_election" &
curl -s "http://${NODE_IP[3]}:${NODE_API_PORT[3]}/start_election" &
curl -s "http://${NODE_IP[4]}:${NODE_API_PORT[4]}/start_election" &
wait

sleep $SLEEP_TIME

# Check status of all
for i in 1 2 3 4; do
  echo "Node $i status:"
  curl -s "http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status"
  echo
  sleep $SLEEP_TIME
done

echo "==== SIMULTANEOUS ELECTION SCENARIO COMPLETE ===="
