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
    -H "${vm_1_public}" \
    -l ruxu \
    "memcached -d -p 11212 -t 1 -m 512"

  echo
  echo "Servers started"
  echo
  sleep 5

  parallel-ssh \
    -i \
    -H "${vm_1_public}" \
    -l ruxu \
    "rm -rf ~/log; mkdir -p ~/log; nohup java -jar ~/middleware-ruxu.jar -l 127.0.0.1 -p 8000 -m 127.0.0.1:11212 -t ${thread_pool_size} -r 1 > /dev/null 2>&1 &"

  echo
  echo "Middleware started"
  echo
  sleep 5

  parallel-ssh \
    -i \
    -H "${vm_1_public}" \
    -l ruxu \
    "nohup ~/libmemcached-1.0.18/clients/memaslap -s 127.0.0.1:8000 -T ${num_clients} -c ${num_clients} -w 1K -o 0.9 -t 60s -S 1s -F ~/scripts/experiments/memaslap/memaslap.cfg 1 > ~/log/client.log 2>&1 &"

  echo
  echo "Clients started"
  echo

  ssh ruxu@${vm_1_public} "~/scripts/experiments/wait-for-middleware.sh"

  parallel-ssh \
    -i \
    -H "${vm_1_public}" \
    -l ruxu \
    "~/scripts/experiments/stop-processes.sh"

  echo
  echo "VM processes reset"
  echo

  echo "Downloading log file from virtual machine ${vm_1_public}"
  scp ruxu@${vm_1_public}:~/log/trace.log ${log_dir}/repetition-${repetition}-trace.log
  scp ruxu@${vm_1_public}:~/log/error.log ${log_dir}/repetition-${repetition}-error.log
  scp ruxu@${vm_1_public}:~/log/client.log ${log_dir}/repetition-${repetition}-client.log
  ssh ruxu@${vm_1_public} "rm -rf ~/log"
  echo

  echo "Repetition ${repetition} completed"
  echo
  sleep 50
done
