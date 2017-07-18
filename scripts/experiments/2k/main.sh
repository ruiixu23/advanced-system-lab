#!/usr/bin/env bash

thread_pool_size=16
num_clients=110
num_servers=5
replication_factor_config=(1 5)
write_percent_config=(1 10)
message_size_config=(128 1024)

for replication_factor in ${replication_factor_config[@]}
do
  for write_percent in ${write_percent_config[@]}
  do
    for message_size in ${message_size_config[@]}
    do
      log_dir=./logs/2k/server-${num_servers}-replication-${replication_factor}-write-${write_percent}-message-${message_size}
      mkdir -p ${log_dir}
      ./scripts/experiments/2k/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} ${num_servers} ${replication_factor} ${write_percent} ${message_size} | tee -a ${log_dir}/bash.log
    done
  done
done
