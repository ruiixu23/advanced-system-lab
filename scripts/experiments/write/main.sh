#!/usr/bin/env bash

thread_pool_size=16
num_clients=110
write_percent=5

num_servers=3
replication_factor_config=(1 3)
for replication_factor in ${replication_factor_config[@]}
do
  log_dir=./logs/write/server-${num_servers}-replication-${replication_factor}-write-${write_percent}
  mkdir -p ${log_dir}
  ./scripts/experiments/write/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} ${num_servers} ${replication_factor} ${write_percent} | tee -a ${log_dir}/bash.log
done

num_servers=5
replication_factor_config=(1 5)
for replication_factor in ${replication_factor_config[@]}
do
  log_dir=./logs/write/server-${num_servers}-replication-${replication_factor}-write-${write_percent}
  mkdir -p ${log_dir}
  ./scripts/experiments/write/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} ${num_servers} ${replication_factor} ${write_percent} | tee -a ${log_dir}/bash.log
done

num_servers=7
replication_factor_config=(1 7)
for replication_factor in ${replication_factor_config[@]}
do
  log_dir=./logs/write/server-${num_servers}-replication-${replication_factor}-write-${write_percent}
  mkdir -p ${log_dir}
  ./scripts/experiments/write/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} ${num_servers} ${replication_factor} ${write_percent} | tee -a ${log_dir}/bash.log
done
