#!/usr/bin/env bash

base_dir="/als-fall16-project/vagrant"

# Update package manager
sudo apt-get update

# Set up locale
export LANGUAGE=en_US.UTF-8;
export LANG=en_US.UTF-8;
export LC_ALL=en_US.UTF-8;
sudo locale-gen en_US.UTF-8;
sudo dpkg-reconfigure locales;
sudo bash -c "locale > /etc/default/locale";

# Install git
sudo add-apt-repository -y ppa:git-core/ppa
sudo apt-get update
sudo apt-get install -y git

# Install docker
curl -sSL https://get.docker.com/ | sh

# Install jdk
sudo apt-get install -y openjdk-7-jdk

# Install ant
sudo apt-get install -y ant

# Install pssh
sudo apt-get install -y pssh

echo "Provision complete"
