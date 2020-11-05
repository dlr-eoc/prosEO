#!/bin/bash

set -e

CLUSTER="$1"
if [ -z "$CLUSTER" ] ; then
  echo "Usage: make_cluster.sh <clustername>"
fi

# Create cluster directory from kubespray sample
CLUSTERDIR="kubespray/inventory/$CLUSTER"
cp -rf kubespray/inventory/sample "$CLUSTERDIR"
cd "$CLUSTERDIR"
ln -sf ../../contrib
ln -snf ../../../scripts/ansible/roles
cd -

# Create autoenv configuration
CONFIGDIR="$CLUSTERDIR/config"
mkdir -p "$CONFIGDIR"
cp templates/autoenv "$CLUSTERDIR/.autoenv"

# Configure cluster name for bash prompt
cat >"$CONFIGDIR/clustername.sh" <<EOF
export PROSEO_CLUSTER_NAME="$CLUSTER"
EOF

echo "Bare cluster configured in: $CLUSTERDIR"
