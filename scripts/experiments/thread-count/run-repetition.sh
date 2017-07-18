#!/usr/bin/env bash

source ./scripts/utils/print-header.sh
source ./scripts/variables.sh

thread_pool_size=$1
num_clients=$2
log_dir=$3

echo "Thread pool size per server ${thread_pool_size}"
echo "Number of clients per server ${num_clients}"
echo

mkdir -p ${log_dir}

repetitions=(1 2 3 4 5)
for repetition in ${repetitions[@]}
do
  echo "Starting repetition ${repetition}"
  echo

  parallel-ssh \
    -i \
    -H "${vm_4_public} ${vm_5_public} ${vm_6_public} ${vm_7_public} ${vm_8_public} ${vm_9_public} ${vm_10_public}" \
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
    "rm -rf ~/log; mkdir -p ~/log; nohup java -jar ~/middleware-ruxu.jar -l ${vm_11_private} -p 8000 -m ${vm_4_private}:11212 ${vm_5_private}:11212 ${vm_6_private}:11212 ${vm_7_private}:11212 ${vm_8_private}:11212 ${vm_9_private}:11212 ${vm_10_private}:11212 -t ${thread_pool_size} -r 1 > /dev/null 2>&1 &"

  echo
  echo "Middleware started"
  echo
  sleep 5

  parallel-ssh \
    -i \
    -H "${vm_1_public} ${vm_2_public} ${vm_3_public}" \
    -l ruxu \
    "rm -rf ~/log; mkdir -p ~/log; nohup ~/libmemcached-1.0.18/clients/memaslap -s ${vm_11_private}:8000 -T ${num_clients} -c ${num_clients} -w 1K -t 60s -S 1s -F ~/scripts/experiments/thread-count/thread-count.cfg 1 > ~/log/client.log 2>&1 &"

  echo
  echo "Clients started"
  echo

  ssh ruxu@${vm_11_public} "~/scripts/experiments/wait-for-middleware.sh"

  parallel-ssh \
    -i \
    -H "${vm_1_public} ${vm_2_public} ${vm_3_public} ${vm_6_public} ${vm_7_public} ${vm_8_public} ${vm_9_public} ${vm_10_public} ${vm_11_public}" \
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

  echo "Repetition ${repetition} completed"
  echo
  sleep 50
done
