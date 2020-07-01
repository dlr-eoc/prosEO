#!/bin/bash

export WORK_DIR=/prosEO

docker run --rm -it \
  -v "$PWD/kubespray:$WORK_DIR/kubespray" \
  -v "$PWD/clusters:$WORK_DIR/clusters" \
    proseo-k8s-deployer:latest
