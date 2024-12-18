#!/bin/bash

### GIT variables
GIT_URL=git@gitlab.fel.cvut.cz:pmtestg/dsv_test.git
GIT_BRANCH=master
#GIT_USER=
#GIT_KEY=

### build directory
# semwork home directory
SEMWORK_HOMEDIR=/home/dsv/semwork
CODE_SUBDIR=code

### nodes variables
NUM_NODES=6
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
for I in $(seq 1 $NUM_NODES) ; do
  NODE_IP[$I]=$BASE_IP
  NODE_PORT[$I]=$((${BASE_PORT}+${I}*10))
  NODE_API_PORT[$I]=$((${BASE_PORT}+5000+${I}*10))
done

NODE_NICKNAME[1]=Alice
NODE_NICKNAME[2]=Bob
NODE_NICKNAME[3]=Cecilia
NODE_NICKNAME[4]=Dan
NODE_NICKNAME[5]=Eva
#NODE_NICKNAME[6]=Fiona

# test
#echo $NODE1_IP
#echo $NODE1_PORT
#echo $NODE1_API_PORT
#
#echo ${NODE_NICKNAME[1]}
#echo ${NODE_IP[1]}
#echo ${NODE_PORT[1]}
#echo ${NODE_API_PORT[1]}
