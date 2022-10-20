#!/bin/bash

cd $(dirname "$0")
docker-compose -p test down -v