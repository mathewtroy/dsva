#!/bin/bash
# Scenario 5: Testing stop_rmi and start_rmi on multiple nodes

source bash_variables.sh
SLEEP_TIME=1

echo "=== SCENARIO 5: Testing RMI stop/start on 5 nodes ==="

# 1) Assume nodes are connected (scenario_1) or do minimal join
echo "=== Check initial statuses ==="
for i in 1 2 3 4 5
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

# 2) stop RMI on Node3
echo "=== Stopping RMI on Node3 ==="
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/stop_rmi
sleep ${SLEEP_TIME}

# 3) Check Node3 status
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}

# 4) Node2 tries to send message to Node3 => should fail or no response
echo "=== Node2 tries to send message to Node3 (likely fail) ==="
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/send_message/$((${BASE_PORT}+3*10))/"Hello3Stopped"

# 5) Now start RMI again on Node3
echo "=== Starting RMI on Node3 again ==="
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/start_rmi
sleep ${SLEEP_TIME}

# 6) Possibly re-join or just let Node3 do it if your code auto-joins
# For safety, we do a join
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[2]}/${NODE_PORT[2]}
sleep ${SLEEP_TIME}

# 7) Check statuses
for i in 1 2 3 4 5
do
  curl http://${NODE_IP[$i]}:${NODE_API_PORT[$i]}/get_status
  sleep ${SLEEP_TIME}
done

echo "=== SCENARIO 5 COMPLETE ==="
