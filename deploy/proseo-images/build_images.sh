#!/bin/bash
set -e
TAG_SUFFIX="proseo"

#check arg
if [ $# -eq 0 ]
  then
    echo "registry-url is missing..."
    echo "Usage: $0 registry-url"
    echo "* registry-url: a valid docker-registry URL e.g. proseo-registry.eoc.dlr.de"
    exit 1
fi

# check if registry-login is successful

if docker login $1; then
  echo "Registry login successful..."
else
  echo "Could not login to given registry-url"
  exit 1
fi 

cd proseo-components

for component in proseo-*; do
 cd $component
 TAGGED_IMAGENAME=$(cat Dockerfile | grep FROM | awk '{gsub("localhost:5000/",""); split($0,a," "); print a[2]}')-$TAG_SUFFIX
 echo "Building $1/$TAGGED_IMAGENAME"
 docker build -t $1/$TAGGED_IMAGENAME .
 cd ..
done
