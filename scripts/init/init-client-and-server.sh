#!/usr/bin/env bash

echo "Update package manager"
sudo apt-get update

# Set up locale
echo "Setup locale"
export LANGUAGE=en_US.UTF-8
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
sudo locale-gen en_US.UTF-8
sudo dpkg-reconfigure locales
sudo bash -c "locale > /etc/default/locale"

echo "Install build tools, memcached and memaslap"
sudo apt-get install -y build-essential libevent-dev memcached
rm -rf libmemcached-1.0.18
wget https://launchpad.net/libmemcached/1.0/1.0.18/+download/libmemcached-1.0.18.tar.gz
tar xvf libmemcached-1.0.18.tar.gz
cd libmemcached-1.0.18
export LDFLAGS=-lpthread
./configure --enable-memaslap && make clients/memaslap

echo "Setup complete"
echo
