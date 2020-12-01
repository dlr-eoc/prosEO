#!/bin/bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SOURCE_DIR="$(dirname "${SCRIPT_DIR}")"

TAGNAME=proseo-k8s-deployer
BUILD=$(git rev-parse --short HEAD)

cd "${SOURCE_DIR}"
docker build \
  -f "${SCRIPT_DIR}/Dockerfile.simple" \
  -t "${TAGNAME}:${BUILD}" \
  -t "${TAGNAME}:latest" \
  .
