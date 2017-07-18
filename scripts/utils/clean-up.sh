#!/usr/bin/env bash

source ./scripts/utils/print-header.sh
source ./scripts/variables.sh

parallel-ssh \
  -i \
  -H "${vm_1_public} ${vm_2_public} ${vm_3_public} ${vm_4_public} ${vm_5_public} ${vm_6_public} ${vm_7_public} ${vm_8_public} ${vm_9_public} ${vm_10_public} ${vm_11_public}" \
  -l ruxu \
  "~/scripts/experiments/stop-processes.sh; rm -rf ~/log; ls -l ~"
