#!/usr/bin/env bash

thread_pool_size=16
num_clients_config=(110)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/thread-count/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/thread-count/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=32
num_clients_config=(110)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/thread-count/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/thread-count/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=64
num_clients_config=(110)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/thread-count/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/thread-count/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=128
num_clients_config=(110)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/thread-count/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/thread-count/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done
