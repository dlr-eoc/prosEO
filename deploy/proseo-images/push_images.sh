#!/bin/bash
set -e

TAG_SUFFIX="cpros"

#check arg
if [[ "$#" -eq 0 || "$#" -ne 1 ]]
  then
    echo "Invalid number of args..."
    echo "Usage: $0 <registry-url>"
    echo "* registry-url: a valid docker-registry to push all images to e.g. proseo-registry.eoc.dlr.de"
    exit 1
fi

# check if registry-login is successful

if docker login $1; then
  echo "Registry login successful..."
else
  echo "Could not login to given registry-url"
  exit 1
fi 

read -p "Are you sure? " -n 1 -r
if [[ $REPLY =~ ^[Yy]$ ]]
then
    cd proseo-components
    for component in proseo-*; do
      cd $component
      TAGGED_IMAGENAME=$(cat Dockerfile | grep FROM | awk '{gsub("localhost:5000/",""); split($0,a," "); print a[2]}')-$TAG_SUFFIX
      docker push $1/$TAGGED_IMAGENAME
      cd ..
    done
fi


