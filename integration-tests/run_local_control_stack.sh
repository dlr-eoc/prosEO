#!/bin/bash
set -e

if [[ "$#" -eq 0 || "$#" -ne 2 ]]
  then
    echo "Invalid number of args..."
    echo "Usage: $0 <registry-url> <proseo-revison>"
    echo "* registry-url: a valid docker-registry URL where the referenced docker-images are registered"
    echo "* proseo-revison: a valid git-revision"
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
export PROSEO_REVISION=$2

docker-compose up -d
