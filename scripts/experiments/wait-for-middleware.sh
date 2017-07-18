#!/usr/bin/env bash

start=`date +%s`

pid=$(ps --User=ruxu | grep java | awk '{print $1}')

if [ ! -z ${pid} ]
then
  echo "Waiting for middleware to stop"
  while ps -p ${pid} > /dev/null
  do
    end=`date +%s`
    duration=$((end - start))
    echo "${duration}s"
    if [ "${duration}" -gt 120 ]
    then
      break
    fi
    sleep 5
  done

  end=`date +%s`
  echo "Runtime $((end - start))s"
  echo

  pid=$(ps --User=ruxu | grep java | awk '{print $1}')
  if [ ! -z ${pid} ]
  then
    echo "Wait timeout"
  else
    echo "Middleware stopped"
    echo
  fi
else
  echo "Middleware is not running"
  echo
fi
