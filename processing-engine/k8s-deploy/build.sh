#!/bin/bash
set -e

BUILD=$(git rev-parse --short HEAD)

YELLOW='\033[1;33m'
NC='\033[0m' # No Color


if [ -f terraform/.ostackrc -a -f ssh-keys/cluster-key.pem -a -f ssh-keys/cluster-key.pub ];then
  docker build -t proseo-k8s-deployer:$BUILD .
  echo -e ${YELLOW}"**build finished -> proseo-k8s-deployer:$BUILD"${NC}
  cat >run.sh <<EOF
#!/bin/bash
docker run --rm -it -v $(pwd)/terraform/state:/terraform/state proseo-k8s-deployer:$BUILD
EOF
  chmod +x run.sh
else
  echo "one or more of terraform/.ostackrc, ssh-keys/cluster-key.pem or ssh-keys/cluster-key.pub is missing..."
fi
