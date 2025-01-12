#!/bin/bash

# Kill Node1, Node2, Node5 => forced election among Node3 & Node4.

source 02_connect_nodes.sh
SLEEP_TIME=1

echo "=== SCENARIO 7: Killing (no neighbor notification) of Node1, Node2, Node5. ==="
echo "=== Then Node4 checks leader, forcing election if old leader is unreachable. ==="

# 1) Kill Node1
echo "=== Killing Node1 ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/kill
sleep ${SLEEP_TIME}
echo "Node1 status after kill:"
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

# 2) Kill Node2
echo "=== Killing Node2 ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/kill
sleep ${SLEEP_TIME}
echo "Node2 status after kill:"
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}

# 3) Kill Node5 (the leader)
echo "=== Killing Node5 (leader) ==="
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/kill
sleep ${SLEEP_TIME}
echo "Node5 status after kill:"
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
sleep ${SLEEP_TIME}

# Now only Node3 and Node4 remain. They do NOT know that Node1, Node2, Node5 are gone
# because kill() does not notify neighbors.

# 4) Node4 checks leader => triggers election if old leader is unreachable
echo "=== Node4 checks leader => triggers election if leader is gone ==="
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/check_leader
sleep ${SLEEP_TIME}

# 5) Check final statuses on Node3 and Node4
echo "=== Checking final statuses on Node3, Node4 ==="
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}

echo "=== SCENARIO 7 COMPLETE ==="
