#!/bin/bash

cd $(dirname "$0")
docker-compose -p camunda up -d --build --remove-orphans