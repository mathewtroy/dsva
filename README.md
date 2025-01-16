# Distributed System with Leader Election (Bully Algorithm)


This project implements a distributed system using the **Bully Algorithm** for leader election.
Nodes communicate via **Java RMI** and expose a REST API for easy management and interaction. 
The project includes automated **Bash scripts** for deployment and testing across multiple virtual machines.


## Features

1. **Leader Election**:
    - Automatic leader election using the Bully Algorithm.
    - Handles leader death or exit gracefully.

2. **Node Communication**:
    - REST API for node management.
    - Java RMI for internal node-to-node communication.

3. **Scenarios**:
    - Incremental joins.
    - Leader reelection on node failure.
    - Concurrent operations.
    - Node leave and revive.

4. **Testing**:
    - Automated Bash scripts for running test scenarios.


## Files Overview

### Core Bash Scripts

1. **`01_start_nodes.sh`**:
    - Starts all nodes on virtual machines.
    - Uses `bash_variables.sh` for node IP and port configurations.
    ```bash
       ./src/main/01_start_nodes.sh
    ```

2. **`02_control_nodes.sh`**:
    - Allows manual control of node states (e.g., start/stop RMI, join operations).
   ```bash
      ./src/main/02_control_nodes.sh
   ```

3. **`bash_variables.sh`**:
   - Stores dynamically detected IPs and ports of virtual machines.
   ```bash
      ./src/main/bash_variables.sh
   ```

4. **`info.txt`**:
    - Contains a brief overview of nodes and their configurations.
 
### Test Scripts

1. **`test_1_incremental_join.sh`**:
    - Tests joining nodes incrementally to form a network.
   ```bash
      ./src/main/test_1_incremental_join.sh
   ```

2. **`test_2_full_leave_kill.sh`**:
    - Simulates nodes leaving or being killed and verifies system consistency.
   ```bash
      ./src/main/test_2_full_leave_kill.sh
   ```

3. **`test_3_simultaneous_election.sh`**:
    - Triggers multiple simultaneous elections to test Bully Algorithm robustness.
   ```bash
      ./src/main/test_3_simultaneous_election.sh
   ```

4. **`test_4_send_messages.sh`**:
    - Tests message sending between nodes.
   ```bash
      ./src/main/test_4_send_messages.sh
   ```

## Instructions

### 1. Clone and Build the Project
```bash
    git clone https://github.com/mathewtroy/dsva.git
   ```
``` bash 
    cd dsva
```

```
mvn clean package
```

### Other useful commands
Run all scripts
``` bash 
    ./src/main/run_all_scripts.sh
```

``` bash 
    ssh-keygen -t rsa -b 1024 -C "alex-debian"
```

``` bash 
    sudo apt install gedit
```

``` bash 
    sudo apt install nmap
```

``` bash 
    sudo apt install sshpass
```
