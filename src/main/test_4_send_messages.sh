#!/bin/bash

# This script demonstrates sending chat messages at key moments.
# For example, you can send a message from Node3 to "Alice" while Node1 (Alice) is being killed.

source bash_variables.sh

SLEEP_TIME=1

echo "==== SCENARIO: SENDING MESSAGES DURING EVENTS ===="

# 1) Node3 sends a message to Alice (Node1)
echo "Node3 -> sending message to 'Alice'"
curl -X POST "http://${NODE_IP[3]}:${NODE_API_PORT[3]}/send_message" \
     -d "toNick=Alice" \
     -d "message=Hello from Node3"
echo
sleep $SLEEP_TIME

# 2) Kill Node1 mid-message
echo "Killing Node1 (Alice) mid-scenario..."
curl -s "http://${NODE_IP[1]}:${NODE_API_PORT[1]}/kill"
sleep $SLEEP_TIME

# 3) Node3 sends another message to 'Alice' - expected to fail or not deliver
echo "Node3 -> sending message to 'Alice' again (but Alice is killed)"
curl -X POST "http://${NODE_IP[3]}:${NODE_API_PORT[3]}/send_message" \
     -d "toNick=Alice" \
     -d "message=Are you still there?"
echo
sleep $SLEEP_TIME

# 4) Node2 starts an election
echo "Node2 (Bob) starts election now..."
curl -s "http://${NODE_IP[2]}:${NODE_API_PORT[2]}/start_election"
sleep $SLEEP_TIME

# 5) Check statuses
for i in $(seq 1 $NUM_NODES); do
  echo "Node $i status:"
  curl -s "http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status"
  echo
  sleep $SLEEP_TIME
done

echo "==== SEND MESSAGES SCENARIO COMPLETE ===="
