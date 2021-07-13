#!/bin/bash
#
# run_registry.sh
# ---------------
#
# Run a local registry, whose data resides at a well-known file system location, and with image deletion enabled
#

docker run -d \
    -p 5000:5000 \
    --restart=always \
    --name registry \
    -v $HOME/registry:/var/lib/registry \
    -e REGISTRY_STORAGE_DELETE_ENABLED=true \
    registry:2