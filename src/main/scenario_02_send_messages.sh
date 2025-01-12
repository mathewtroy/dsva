#!/bin/bash
# Send messages, then check statuses.

source bash_variables.sh
SLEEP_TIME=1

echo "=== SCENARIO 2: Sending messages among 5 nodes ==="

# 2) Node1 sends a message "HelloBob" to Node2
echo "=== Node1 => Node2: 'HelloBob' ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/send_message/$((BASE_PORT+2*10))/"HelloBob"
sleep ${SLEEP_TIME}

# 3) Node2 sends a message "HiCecilia" to Node3
echo "=== Node2 => Node3: 'HiCecilia' ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/send_message/$((BASE_PORT+3*10))/"HiCecilia"
sleep ${SLEEP_TIME}

echo "=== SCENARIO 2 COMPLETE ==="
