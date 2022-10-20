#!/bin/bash

cd $(dirname "$0")
docker-compose -p camunda down -v --remove-orphans