#!/usr/bin/env bash

thread_pool_size=4
num_clients_config=(10 40 70 100 130 160 190)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/maximum-throughput/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/maximum-throughput/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=8
num_clients_config=(10 40 70 100 130 160 190)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/maximum-throughput/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/maximum-throughput/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=12
num_clients_config=(10 40 70 100 130 160 190)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/maximum-throughput/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/maximum-throughput/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=16
num_clients_config=(10 40 70 100 130 160 190)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/maximum-throughput/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/maximum-throughput/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=20
num_clients_config=(10 40 70 100 130 160 190)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/maximum-throughput/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/maximum-throughput/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=24
num_clients_config=(10 40 70 100 130 160 190)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/maximum-throughput/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/maximum-throughput/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=12
num_clients_config=(4 7 20 30 50 60 80 90 110 120)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/maximum-throughput-more/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/maximum-throughput/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=16
num_clients_config=(4 7 20 30 50 60 80 90 110 120)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/maximum-throughput-more/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/maximum-throughput/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done

thread_pool_size=20
num_clients_config=(4 7 10 20 30 40 50 60 70 80 90 100 110 120 130)
for num_clients in ${num_clients_config[@]}
do
 log_dir=./logs/maximum-throughput-more/thread-${thread_pool_size}-client-${num_clients}
 mkdir -p ${log_dir}
 ./scripts/experiments/maximum-throughput/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
done
