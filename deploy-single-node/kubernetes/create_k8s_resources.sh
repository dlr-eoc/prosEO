#!/bin/bash
#
# create_k8s_resources.sh
# --------------------
#
# Usage: create_k8s_resources.sh <Storage Manager image tag> <path to shared storage>
#
# Create Kubernetes services on a local Docker Desktop instance
#

# -------------------------
# Check parameters
# -------------------------
STORAGE_MGR_TAG=$1
SHARED_STORAGE_PATH=$2

if [ x$STORAGE_MGR_TAG = x -o x$SHARED_STORAGE_PATH = x ] ; then
	echo "Usage: $0 <Storage Manager image tag> <path to shared storage>"
	exit 1
fi

# -------------------------
# Tag Storage Manager image
# -------------------------
docker tag localhost:5000/proseo-storage-mgr:$STORAGE_MGR_TAG localhost:5000/proseo-storage-mgr:latest
docker push localhost:5000/proseo-storage-mgr:latest

# -------------------------
# Prepare local file server
# -------------------------

# File server is on "hostPath"
# Update the path in the Persistent Volume configuration
# sed "s|%SHARED_STORAGE_PATH%|${SHARED_STORAGE_PATH}|" <../kubernetes/nfs-pv.yaml.template >../kubernetes/nfs-pv.yaml
# Create the Persistent Volumes
kubectl apply -f nfs-pv.yaml

# Simulated "internal" POSIX storage area (must correspond to the specs in nfs-server-local.yaml)
mkdir -p ${SHARED_STORAGE_PATH}/proseodata

# Simulated "external" mount point for product ingestion (must correspond to the specs in nfs-server-local.yaml)
mkdir -p ${SHARED_STORAGE_PATH}/transfer

# -------------------------
# Create Storage Manager
# -------------------------

# Create the storage manager in the local Minikube
kubectl apply -f storage-mgr-local.yaml


# -------------------------
# Create Kubernetes Dashboard
# -------------------------

# Create a dashboard at http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
# kubectl apply -f ../kubernetes/kubernetes-dashboard.yaml
# kubectl proxy --accept-hosts='.*' &

