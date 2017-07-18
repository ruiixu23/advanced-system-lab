#!/usr/bin/env bash

source ./scripts/utils/print-header.sh

mkdir -p ./log
rm ./log/*

sudo docker stop s1
sudo docker rm -v s1
sudo docker run -d -p 11211:11211 --name=s1 memcached -m 128 -t 1

sudo docker stop s2
sudo docker rm -v s2
sudo docker run -d -p 11212:11211 --name=s2 memcached -m 128 -t 1

sudo docker stop s3
sudo docker rm -v s3
sudo docker run -d -p 11213:11211 --name=s3 memcached -m 128 -t 1

echo "Servers started"
echo
sleep 2

nohup java -jar ./dist/middleware-ruxu.jar -l 127.0.0.1 -p 8000 -m 127.0.0.1:11211 127.0.0.1:11212 127.0.0.1:11213 -t 8 -r 1 > /dev/null 2>&1 &
echo $! > ./log/middleware.pid

echo "Middleware started"
echo
sleep 2

nohup ./libmemcached-1.0.18/clients/memaslap -s 127.0.0.1:8000 -T 60 -c 60 -w 1K -t 60s -S 1s -F ./scripts/experiments/localhost/localhost.cfg 1 > ./log/client.log 2>&1 &
echo $! > ./log/client.pid

echo "Clients started"
echo

start=`date +%s`

pid=$(ps --User=vagrant | grep java | awk '{print $1}')

if [ ! -z ${pid} ]
then
  while ps -p ${pid} > /dev/null
  do
    end=`date +%s`
    duration=$((end - start))
    echo "${duration}s"
    sleep 5
  done

  echo "Middleware stopped"
  echo
else
  echo "Middleware is not running"
  echo
fi

end=`date +%s`
echo "Runtime $((end-start))s"
echo
