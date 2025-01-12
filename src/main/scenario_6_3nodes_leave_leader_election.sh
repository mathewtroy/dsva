#!/bin/bash
# Scenario 6: 3 nodes, leader leaves => forced election among the remaining

source bash_variables.sh
SLEEP_TIME=1

echo "=== SCENARIO 6: 3 nodes, forced election after leader leaves ==="

# 1) Minimal join so all 3 see each other
echo "=== Join commands for Node1->2->3 ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/join/${NODE_IP[2]}/${NODE_PORT[2]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/join/${NODE_IP[3]}/${NODE_PORT[3]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[3]}/${NODE_PORT[3]}
sleep ${SLEEP_TIME}

# 2) Check statuses
echo "=== Checking statuses (Node1, Node2, Node3) ==="
for i in 1 2 3
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

# 3) Suppose Node1 is leader, so Node1 leaves
echo "=== Leader Node1 leaves the network ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/leave
sleep ${SLEEP_TIME}

echo "=== Check Node1 status after leaving ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

# 4) Node2 or Node3 should do an election automatically or we can force it
echo "=== Force election from Node2 and Node3 simultaneously (simulate concurrency) ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/start_election &
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/start_election &
wait
sleep ${SLEEP_TIME}

# 5) Check statuses again
echo "=== Checking final statuses among Node2, Node3 ==="
for i in 2 3
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

echo "=== SCENARIO 6 COMPLETE ==="
