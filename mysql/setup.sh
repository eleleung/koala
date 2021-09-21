#!/bin/bash

{ [ $(id -u) -eq 0 ]; } || { echo "execute as root"; exit; }

docker run --rm --publish 3306:3306 --name mysql -e MYSQL_ROOT_PASSWORD=my-secret-pw -d mysql:8 2> /dev/null;

SECONDS=0

until mysql -u root -h 127.0.0.1 -pmy-secret-pw 2> /dev/null;
do
  ((SECONDS < 120)) || exit;
  sleep 1;
done
