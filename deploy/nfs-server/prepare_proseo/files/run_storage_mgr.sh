#!/bin/bash
set -e

if [[ "$#" -ne 2 ]]
  then
    echo "Invalid number of args..."
    echo "Usage: $0 <registry-url> <version>"
    echo "* registry-url: a valid docker-registry URL where the referenced docker-images are registered"
    echo ""
    echo "-->used compose file is:"
    cat docker-compose.yml
    exit 1
fi

# check if registry-login is successful

if docker login $1; then
  echo "registry login successful..."
else
  echo "could not login to given registry-url"
  exit 1
fi

export REGISTRY_URL=$1
export VERSION=$2

docker-compose -p proseo pull
docker-compose -p proseo up -d
