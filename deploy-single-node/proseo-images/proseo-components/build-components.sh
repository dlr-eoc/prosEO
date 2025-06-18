#!/bin/bash
# Title           : build-components.sh
# Description     : This script performs the following tasks:
#                  1) Creates `application.yml` files with environment 
#                  variables replaced from the corresponding
#                  `application.yml.template` files.
#                  2) Builds Docker images using the correct registry URL
#                  and component name.
# Author          : 
# Date            : 2025_06_16
# Version         : 0.1
# Usage           : bash build-components.sh
#                   ./build-components.sh
# Dependencies    : envsubst, docker
#==============================================================================

set -e

# Ensure REGISTRY_URL environment variable is set
if [ -z "${REGISTRY_URL}" ]; then
    echo "Error: REGISTRY_URL environment variable is not set."
    exit 1
fi

# Loop over each component directory (assuming directories are named proseo-*)
for component in proseo-*; do
    # Navigate into the component directory
    cd "${component}" || { echo "Failed to cd into ${component}"; exit 1; }

    # Replace environment variables in the application.yml file using envsubst
    if [ -f "application.yml.template" ]; then
        envsubst < application.yml.template > application.yml
    else
        echo "Warning: No application.yml.template found in ${component}. Skipping."
        # continue
    fi

    # Extract the component name from the Dockerfile (after the FROM statement)
    COMPONENT_NAME=$(cat Dockerfile | grep FROM | awk '{gsub("localhost:5000/",""); split($0,a," "); print a[2]}')
    
    # Check if the COMPONENT_NAME was extracted correctly
    if [ -z "${COMPONENT_NAME}" ]; then
        echo "Error: Unable to extract component name from Dockerfile in ${component}."
        # cd - || exit
        exit 1
        # continue
    fi

    # Tag the Docker image with the correct registry URL and component name
    TAGGED_NAME="${REGISTRY_URL}/${COMPONENT_NAME}-proseo"
    
    # Build and push the Docker image
    docker build -t "${TAGGED_NAME}" .
    if [ $? -ne 0 ]; then
        echo "Error: Docker build failed for ${component}."
        # cd - || exit
        exit 1
        # continue
    fi

    # Push the Docker image to the registry
    docker push "${TAGGED_NAME}"
    if [ $? -ne 0 ]; then
        echo "Error: Docker push failed for $TAGGED_NAME."
        # cd - || exit
        exit 1
        # continue
    fi

    # Navigate back to the previous directory
    # cd - || exit
    cd -

done
