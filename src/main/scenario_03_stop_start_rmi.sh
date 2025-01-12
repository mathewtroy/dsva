#!/bin/bash
# Scenario 5: Testing RMI stop/start on Node1 among 5 nodes.

source bash_variables.sh
SLEEP_TIME=1

echo "=== SCENARIO 3: stop_rmi/start_rmi on Node1 (while 5 nodes exist) ==="

# 1) Check initial statuses for all 5
echo "=== Checking initial statuses of all 5 nodes ==="
for i in 1 2 3 4 5
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

# 2) stop RMI on Node1
echo "=== Stopping RMI on Node1 ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/stop_rmi
sleep ${SLEEP_TIME}

# 3) Check Node1 status
echo "=== Checking Node1 status after stop_rmi ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}

# 4) (Optional) Another node tries to send message to Node1 => likely fails
echo "=== Node2 tries to send message to Node1 => might fail ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/send_message/$((BASE_PORT+1*10))/"HelloNode1StoppedRMI"
sleep ${SLEEP_TIME}

# 5) start RMI on Node1 again
echo "=== Starting RMI on Node1 again ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/start_rmi
sleep ${SLEEP_TIME}

# 6) Possibly do a re-join if necessary (depends on your code)
# For safety, do a join so Node1 is recognized
echo "=== Re-joining Node1 -> Node2, for instance ==="
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/join/${NODE_IP[2]}/${NODE_PORT[2]}
sleep ${SLEEP_TIME}



echo "=== SCENARIO 3 COMPLETE ==="
