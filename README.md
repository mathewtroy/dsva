# Distributed System with Leader Election (Bully Algorithm)

This project implements a distributed system using the **Bully Algorithm** for leader election.
Nodes communicate via **Java RMI** and expose a REST API for easy management and interaction.
The project also includes automated **Bash scripts** to simplify deployment and testing across multiple virtual machines.

## Running and Testing via API Handler

## 1. Clone and Build the Project

Clone the repository from GitHub.

Build the project using Maven:  `mvn clean package`

## 2. Detect Virtual Machine IPs

Use the provided script to dynamically find and configure IPs of active virtual machines.

The detected IPs are stored in `bash_variables.sh`.

## 3. Start and Connect Nodes

Start all nodes using: `./01_start_nodes.sh`

Connect the nodes to form a network: `./02_connect_nodes.sh`

## 4. Run Test Scenarios

**Use the following scripts to test various functionalities:**

Get Node Status: `./scenario_01_get_status.sh`

Send Messages: `./scenario_02_send_messages.sh`

Stop/Start RMI: `./scenario_03_stop_start_rmi.sh`

Check Leader: `./scenario_04_check_leader.sh`

Kill and Revive Nodes: `./scenario_05_kill_and_revive.sh`

Leave Nodes: `./scenario_06_leave_nodes.sh`