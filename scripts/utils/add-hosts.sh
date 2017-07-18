#!/usr/bin/env bash

source ./scripts/utils/print-header.sh
source ./scripts/variables.sh

# Add the vms to the known_hosts
for vm in ${vms[@]}
do
  ssh ruxu@${vm} echo "Added ${vm} to known_hosts"
  echo
done
