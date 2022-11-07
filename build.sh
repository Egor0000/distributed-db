#!/usr/bin/env bash

DOCKER_BUILDKIT=1
docker build --network=host -t distributed-db:latest -f Dockerfile2 .
docker rmi -f $(docker images -f "dangling=true" -q)
docker save distributed-db

