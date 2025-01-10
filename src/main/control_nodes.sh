#!/bin/bash
source bash_variables.sh

SLEEP_TIME=1

curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[3]}/${NODE_PORT[3]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[4]}/${NODE_PORT[4]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/join/${NODE_IP[5]}/${NODE_PORT[5]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[4]}/${NODE_PORT[4]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/join/${NODE_IP[5]}/${NODE_PORT[5]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/join/${NODE_IP[5]}/${NODE_PORT[5]}
sleep ${SLEEP_TIME}
#curl http://${NODE_IP[6]}:${NODE_API_PORT[6]}/join/${NODE_IP[1]}/${NODE_PORT[1]}
#sleep ${SLEEP_TIME}
##
curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}
curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
sleep ${SLEEP_TIME}
#curl http://${NODE_IP[6]}:${NODE_API_PORT[6]}/get_status
#sleep ${SLEEP_TIME}
##
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/stop_rmi
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_API_PORT[4]}/get_status
sleep ${SLEEP_TIME}
#curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/send_hello_leader
#sleep ${SLEEP_TIME}
#curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/get_status
#sleep ${SLEEP_TIME}
#curl http://${NODE_IP[3]}:${NODE_API_PORT[3]}/get_status
#sleep ${SLEEP_TIME}

#
#curl http://${NODE_IP[6]}:${NODE_API_PORT[6]}/stop_rmi
#sleep ${SLEEP_TIME}
#curl http://${NODE_IP[2]}:${NODE_API_PORT[2]}/send_hello_leader
#sleep ${SLEEP_TIME}
##
#curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/start_rmi
#sleep ${SLEEP_TIME}
#curl http://${NODE_IP[1]}:${NODE_API_PORT[1]}/join/${NODE_IP[5]}/${NODE_PORT[5]}
#sleep ${SLEEP_TIME}
#curl http://${NODE_IP[6]}:${NODE_API_PORT[6]}/start_rmi
#sleep ${SLEEP_TIME}
#curl http://${NODE_IP[6]}:${NODE_API_PORT[6]}/join/${NODE_IP[5]}/${NODE_PORT[5]}
#sleep ${SLEEP_TIME}
##
#curl http://${NODE_IP[5]}:${NODE_API_PORT[5]}/get_status
#sleep ${SLEEP_TIME}
# start election ...
