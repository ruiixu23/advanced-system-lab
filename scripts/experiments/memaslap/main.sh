#!/usr/bin/env bash

thread_pool_size=4
num_clients=64
log_dir=./logs/memaslap/thread-${thread_pool_size}-client-${num_clients}
mkdir -p ${log_dir}
./scripts/experiments/memaslap/run-repetition.sh ${thread_pool_size} ${num_clients} ${log_dir} | tee -a ${log_dir}/bash.log
