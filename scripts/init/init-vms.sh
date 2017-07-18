#!/usr/bin/env bash

source ./scripts/utils/print-header.sh
source ./scripts/variables.sh

# Initialize clients and servers
for vm in ${clients_and_servers[@]}
do
  ssh ruxu@${vm} "~/scripts/init/init-client-and-server.sh"
  echo "Initialized vm ${vm}"
  echo
done

# Initialize middleware
ssh ruxu@${vm_11_public} "~/scripts/init/init-middleware.sh"
echo "Initialized vm ${vm_11_public}"
echo
