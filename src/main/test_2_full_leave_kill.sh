#!/bin/bash

# This script tests removing nodes one by one until only one remains,
# mixing "leave" (graceful) and "kill" (abrupt).
# We assume we start with 5 running nodes in a full mesh.

source bash_variables.sh

SLEEP_TIME=1

echo "==== SCENARIO: LEAVE AND KILL NODES ===="

# 1) Node5 leaves gracefully
echo "Node5 (Ethan) leaving..."
curl -s "http://${NODE_IP[5]}:${NODE_API_PORT[5]}/leave"
sleep $SLEEP_TIME

# 2) Node4 is killed abruptly
echo "Node4 (Dan) being killed..."
curl -s "http://${NODE_IP[4]}:${NODE_API_PORT[4]}/kill"
sleep $SLEEP_TIME

# 3) Node3 also leaves gracefully
echo "Node3 (Cecilia) leaving..."
curl -s "http://${NODE_IP[3]}:${NODE_API_PORT[3]}/leave"
sleep $SLEEP_TIME

# 4) Check status of Node1 and Node2
echo "Status of Node1 (Alice) and Node2 (Bob):"
for i in 1 2; do
  curl -s "http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status"
  echo
  sleep $SLEEP_TIME
done

# 5) Kill Node2 so only Node1 remains
echo "Node2 (Bob) being killed..."
curl -s "http://${NODE_IP[2]}:${NODE_API_PORT[2]}/kill"
sleep $SLEEP_TIME

# 6) Check final status of Node1
echo "Final status of Node1 (Alice):"
curl -s "http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status"
echo

echo "==== LEAVE/KILL SCENARIO COMPLETE ===="
