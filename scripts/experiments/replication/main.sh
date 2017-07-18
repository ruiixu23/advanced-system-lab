#!/usr/bin/env bash

thread_pool_size=16
num_clients=110

num_servers=1
replication_factor_config=(1)
for replication_factor in ${replication_factor_config[@]}
do
  log_dir=./logs/replication/server-${num_servers}-replication-${replication_factor}
  mkdir -p ${log_dir}
  ./scripts/experiments/replication/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} ${num_servers} ${replication_factor} | tee -a ${log_dir}/bash.log
done

num_servers=3
replication_factor_config=(1 2 3)
for replication_factor in ${replication_factor_config[@]}
do
  log_dir=./logs/replication/server-${num_servers}-replication-${replication_factor}
  mkdir -p ${log_dir}
  ./scripts/experiments/replication/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} ${num_servers} ${replication_factor} | tee -a ${log_dir}/bash.log
done

num_servers=5
replication_factor_config=(1 3 5)
for replication_factor in ${replication_factor_config[@]}
do
  log_dir=./logs/replication/server-${num_servers}-replication-${replication_factor}
  mkdir -p ${log_dir}
  ./scripts/experiments/replication/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} ${num_servers} ${replication_factor} | tee -a ${log_dir}/bash.log
done

num_servers=7
replication_factor_config=(1 4 7)
for replication_factor in ${replication_factor_config[@]}
do
  log_dir=./logs/replication/server-${num_servers}-replication-${replication_factor}
  mkdir -p ${log_dir}
  ./scripts/experiments/replication/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} ${num_servers} ${replication_factor} | tee -a ${log_dir}/bash.log
done
