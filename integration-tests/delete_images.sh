#!/bin/bash
#
# delete_images.sh
# ----------------
#
# Delete prosEO integration images with given commit ID and repository
#
REPO=$1
COMMIT=$2

if [ x$REPO = x -o x$COMMIT = x ] ; then
  echo "Usage: $0 <repository> <commit ID>
  exit 1
fi

docker image rm ${REPO}/proseo-user-mgr:0.5.2-integration-rc1-${COMMIT}
docker image rm ${REPO}/proseo-ui-gui:0.0.1-SNAPSHOT-integration-rc1-${COMMIT}
docker image rm ${REPO}/proseo-productclass-mgr:0.5.2-integration-rc1-${COMMIT}
docker image rm ${REPO}/proseo-storage-mgr:0.5.2-integration-rc1-${COMMIT}
docker image rm ${REPO}/proseo-processor-mgr:0.5.2-integration-rc1-${COMMIT}
docker image rm ${REPO}/proseo-planner:0.5.2-integration-rc1-${COMMIT}
docker image rm ${REPO}/proseo-order-mgr:0.5.2-integration-rc1-${COMMIT}
docker image rm ${REPO}/proseo-ingestor:0.5.2-integration-rc1-${COMMIT}
docker image rm ${REPO}/proseo-api-prip:0.5.2-integration-rc1-${COMMIT}
docker image rm ${REPO}/proseo-facility-mgr:0.5.2-integration-rc1-${COMMIT}
docker image rm ${REPO}/dpage/pgadmin4:2019-11-12-2-integration-rc1-${COMMIT}
docker image rm ${REPO}/postgres:11-integration-rc1-${COMMIT}

