#!/bin/bash

echo "Starting all core scripts..."

# 01. Start nodes
echo "Running 01_start_nodes.sh..."
./src/main/01_start_nodes.sh

# 02. Control nodes
echo "Running 02_control_nodes.sh..."
./src/main/02_control_nodes.sh

# Test Scripts
echo "Running test scripts..."

echo "Running test_1_incremental_join.sh..."
./src/main/test_1_incremental_join.sh

echo "Running test_2_full_leave_kill.sh..."
./src/main/test_2_full_leave_kill.sh

echo "Running test_3_simultaneous_election.sh..."
./src/main/test_3_simultaneous_election.sh

echo "Running test_4_send_messages.sh..."
./src/main/test_4_send_messages.sh

echo "All scripts executed!"
