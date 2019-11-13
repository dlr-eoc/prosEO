#!/bin/bash
set -e
PROSEO_REVISION=$(git rev-parse --short HEAD)
TAG_SUFFIX="integration-rc1"

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
  echo "registry login successful..."
else
  echo "could not login to given registry-url"
  exit 1
fi 

cd proseo-components

for component in *; do
 cd $component
 TAGGED_IMAGENAME=$(cat Dockerfile | grep FROM | awk '{gsub("localhost:5000/",""); split($0,a," "); print a[2]}')-$TAG_SUFFIX-$PROSEO_REVISION
 echo $1/$TAGGED_IMAGENAME
 docker build -t $1/$TAGGED_IMAGENAME .
 cd ..
done
