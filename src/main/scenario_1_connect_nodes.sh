#!/bin/bash

# This script connects 5 nodes among themselves,
# then checks their statuses.

source bash_variables.sh
SLEEP_TIME=1

# Join commands
echo "=== SCENARIO 1: Connect 5 Nodes ==="
echo "Joining Node1 -> Node2, Node3, Node4, Node5"
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/join/${NODE_IP[2]}/${NODE_PORT[2]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/join/${NODE_IP[3]}/${NODE_PORT[3]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/join/${NODE_IP[4]}/${NODE_PORT[4]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/join/${NODE_IP[5]}/${NODE_PORT[5]}
sleep ${SLEEP_TIME}

echo "Joining Node2 -> Node1, Node3, Node4, Node5"
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[3]}/${NODE_PORT[3]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[4]}/${NODE_PORT[4]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[5]}/${NODE_PORT[5]}
sleep ${SLEEP_TIME}

echo "Joining Node3 -> Node1, Node2, Node4, Node5"
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[2]}/${NODE_PORT[2]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[4]}/${NODE_PORT[4]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[5]}/${NODE_PORT[5]}
sleep ${SLEEP_TIME}

echo "Joining Node4 -> Node1, Node2, Node3, Node5"
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/join/${NODE_IP[2]}/${NODE_PORT[2]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/join/${NODE_IP[3]}/${NODE_PORT[3]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/join/${NODE_IP[5]}/${NODE_PORT[5]}
sleep ${SLEEP_TIME}

echo "Joining Node5 -> Node1, Node2, Node3, Node4"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/join/${NODE_IP[2]}/${NODE_PORT[2]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/join/${NODE_IP[3]}/${NODE_PORT[3]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/join/${NODE_IP[4]}/${NODE_PORT[4]}
sleep ${SLEEP_TIME}

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
