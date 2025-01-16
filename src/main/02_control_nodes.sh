#!/bin/bash

source bash_variables.sh

SLEEP_TIME=1

echo "==== Control script started ===="

# Example sequence:
# 1) Node 2 joins Node 1
# 2) Node 3 joins Node 1
# 3) Node 4 joins Node 1
# 4) Node 5 joins Node 1
# 5) Check status of each node
# 6) Start election on Node 1
# 7) Kill Node 1 (the leader) and see how the others respond
# 8) Node 2 starts election
# 9) Node 5 leaves the network
# 10) Node 1 revives
# etc.

# Adjust these steps as needed for your test scenario.

echo "Node2 joining Node1..."
curl -s "http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[1]}/${NODE_PORT[1]}" ; echo
sleep $SLEEP_TIME

echo "Node3 joining Node1..."
curl -s "http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[1]}/${NODE_PORT[1]}" ; echo
sleep $SLEEP_TIME

echo "Node4 joining Node1..."
curl -s "http://${NODE_IP[4]}:${NODE_API_PORT[4]}/join/${NODE_IP[1]}/${NODE_PORT[1]}" ; echo
sleep $SLEEP_TIME

echo "Node5 joining Node1..."
curl -s "http://${NODE_IP[5]}:${NODE_API_PORT[5]}/join/${NODE_IP[1]}/${NODE_PORT[1]}" ; echo
sleep $SLEEP_TIME

echo "Check status of all nodes..."
for i in $(seq 1 $NUM_NODES); do
  curl -s "http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status" ; echo
  sleep $SLEEP_TIME
done

echo "Start election on Node1..."
curl -s "http://${NODE_IP[1]}:${NODE_API_PORT[1]}/start_election" ; echo
sleep $SLEEP_TIME

echo "Kill the leader (Node1) to see how others react..."
curl -s "http://${NODE_IP[1]}:${NODE_API_PORT[1]}/kill" ; echo
sleep $SLEEP_TIME

echo "Check status after kill..."
for i in $(seq 1 $NUM_NODES); do
  curl -s "http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status" ; echo
  sleep $SLEEP_TIME
done

echo "Node2 starts election..."
curl -s "http://${NODE_IP[2]}:${NODE_API_PORT[2]}/start_election" ; echo
sleep $SLEEP_TIME

echo "Node5 leaves the network..."
curl -s "http://${NODE_IP[5]}:${NODE_API_PORT[5]}/leave" ; echo
sleep $SLEEP_TIME

echo "Check status after Node5 left..."
for i in $(seq 1 $NUM_NODES); do
  curl -s "http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status" ; echo
  sleep $SLEEP_TIME
done

echo "Revive Node1..."
curl -s "http://${NODE_IP[1]}:${NODE_API_PORT[1]}/revive" ; echo
sleep $SLEEP_TIME

echo "Check final status..."
for i in $(seq 1 $NUM_NODES); do
  curl -s "http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status" ; echo
  sleep $SLEEP_TIME
done

echo "==== Control script finished ===="
