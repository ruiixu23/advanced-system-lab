#!/usr/bin/env bash

# Docker commands

sudo docker run --rm -it --name azure microsoft/azure-cli bash

# Azure commands

azure login

azure group list

azure vm list

azure vm start -v For_ASL foraslvms1 &
azure vm start -v For_ASL foraslvms2 &
azure vm start -v For_ASL foraslvms3 &
azure vm start -v For_ASL foraslvms4 &
azure vm start -v For_ASL foraslvms5 &
azure vm start -v For_ASL foraslvms6 &
azure vm start -v For_ASL foraslvms7 &
azure vm start -v For_ASL foraslvms8 &
azure vm start -v For_ASL foraslvms9 &
azure vm start -v For_ASL foraslvms10 &
azure vm start -v For_ASL foraslvms11 &

azure vm deallocate For_ASL foraslvms1 &
azure vm deallocate For_ASL foraslvms2 &
azure vm deallocate For_ASL foraslvms3 &
azure vm deallocate For_ASL foraslvms4 &
azure vm deallocate For_ASL foraslvms5 &
azure vm deallocate For_ASL foraslvms6 &
azure vm deallocate For_ASL foraslvms7 &
azure vm deallocate For_ASL foraslvms8 &
azure vm deallocate For_ASL foraslvms9 &
azure vm deallocate For_ASL foraslvms10 &
azure vm deallocate For_ASL foraslvms11 &
