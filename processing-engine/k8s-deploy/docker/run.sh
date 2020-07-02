#!/bin/bash

export WORK_DIR=/prosEO

docker run --rm -it \
  -v "$PWD/kubespray:$WORK_DIR/kubespray" \
  -v "$PWD/scripts:$WORK_DIR/scripts" \
    proseo-k8s-deployer:latest
