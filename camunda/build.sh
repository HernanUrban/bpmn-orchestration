#!/bin/bash
set -e

./gradlew build copyDependencies
docker build -t camunda:local-build .
