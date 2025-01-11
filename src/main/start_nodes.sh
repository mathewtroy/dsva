#!/bin/bash

# Read variable configuration
source bash_variables.sh

### Fetch code from GIT if necessary
if [ ! -d ${SEMWORK_HOMEDIR}/${CODE_SUBDIR} ] ; then
  echo "Code directory doesn't exist - creating one."
  mkdir -p ${SEMWORK_HOMEDIR}/${CODE_SUBDIR}
  cd ${SEMWORK_HOMEDIR}
  git clone ${GIT_URL} ${CODE_SUBDIR}
  cd ${CODE_SUBDIR}
  git checkout ${GIT_BRANCH}
fi

cd ${SEMWORK_HOMEDIR}/${CODE_SUBDIR}
git checkout ${GIT_BRANCH}
git pull

### Compile/build the project (Maven)
mvn clean
mvn package

FAT_JAR=dsva-pro-1.0-SNAPSHOT-jar-with-dependencies.jar
FAT_JAR_PATH=target

DSV_PASS=dsv

# Optional: kill any existing tmux sessions with the same names
# (comment out if you do not want to auto-kill sessions)
# for ID in $(seq 1 $NUM_NODES) ; do
#   sshpass -p ${DSV_PASS} ssh -o StrictHostKeyChecking=no dsv@${NODE_IP[$ID]} "tmux kill-session -t NODE_${ID} 2>/dev/null || true"
# done

for ID in $(seq 1 $NUM_NODES) ; do
  echo "Starting node $ID"

  # Create directory on remote node
  sshpass -p ${DSV_PASS} ssh -o StrictHostKeyChecking=no dsv@${NODE_IP[$ID]} mkdir -p ${SEMWORK_HOMEDIR}/NODE_${ID}

  # Copy the JAR
  sshpass -p ${DSV_PASS} scp ${FAT_JAR_PATH}/${FAT_JAR} dsv@${NODE_IP[$ID]}:${SEMWORK_HOMEDIR}/NODE_${ID}/

  # Kill old tmux session if it exists
  sshpass -p ${DSV_PASS} ssh dsv@${NODE_IP[$ID]} -- "tmux kill-session -t NODE_${ID} 2>/dev/null || true"

  # Create new tmux session
  sshpass -p ${DSV_PASS} ssh dsv@${NODE_IP[$ID]} -- tmux new-session -d -s NODE_${ID}

  if [ $ID -eq 1 ]; then
    # First node: 2 arguments (nodeId, myIP)
    sshpass -p ${DSV_PASS} ssh dsv@${NODE_IP[$ID]} -- "tmux send -t NODE_${ID} 'cd ${SEMWORK_HOMEDIR}/NODE_${ID}/ && java -cp ${FAT_JAR} cz.cvut.fel.dsva.Node ${ID} ${NODE_IP[$ID]}' ENTER"
  else
    # Other nodes: 4 arguments (nodeId, myIP, otherNodeIP, otherNodePort) => auto-join the first node
    sshpass -p ${DSV_PASS} ssh dsv@${NODE_IP[$ID]} -- "tmux send -t NODE_${ID} 'cd ${SEMWORK_HOMEDIR}/NODE_${ID}/ && java -cp ${FAT_JAR} cz.cvut.fel.dsva.Node ${NODE_NICKNAME[$ID]} ${NODE_IP[$ID]} ${NODE_PORT[$ID]}' ENTER"
  fi
done
