#!/bin/bash
#
# delete_images.sh
# ----------------
#
# Delete prosEO integration images with given commit ID and repository
#
REPO=$1
VERSION=$2

if [ x$REPO = x -o x$VERSION = x ] ; then
  echo "Usage: $0 <repository> <version>
  exit 1
fi

docker image rm ${REPO}/proseo-user-mgr:${VERSION}-proseo
docker image rm ${REPO}/proseo-ui-gui:${VERSION}-proseo
docker image rm ${REPO}/proseo-productclass-mgr:${VERSION}-proseo
docker image rm ${REPO}/proseo-storage-mgr:${VERSION}-proseo
docker image rm ${REPO}/proseo-processor-mgr:${VERSION}-proseo
docker image rm ${REPO}/proseo-planner:${VERSION}-proseo
docker image rm ${REPO}/proseo-order-mgr:${VERSION}-proseo
docker image rm ${REPO}/proseo-ingestor:${VERSION}-proseo
docker image rm ${REPO}/proseo-api-prip:${VERSION}-proseo
docker image rm ${REPO}/proseo-facility-mgr:${VERSION}-proseo
docker image rm ${REPO}/dpage/pgadmin4:latest-proseo
docker image rm ${REPO}/postgres:11-proseo

