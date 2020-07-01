#!/bin/bash

export WORK_DIR=/prosEO

docker run --rm -it \
  -v "$PWD/kubespray:$WORK_DIR/kubespray" \
    proseo-k8s-deployer:latest
