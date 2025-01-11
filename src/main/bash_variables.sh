#!/bin/bash

### GIT variables
GIT_URL=git@github.com:mathewtroy/dsva.git
GIT_BRANCH=feature/script
#GIT_USER=
GIT_KEY=~/.ssh/id_rsa


### build directory
# semwork home directory
SEMWORK_HOMEDIR=/home/dsv/semwork2
CODE_SUBDIR=code

### nodes variables
NUM_NODES=4
BASE_IP=127.0.0.1
BASE_PORT=2000

## define node manually
#NODE1_IP=192.168.56.183
#NODE1_PORT=$((${BASE_PORT}+10))
#NODE1_API_PORT=$((${BASE_PORT}+5000+10))
#NODE2_IP=${NODE1_IP}
#NODE2_PORT=$((${BASE_PORT}+10))
#NODE2_API_PORT=$((${BASE_PORT}+5000+10))

## define within for loop
#for I in $(seq 1 $NUM_NODES) ; do
#  declare NODE${I}_IP=$BASE_IP
#  declare NODE${I}_PORT=$((${BASE_PORT}+${I}*10))
#  declare NODE${I}_API_PORT=$((${BASE_PORT}+5000+${I}*10))
#done

## define within for loop with use of arrays
#for I in $(seq 1 $NUM_NODES) ; do
#  NODE_IP[$I]=$BASE_IP
#  NODE_PORT[$I]=$((${BASE_PORT}+${I}*10))
#  NODE_API_PORT[$I]=$((${BASE_PORT}+5000+${I}*10))
#done

declare -A NODE_IP
declare -A NODE_PORT
declare -A NODE_API_PORT

# Network range to scan (adjust based on your network configuration)
NETWORK_RANGE="192.168.56.0/24"
ACTIVE_HOSTS=$(nmap -sn $NETWORK_RANGE | grep "Nmap scan report" | awk '{print $NF}' | tr -d '()')

ID=1
for IP in $ACTIVE_HOSTS; do
  if [ $ID -le $NUM_NODES ]; then
    NODE_IP[$ID]=$IP
    NODE_PORT[$ID]=$((BASE_PORT + ID * 10))
    NODE_API_PORT[$ID]=$((BASE_PORT + 5000 + ID * 10))
    echo "Node $ID - IP: ${NODE_IP[$ID]}, PORT: ${NODE_PORT[$ID]}, API_PORT: ${NODE_API_PORT[$ID]}"
    ((ID++))
  fi
done


NODE_NICKNAME[1]=Alice
NODE_NICKNAME[2]=Bob
NODE_NICKNAME[3]=Cecilia
NODE_NICKNAME[4]=Dan
NODE_NICKNAME[5]=Ethan
NODE_NICKNAME[6]=Ella

# test
#echo $NODE1_IP
#echo $NODE1_PORT
#echo $NODE1_API_PORT
#
#echo ${NODE_NICKNAME[1]}
#echo ${NODE_IP[1]}
#echo ${NODE_PORT[1]}
#echo ${NODE_API_PORT[1]}
