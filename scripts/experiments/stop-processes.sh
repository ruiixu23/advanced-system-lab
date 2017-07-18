#!/usr/bin/env bash

pid=$(ps --User=ruxu | grep memcached | awk '{print $1}')

if [ ! -z ${pid} ]
then
  ps --User=ruxu -f
  echo
  echo "Stopping server with pid ${pid}"
  echo
  kill ${pid}
fi

pid=$(ps --User=ruxu | grep java | awk '{print $1}')

if [ ! -z ${pid} ]
then
  ps --User=ruxu -f
  echo
  echo "Stopping middleware with pid ${pid}"
  echo
  kill ${pid}
fi

pid=$(ps --User=ruxu | grep memaslap | awk '{print $1}')

if [ ! -z ${pid} ]
then
  ps --User=ruxu -f
  echo
  echo "Stopping client with pid ${pid}"
  echo
  kill ${pid}
fi
