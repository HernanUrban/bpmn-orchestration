#!/bin/bash

cd $(dirname "$0")
export HOST_APPLICATION="host.docker.internal"
export HOST_DOCKER_ENGINE="localhost"
#Local resources (camunda flows)
export RESOURCES="../resources"

docker-compose -p test up -d --build --remove-orphans