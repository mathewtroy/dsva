#!/bin/bash

# 5 nodes => Node1 leaves, Node2 leaves, Node3 leaves, Node5 (leader) leaves.
# Then Node4 performs a Bully election to become the new leader.

source 02_connect_nodes.sh
SLEEP_TIME=1

echo "=== SCENARIO 6: Node1, Node2, Node3, Node5 leave gracefully, leaving Node4 to elect as new leader ==="

############### 1) Node1 leaves ###############
echo "=== Node1 leaves the network ==="
curl s http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave
echo "Node1 left the network gracefully."
sleep ${SLEEP_TIME}

echo "=== Status of Node1 (just left) ==="
curl s http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
echo ""
sleep ${SLEEP_TIME}

echo "=== Status of the remaining nodes after Node1 left ==="
for i in 2 3 4 5
do
  echo "----- Node$i Status -----"
  curl s http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  echo ""
  sleep ${SLEEP_TIME}
done

############### 2) Node2 leaves ###############
echo "=== Node2 leaves the network ==="
curl s http://${NODE_IP[2]}:${NODE_API_PORT[2]}/leave
echo "Node2 left the network gracefully."
sleep ${SLEEP_TIME}

echo "=== Status of Node2 (just left) ==="
curl s http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
echo ""
sleep ${SLEEP_TIME}

echo "=== Status of the remaining nodes after Node2 left ==="
for i in 3 4 5
do
  echo "----- Node$i Status -----"
  curl s http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  echo ""
  sleep ${SLEEP_TIME}
done

############### 3) Node3 leaves ###############
echo "=== Node3 leaves the network ==="
curl s http://${NODE_IP[3]}:${NODE_API_PORT[3]}/leave
echo "Node3 left the network gracefully."
sleep ${SLEEP_TIME}

echo "=== Status of Node3 (just left) ==="
curl s http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
echo ""
sleep ${SLEEP_TIME}

echo "=== Status of the remaining nodes after Node3 left ==="
for i in 4 5
do
  echo "----- Node$i Status -----"
  curl s http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  echo ""
  sleep ${SLEEP_TIME}
done

############### 4) Node5 (leader) leaves ###############
echo "=== Node5 (leader) leaves the network ==="
curl s http://${NODE_IP[5]}:${NODE_API_PORT[5]}/leave
echo "Node5 (leader) left the network gracefully."
sleep ${SLEEP_TIME}

echo "=== Status of Node5 (just left) ==="
curl s http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
echo ""
sleep ${SLEEP_TIME}

echo "=== Status of the remaining nodes after Node5 left ==="
for i in 4
do
  echo "----- Node$i Status -----"
  curl s http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  echo ""
  sleep ${SLEEP_TIME}
done

############### 5) Force an election on Node4 ###############
echo "=== Node4 checks leader => triggers election since old leader is gone ==="
curl s http://${NODE_IP[4]}:${NODE_API_PORT[4]}/check_leader
echo "Triggered leader check on Node4."
sleep ${SLEEP_TIME}

echo "=== Node4 starts election explicitly ==="
curl s http://${NODE_IP[4]}:${NODE_API_PORT[4]}/start_election
echo "Node4 started the election."
sleep ${SLEEP_TIME}

############### 6) Final status of Node4 ###############
echo "=== Final status of Node4 (should be the new leader) ==="
curl s http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
echo ""
sleep ${SLEEP_TIME}

echo "=== SCENARIO 6 COMPLETE ==="
