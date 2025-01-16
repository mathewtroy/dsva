#!/bin/bash

### GIT variables
GIT_URL=git@github.com:mathewtroy/dsva.git
GIT_BRANCH=feature/chat
GIT_KEY=~/.ssh/id_rsa

### semwork (home) directory on remote VMs
SEMWORK_HOMEDIR=/home/dsv/semwork
CODE_SUBDIR=code

### Number of nodes
NUM_NODES=5

### Base configuration for nodes
BASE_IP=127.0.0.1
BASE_PORT=2000

declare -A NODE_IP
declare -A NODE_PORT
declare -A NODE_API_PORT
declare -A NODE_NICKNAME

# Manual assignment.
# Example manual assignment:

#NODE_IP[1]=192.168.56.105
#NODE_IP[2]=192.168.56.106
#NODE_IP[3]=192.168.56.107
#NODE_IP[4]=192.168.56.109
#NODE_IP[5]=192.168.56.154

# Alternatively, you can attempt to dynamically discover them using nmap, if your network supports it.
# Example (commented):
NETWORK_RANGE="192.168.56.0/24"
ACTIVE_HOSTS=$(nmap -sn $NETWORK_RANGE | grep "Nmap scan report" | awk '{print $NF}' | tr -d '()')
ID=1
for IP in $ACTIVE_HOSTS; do
 if [ $ID -le $NUM_NODES ]; then
   NODE_IP[$ID]=$IP
   ((ID++))
 fi
done

# Generate ports for each node
for i in $(seq 1 $NUM_NODES) ; do
  NODE_PORT[$i]=$((BASE_PORT + i * 10))
  NODE_API_PORT[$i]=$((BASE_PORT + 5000 + i * 10))
done

# Nicknames for nodes
NODE_NICKNAME[1]=Alice
NODE_NICKNAME[2]=Bob
NODE_NICKNAME[3]=Cecilia
NODE_NICKNAME[4]=Dan
NODE_NICKNAME[5]=Ethan

# If you want to see the resolved configuration:
echo "----- Configuration -----"
for i in $(seq 1 $NUM_NODES); do
  echo "Node $i => IP: ${NODE_IP[$i]}, RMI_PORT: ${NODE_PORT[$i]}, API_PORT: ${NODE_API_PORT[$i]}, Nickname: ${NODE_NICKNAME[$i]}"
done
echo "-------------------------"
