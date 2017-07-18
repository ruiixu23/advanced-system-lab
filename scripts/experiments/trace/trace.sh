#!/usr/bin/env bash

source ./scripts/utils/print-header.sh
source ./scripts/variables.sh

parallel-ssh \
  -i \
  -H "${vm_6_public} ${vm_7_public} ${vm_8_public}" \
  -l ruxu \
  "memcached -d -p 11212 -t 1 -m 512"

echo
echo "Servers started"
echo
sleep 5

parallel-ssh \
  -i \
  -H "${vm_11_public}" \
  -l ruxu \
  "rm -rf ~/log; mkdir -p ~/log; nohup java -jar ~/middleware-ruxu.jar -l ${vm_11_private} -p 8000 -m ${vm_6_private}:11212 ${vm_7_private}:11212 ${vm_8_private}:11212 -t 16 -r 3 > /dev/null 2>&1 &"

echo
echo "Middleware started"
echo
sleep 5

parallel-ssh \
  -i \
  -H "${vm_1_public} ${vm_2_public} ${vm_3_public}" \
  -l ruxu \
  "rm -rf ~/log; mkdir -p ~/log; nohup ~/libmemcached-1.0.18/clients/memaslap -s ${vm_11_private}:8000 -T 64 -c 64 -o 0.9 -w 1K -t 5400s -S 1s -F ~/scripts/experiments/trace/trace.cfg 1 > ~/log/client.log 2>&1 &"

echo
echo "Clients started"
echo

ssh ruxu@${vm_11_public} "~/scripts/experiments/wait-for-middleware.sh"

parallel-ssh \
  -i \
  -H "${vm_1_public} ${vm_2_public} ${vm_3_public} ${vm_6_public} ${vm_7_public} ${vm_8_public} ${vm_11_public}" \
  -l ruxu \
  "~/scripts/experiments/stop-processes.sh"

echo
echo "VM processes reset"
echo

echo "Downloading log file from middleware ${vm_11_public}"
scp ruxu@${vm_11_public}:~/log/trace.log ${log_dir}/repetition-${repetition}-trace.log
scp ruxu@${vm_11_public}:~/log/error.log ${log_dir}/repetition-${repetition}-error.log
ssh ruxu@${vm_11_public} "rm -rf ~/log"
echo

clients=(${vm_1_public} ${vm_2_public} ${vm_3_public})
for i in ${!clients[@]}
do
  client=${clients[${i}]}
  echo "Downloading log file from client ${client}"
  scp ruxu@${client}:~/log/client.log ${log_dir}/repetition-${repetition}-client-$((i + 1)).log
  ssh ruxu@${client} "rm -rf ~/log"
  echo
done
