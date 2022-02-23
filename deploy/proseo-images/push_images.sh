#!/bin/bash
set -e

TAG_SUFFIX="proseo"

function push_component {
  echo "Pushing $component"
  cd $component
  TAGGED_IMAGENAME=$(cat Dockerfile | grep FROM | awk '{gsub("localhost:5000/",""); split($0,a," "); print a[2]}')-$TAG_SUFFIX
  echo "Image name: $TAGGED_IMAGENAME"
  docker push $REGISTRY/$TAGGED_IMAGENAME
  cd - >/dev/null
}

#check arg
if [[ "$#" -eq 0 || "$#" -gt 2 ]]
  then
    echo "Invalid number of arguments..."
    echo "Usage: $0 <registry-url> [<proseo-component>]"
    echo "* registry-url: a valid docker-registry to push all images to e.g. proseo-registry.eoc.dlr.de"
    echo "* proseo-component: the name of a prosEO component directory"
    exit 1
fi
REGISTRY=$1

if [ ! -z "$2" -a ! -d proseo-components/$2 ] ; then
    echo "$2 is not a prosEO component directory"
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

if [ -z $2 ] ; then
	for component in proseo-*; do
        push_component
	done
else
    component=$2
    push_component
fi
