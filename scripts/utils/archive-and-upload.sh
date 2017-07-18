#!/usr/bin/env bash

source ./scripts/utils/print-header.sh
source ./scripts/variables.sh

# Create the jar file
jar="./dist/middleware-ruxu.jar"
ant clean
echo
ant
echo "Created jar file"
echo

# Create the scripts archive file
scripts="./dist/scripts.tar"
tar -czvf ${scripts} ./scripts
echo "Created scripts archive"
echo

# Clean up before upload
parallel-ssh \
  -i \
  -H "${vm_1_public} ${vm_2_public} ${vm_3_public} ${vm_4_public} ${vm_5_public} ${vm_6_public} ${vm_7_public} ${vm_8_public} ${vm_9_public} ${vm_10_public} ${vm_11_public}" \
  -l ruxu \
  "rm -rf ~/middleware-ruxu.jar ~/scripts.tar ~/scripts; ls -l ~"

# Upload the files to the vms
for vm in ${clients_and_servers[@]}
do
  scp ${scripts} ruxu@${vm}:~
done

# Upload the files to the vms
scp ${jar} ruxu@${vm_11_public}:~
scp ${scripts} ruxu@${vm_11_public}:~

parallel-ssh \
  -i \
  -H "${vm_1_public} ${vm_2_public} ${vm_3_public} ${vm_4_public} ${vm_5_public} ${vm_6_public} ${vm_7_public} ${vm_8_public} ${vm_9_public} ${vm_10_public} ${vm_11_public}" \
  -l ruxu \
  "tar -xvf ~/scripts.tar; ls -l ~"

