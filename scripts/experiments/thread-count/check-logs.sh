#!/usr/bin/env bash

source ./scripts/utils/print-header.sh

thread_pool_size=$1

num_clients_config=(110)
for num_clients in ${num_clients_config[@]}
do
  log_dir=./logs/thread-count/thread-${thread_pool_size}-client-${num_clients}
  echo ${log_dir}
  echo
  cat ${log_dir}/*client*.log | grep  "Global   60" | awk 'NR % 2 == 0'
  echo
  cat ${log_dir}/*client*.log | grep  "Global   60" | awk 'NR % 2 == 0' | wc
  echo
  ls -l ${log_dir} | wc
  cat ${log_dir}/*bash*.log | grep "Wait timeout"
  cat ${log_dir}/*bash*.log | grep "FAILURE"
  cat ${log_dir}/*error*.log
  echo
  sleep 3
done
