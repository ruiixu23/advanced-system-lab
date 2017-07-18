#!/usr/bin/env bash

# Public addresses for the vms
vm_1_public="ruxuforaslvms1.westeurope.cloudapp.azure.com"
vm_2_public="ruxuforaslvms2.westeurope.cloudapp.azure.com"
vm_3_public="ruxuforaslvms3.westeurope.cloudapp.azure.com"
vm_4_public="ruxuforaslvms4.westeurope.cloudapp.azure.com"
vm_5_public="ruxuforaslvms5.westeurope.cloudapp.azure.com"
vm_6_public="ruxuforaslvms6.westeurope.cloudapp.azure.com"
vm_7_public="ruxuforaslvms7.westeurope.cloudapp.azure.com"
vm_8_public="ruxuforaslvms8.westeurope.cloudapp.azure.com"
vm_9_public="ruxuforaslvms9.westeurope.cloudapp.azure.com"
vm_10_public="ruxuforaslvms10.westeurope.cloudapp.azure.com"
vm_11_public="ruxuforaslvms11.westeurope.cloudapp.azure.com"

# Private addresses for the vms
vm_1_private="10.0.0.14"
vm_2_private="10.0.0.11"
vm_3_private="10.0.0.6"
vm_4_private="10.0.0.8"
vm_5_private="10.0.0.7"
vm_6_private="10.0.0.12"
vm_7_private="10.0.0.9"
vm_8_private="10.0.0.5"
vm_9_private="10.0.0.4"
vm_10_private="10.0.0.10"
vm_11_private="10.0.0.13"

# Public addresses of the client and server vms as an array
clients_and_servers=( \
  ${vm_1_public} \
  ${vm_2_public} \
  ${vm_3_public} \
  ${vm_4_public} \
  ${vm_5_public} \
  ${vm_6_public} \
  ${vm_7_public} \
  ${vm_8_public} \
  ${vm_9_public} \
  ${vm_10_public} \
)

# Public addresses of the vms as an array
vms=( \
  ${vm_1_public} \
  ${vm_2_public} \
  ${vm_3_public} \
  ${vm_4_public} \
  ${vm_5_public} \
  ${vm_6_public} \
  ${vm_7_public} \
  ${vm_8_public} \
  ${vm_9_public} \
  ${vm_10_public} \
  ${vm_11_public} \
)

