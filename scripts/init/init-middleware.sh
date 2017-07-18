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

sudo apt-get install -y openjdk-7-jdk

echo "Middleware setup complete"
echo
