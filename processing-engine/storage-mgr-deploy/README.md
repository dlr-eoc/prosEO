prosEO Storage Manager Deployment
=================================

## Prerequisites

The following instructions assume that a working `kubectl` configuration for your cluster (including all the necessary credentials)
is configured at the deployment host.

Furthermore on the local (development) workstation a Docker instance and a local registry (at `localhost:5000`) is assumed,
as is a production registry at `<registry-url>`.


## Build prosEO Storage Manager

The Storage Manager Docker image is built and pushed to a local (development) registry as part of the Maven `install` phase.
We assume this has been performed, and the image is now available at `localhost:5000/proseo-storage-mgr:<module version>`.

Execute the following commands on the local workstation to tag and push the image to the production environment: 
1. Login to production registry (if not already done):
   ```sh
   docker login <registry-url>
   ```
2. Re-tag and push the Docker image:
   ```sh
   docker tag localhost:5000/proseo-storage-mgr:<module version> <registry-url>/proseo-storage-mgr:latest
   docker push <registry-url>/proseo-storage-mgr:latest
   ```

## Deploy Storage Manager in Kubernetes

The following steps need to be performed:

1. Create a Kubernetes secret holding the credentials for the private registry on some host, which has both `kubectl` and Docker
   (not Docker Desktop!) configured (e. g. the Kubernetes master node):
   ```sh
   docker login <registry-url>
   # Check if docker client file is under ~/.docker/config.json, and make sure it actually contains the desired credentials
   cat ~/.docker/config.json
   # Create Kubernetes secret
   kubectl create secret generic proseo-regcred  --from-file=.dockerconfigjson=$HOME/.docker/config.json --type=kubernetes.io/dockerconfigjson
   ```
   See also: <https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/>

2. Deploy the Storage Manager service using the files in `storage-mgr-deploy/kubernetes` (from any host with a suitably configured
   `kubectl`, including the local development workstation):
   ```sh
   cd kubernetes
   sed "s/proseo-nfs-server.default.svc.cluster.local/<bastion host IP>/" <nfs-pv.yaml.template >nfs-pv.yaml
   kubectl apply -f nfs-pv.yaml
   kubectl apply -f storage-mgr.yaml
   ```
    
    