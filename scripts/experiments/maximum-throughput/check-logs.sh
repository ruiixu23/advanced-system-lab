#!/usr/bin/env bash

source ./scripts/utils/print-header.sh

thread_pool_size=$1

num_clients_config=(4 7 10 20 30 40 50 60 70 80 90 100 110 120 130 140 150 160 190)
for num_clients in ${num_clients_config[@]}
do
  echo "${num_clients}"
  echo
  log_dir=./logs/maximum-throughput/thread-${thread_pool_size}-client-${num_clients}
  cat ${log_dir}/*client*.log | grep  "Global   60" | awk 'NR % 2 == 0'
  echo
  cat ${log_dir}/*client*.log | grep  "Global   60" | awk 'NR % 2 == 0' | wc
  echo
  ls -l ${log_dir} | wc
  cat ${log_dir}/*bash*.log | grep "Wait timeout"
  cat ${log_dir}/*bash*.log | grep "FAILURE"
  cat ${log_dir}/*error*.log
  echo
  sleep 1
done
