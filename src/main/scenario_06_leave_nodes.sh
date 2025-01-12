#!/bin/bash
# scenario_4_leaves_forced_election.sh
# 5 nodes => Node1 leaves, Node2 leaves, Node5 leaves (leader).
# Then only Node3 and Node4 remain => they do a Bully election to choose a new leader.

source 02_connect_nodes.sh
SLEEP_TIME=1

echo "=== SCENARIO 6: Node1, Node2, Node5 leave gracefully, leaving Node3 & Node4 to elect a new leader ==="

############### 1) Node1 leaves ###############
echo "=== Node1 leaves the network ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave
sleep ${SLEEP_TIME}

echo "=== Status of Node1 (just left) ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

echo "=== Status of the remaining nodes after Node1 left ==="
for i in 2 3 4 5
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

############### 2) Node2 leaves ###############
echo "=== Node2 leaves the network ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave
sleep ${SLEEP_TIME}

echo "=== Status of Node2 (just left) ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}

echo "=== Status of the remaining nodes after Node2 left ==="
for i in 3 4 5
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

############### 3) Node5 (leader) leaves ###############
echo "=== Node5 (leader) leaves the network ==="
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/leave
sleep ${SLEEP_TIME}

echo "=== Status of Node5 (just left) ==="
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
sleep ${SLEEP_TIME}

echo "=== Status of the remaining nodes after Node5 left (now only Node3 & Node4 remain) ==="
for i in 3 4
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

############### 4) Force an election among Node3 & Node4 ###############
# Option A: We can just do check_leader from Node4
echo "=== Node4 checks leader => triggers election if old leader is gone ==="
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/check_leader
sleep ${SLEEP_TIME}

############### 5) Final statuses of Node3 & Node4 ###############
echo "=== Final statuses for Node3 & Node4 (one should become new leader) ==="
for i in 3 4
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

echo "=== SCENARIO 6 COMPLETE ==="
