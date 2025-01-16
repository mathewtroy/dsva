#!/bin/bash
# Join all nodes

source bash_variables.sh

SLEEP_TIME=1

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

