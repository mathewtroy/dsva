#!/bin/bash
# Send messages, then check statuses.

source scenario_1_connect_nodes.sh
SLEEP_TIME=1

echo "=== SCENARIO 3: Sending messages among 5 nodes ==="

# 1) (Optional) We assume scenario_1_connect_nodes.sh.

# 2) Node1 sends a message "HelloBob" to Node2
echo "=== Node1 => Node2: 'HelloBob' ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/send_message/$((BASE_PORT+2*10))/"HelloBob"
sleep ${SLEEP_TIME}

# 3) Node2 sends a message "HiCecilia" to Node3
echo "=== Node2 => Node3: 'HiCecilia' ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/send_message/$((BASE_PORT+3*10))/"HiCecilia"
sleep ${SLEEP_TIME}

# 4) Check statuses on all nodes
echo "=== Checking statuses after sending messages ==="
for i in 1 2 3 4 5
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

echo "=== SCENARIO 3 COMPLETE ==="
