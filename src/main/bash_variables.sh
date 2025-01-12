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
NUM_NODES=5
BASE_IP=127.0.0.1
BASE_PORT=2000

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



