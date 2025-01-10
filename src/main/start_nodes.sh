#!/bin/bash

# read variable configuration
source bash_variables.sh

### fetch from GIT
# script use SSH keys - you can generate one with ssh-keygen (default values are OK)
# keys are defaultly stored in /home/dsv/.ssh/id_rsa(.pub)
# and then you need to add you public part of the key to gitlab - https://gitlab.fel.cvut.cz/-/user_settings/ssh_keys
if [ ! -d ${SEMWORK_HOMEDIR}/${CODE_SUBDIR} ] ; then
  echo "code directory doesn't exists - creating one"
  mkdir -p ${SEMWORK_HOMEDIR}/${CODE_SUBDIR}
  cd ${SEMWORK_HOMEDIR}
  git clone ${GIT_URL} ${CODE_SUBDIR}
  cd ${CODE_SUBDIR}
  git checkout ${GIT_BRANCH}
fi
 cd ${SEMWORK_HOMEDIR}/${CODE_SUBDIR}
 git checkout ${GIT_BRANCH}
 git pull

### compile/build codes
# maven s needed - sudo apt install maven
mvn clean
mvn package
# where is fat jar stored
FAT_JAR=dsva-pro-1.0-SNAPSHOT-jar-with-dependencies.jar
FAT_JAR_PATH=target

### upload to other nodes
# script use sshpass - sudo apt install sshpass (ssh keys are better way)
# -o StrictHostKeyChecking=no -> do not ask to check host key hash
DSV_PASS=dsv
for ID in $(seq 1 $NUM_NODES) ; do
  echo "Starting node $ID"

  # Create a directory on a remote node
  sshpass -p ${DSV_PASS} ssh -o StrictHostKeyChecking=no dsv@${NODE_IP[$ID]} mkdir -p ${SEMWORK_HOMEDIR}/NODE_${ID}

  # Copy the JAR file
  sshpass -p ${DSV_PASS} scp ${FAT_JAR_PATH}/${FAT_JAR} dsv@${NODE_IP[$ID]}:${SEMWORK_HOMEDIR}/NODE_${ID}/

  # Check and kill an existing tmux session
  sshpass -p ${DSV_PASS} ssh dsv@${NODE_IP[$ID]} -- "tmux kill-session -t NODE_${ID} 2>/dev/null || true"

  # Create a new tmux session and start the node
  sshpass -p ${DSV_PASS} ssh dsv@${NODE_IP[$ID]} -- tmux new-session -d -s NODE_${ID}
  sshpass -p ${DSV_PASS} ssh dsv@${NODE_IP[$ID]} -- "tmux send -t NODE_${ID} 'cd ${SEMWORK_HOMEDIR}/NODE_${ID}/ && java -cp ${FAT_JAR} cz.cvut.fel.dsva.Node ${ID} ${NODE_IP[$ID]} ${NODE_PORT[$ID]}' ENTER"
done
