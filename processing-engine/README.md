prosEO Processing Facility Deployment
=====================================

This document describes how to deploy a prosEO processing facility on a new set of
(virtual) machines, e. g. as provisioned by some cloud service provider.

# Installation Requirements

The (virtual) machines need to at least fulfil the following requirements:
- Linux installed
- Ability to run Docker and Kubernetes (i. e. standard CentOS kernel, no provider-specific builds)
- TBD GB RAM
- TBD GB online storage
- more TBD
- Optional: Access to some object storage conforming to the AWS S3 protocol


# Installation Process

## Step 1: Install Docker and Kubernetes

For the automated deployment of Kubernetes and Docker on the processing facility using Terraform and Kubespray
see [the documentation for Kubernetes](k8s-deploy/README.md).

You may also want to deploy the Kubernetes Dashboard GUI. The following steps are best performed on the bastion host,
which has a fully configured `kubectl` deployment, when the automated deployment is completed.
1. Create a directory to gather the files:
   ```
   mkdir $HOME/kubernetes-dashboard
   cd $HOME/kubernetes-dashboard
   ```
1. Download the recommended dashboard configuration from
   <https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.0-beta8/aio/deploy/recommended.yaml>.
2. Run the dashboard:
   ```
   kubectl apply -f recommended.yaml
   ```
3. To create a Kubernetes administrator account with full privileges, first put the following lines in `kube-admin.yaml`:
   ```
    apiVersion: v1
    kind: ServiceAccount
    metadata:
      name: admin-user
      namespace: kubernetes-dashboard
    ---
    apiVersion: rbac.authorization.k8s.io/v1
    kind: ClusterRoleBinding
    metadata:
      name: admin-user
    roleRef:
      apiGroup: rbac.authorization.k8s.io
      kind: ClusterRole
      name: cluster-admin
    subjects:
    - kind: ServiceAccount
      name: admin-user
      namespace: kubernetes-dashboard
   ```
   Then create the administrator account:
   ```
   kubectl apply -f kube-admin.yaml
   ```
4. Find the account's secret and copy the token to a safe place (note that there is no line break between the end of the
   token and the prompt for the next shell command):
   ```
   kubectl get secret -n kubernetes-dashboard \
      $(kubectl get serviceaccount admin-user -n kubernetes-dashboard -o jsonpath="{.secrets[0].name}") \
      -o jsonpath="{.data.token}" | base64 --decode 
   ```

You can now access the Kubernetes dashboard from any browser (e. g. on your local workstation) using the URL
<https://your.bastion.host/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/>
clicking on "Token" at the login screen and providing the saved token string as input.


## Step 2: Configure the NFS File Server

The NFS file server is required for providing a file system cache to the Storage Manager on one hand, and to ingest
data into the Storage Manager on the other hand. It is a data link between the "outside world" and the processing
facility.

On the bastion host, perform the following steps:
1. Make sure a sufficiently large disk is attached, say at device `/dev/vdb` with one partition (use `lsblk` to check).
2. Mount the disk at the intended mount point:
   ```
   mkdir -p /exports
   mount /dev/vdb1 /exports
   ```
3. Make the file system mount persistent by adding it to `/etc/fstab`:
   ```
   blkid /dev/vdb1
   # For a GUID volume output is a line including a quoted UUID
   ```
   Add the following line to  `/etc/fstab`:
   ```
   UUID=<detected UUID> /exports defaults 0 2
   ```
4. The NFS server service has already been installed and launched in step 1, but it may be a good idea to relaunch
   it at this point, because the exported directory was not available before:
   ```
   systemctl restart nfs-server.service
   ```


## Step 3: Deploy the prosEO Storage Manager

The steps to deploy the prosEO Storage Manager can be found in the [Storage Manager deployment README](storage-mgr-deploy/README.md).


## Step 4 (optional): Deploy Alluxio as Storage Provider

Currently not supported.


## Step 5 (optional): Deploy Minio as S3 Object Storage Provider

See [Minio documentation](https://docs.min.io/).

For the purpose of this document it is assumed that an S3-compatible object storage is available from the Cloud provider.


## Step 6: Configure kubectl for Access to the New Cluster

Add a new cluster to the kubectl configuration file (usually `$HOME/.kube/config`):
- With nginx proxy (assuming nginx accepts Kubernetes API calls on port 443):
  ```
  kubectl config set-cluster <cluster nickname> --server=<server DNS name without port> --certificate-authority=<path to nginx(!) CA file>
  ```
- Without nginx proxy:
  ```
  kubectl config set-cluster <cluster nickname> --server=<server DNS name>:6443 --certificate-authority=<path to Kubernetes CA file>
  ```
It may be possible that the certificate file needs to be copied from the Nginx/Kubernetes master host.

Next, configure the Kubernetes API user (TODO None of this seems to work currently):
- With certificate authentication:
  ```
  kubectl config set-credentials <user nickname> --client-certificate=<path/to/certfile> --client-key=<path/to/keyfile>
  ```
- With basic authentication (if enabled on Kubernetes API server):
  ```
  kubectl config set-credentials <user nickname> --username=<basic user> --password=<basic password>
  ```
- With a bearer token:
  ```
  kubectl config set-credentials <user nickname> --username=<basic user> --password=<basic password>
  ```

Configure the Kubernetes context:
```
kubectl config set-context <context nickname> --cluster=<cluster nickname> --user=<user nickname>
```


# prosEO Configuration
Add the new storage facility to the prosEO Control Instance configuration:

## Alternative 1: Using Command Line Interface
Run a prosEO CLI connected to the prosEO Control Instance to configure. Log in with
facility management privileges and issue the following command (note the quotes around the
description parameter to include blanks in the description):
```
facility create <facility name> 'description=<any description>' processingEngineUrl=https://<K8S host>:<K8S port>/ storageManagerUrl=http://<K8S host>:<K8S port>/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1 defaultStorageType=[POSIX|S3|ALLUXIO]
```

Alternatively create a JSON file describing the processing facility:
```json
{
  "name" : "<facility name>",
  "description" : "<any description>",
  "processingEngineUrl" : "https://<K8S host>:<K8S port>/",
  "storageManagerUrl" : "http://<K8S host>:<K8S port>/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1",
  "defaultStorageType" : "[POSIX|S3|ALLUXIO]"
}
```

Then in the prosEO CLI issue the command:
```
facility create --file=<JSON file name>
```

## Alternative 2: Using the prosEO GUI
Not yet implemented.


# Example: Setting up a Single-Node Processing Facility
Assuming there is a MacOS or Windows machine available with sufficient RAM and disk capacity, a single-node setup
can be achieved for both the prosEO Control Instance and the prosEO Processing Facility. The following
example shows a setup based on MacOS.

Note that for the following to work the target machine must have unrestricted access
to resources on the Internet.

## Step 1: Install Docker and Kubernetes
Download Docker Desktop from <https://www.docker.com/products/docker-desktop>.

Install and run Docker Desktop and activate Kubernetes as described here:
- For MacOS:
  1) Install and run: <https://docs.docker.com/docker-for-mac/install/>
  2) Activate Kubernetes: <https://docs.docker.com/docker-for-mac/>
- For Windows:
  1) Install and run: <https://docs.docker.com/docker-for-windows/install/>
  2) Activate Kubernetes: <https://docs.docker.com/docker-for-windows/>

Deploy and run a Kubernetes dashboard without requiring a login:
1) Download the recommended dashboard configuration from <https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.0-beta8/aio/deploy/recommended.yaml>
2) Copy the recommended configuration to a new file `kubernetes-dashboard.yaml`.
3) Edit `kubernetes-dashboard.yaml`:
   a) Locate the deployment entry for `kubernetes-dashboard` (around line 170)
   b) Locate the subsection `spec.template.spec.containers.args` and add entries for
      `--enable-skip-login` and `--disable-settings-authorizer`
4) Run the dashboard:
   ```
   kubectl apply -f kubernetes-dashboard.yaml
   kubectl proxy --accept-hosts='.*' &
   ```
5) Access the dashboard at <http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/>.

## Step 2: Deploy the prosEO Storage Manager
For the single-node installation we will use POSIX as the default file system, thereby
avoiding the overhead of running an S3 object storage provider. We will not make Alluxio
available either. This does not place any functional constraints on prosEO, since these
storage options are meant for externally provided installations only, where online storage
is expensive (a multi-TB USB-3 disk on a laptop is not).

1) Locate the file `storage-mgr.yaml` and edit the image reference (around line 20) to point
   to your preferred prosEO repository.
2) Run the Storage Manager:
   ```
   kubectl apply -f <path to file>/storage-mgr.yaml
   ```

## Step 3: Deploy the prosEO Control Instance
Create the prosEO Control Instance from the `single-node-deploy/testdata/docker-compose.yml` file like the following:
```yaml
version: '3'
services:
  proseo-db:
    image: postgres:11
    environment:
      - POSTGRES_DB=proseo
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5432:5432"
  proseo-ingestor:
    image: ${REGISTRY_URL}/proseo-ingestor:${PROSEO_VERSION}
    ports:
      - "8081:8080"
    depends_on:
      - proseo-db
  proseo-order-mgr:
    image: ${REGISTRY_URL}/proseo-order-mgr:${PROSEO_VERSION}
    ports:
      - "8082:8080"
    depends_on:
      - proseo-db
  proseo-prodplanner:
    image: ${REGISTRY_URL}/proseo-planner:${PROSEO_VERSION}
    ports:
      - "8083:8080"
    depends_on:
      - proseo-db
  proseo-processor-mgr:
    image: ${REGISTRY_URL}/proseo-processor-mgr:${PROSEO_VERSION}
    ports:
      - "8084:8080"
    depends_on:
      - proseo-db
  proseo-productclass-mgr:
    image: ${REGISTRY_URL}/proseo-productclass-mgr:${PROSEO_VERSION}
    ports:
      - "8085:8080"
    depends_on:
      - proseo-db
  proseo-user-mgr:
    image: ${REGISTRY_URL}/proseo-user-mgr:${PROSEO_VERSION}
    ports:
      - "8086:8080"
    depends_on:
      - proseo-db
  proseo-facility-mgr:
    image: ${REGISTRY_URL}/proseo-facility-mgr:${PROSEO_VERSION}
    ports:
      - "8087:8080"
    depends_on:
      - proseo-db
  proseo-gui:
    image: ${REGISTRY_URL}/proseo-ui-gui:${PROSEO_VERSION}
    ports:
      - "8088:8080"
    depends_on:
      - proseo-ingestor
      - proseo-prodplanner
      - proseo-order-mgr
      - proseo-processor-mgr
      - proseo-productclass-mgr
  proseo-pgadmin:
    image: dpage/pgadmin4
    environment:
      - PGADMIN_ENABLE_TLS=True
      - PGADMIN_DEFAULT_EMAIL=${PGADMIN_EMAIL}
      - PGADMIN_DEFAULT_PASSWORD=${PGADMIN_PASSWORD}
    volumes:
      - "./proseo-components/proseo-pgadmin/certs/proseo-selfsigned.crt:/certs/server.cert"
      - "./proseo-components/proseo-pgadmin/certs/proseo-selfsigned.key:/certs/server.key"
    ports:
      - "8443:443"
    depends_on:
      - proseo-db
volumes:
  pgdata:
networks:
  default:
    driver: bridge
    ipam:
      config:
        - subnet: 172.177.57.0/24
```

To run the Control Instance issue the following commands from the directory, where the
`docker-compose.yml` file resides:
```
export REGISTRY_URL=<your preferred prosEO repository>
export PROSEO_VERSION=<the prosEO version to install>
export POSTGRES_PASSWORD=<password for postgres user as configured in prosEO images>
export PGADMIN_EMAIL=<email address for pgAdmin authentication>
export PGADMIN_PASSWORD=<some password for pgAdmin authentication>
docker-compose up -d
```

The `single-node-deploy/testdata` directory contains convenience scripts for these steps
and a prepared `docker-compose.yml` file.

# Step 4: Setup the Kubernetes Cluster with Storage Manager and File System Cache
This step requires configuring a "host path" file server to serve the common storage area
to both the Storage Manager and the Processing Engine. Assuming a configuration as in the files
given in the example directory `single-node-deploy`, the following commands must be issued
(also available as part of the script `single-node-deploy/testdata/create_data_local.sh`):
```sh
# Define actual path to storage area
SHARED_STORAGE_PATH=<path on Docker Desktop host>

# Update the path in the Persistent Volume configuration
sed "s|%SHARED_STORAGE_PATH%|${SHARED_STORAGE_PATH}|" <nfs-pv.yaml.template >nfs-pv.yaml

# Create the Persistent Volumes
kubectl apply -f nfs-pv.yaml

# Create the export directories
mkdir -p ${SHARED_STORAGE_PATH}/proseodata ${SHARED_STORAGE_PATH}/transfer

# Create the Storage Manager
kubectl apply -f storage-mgr-local.yaml
```


# Step 5: Install and Run the prosEO Command Line Interface
The current version of the prosEO Command Line Interface (CLI) can be downloaded from
<https://proseo-registry.eoc.dlr.de/artifactory/proseo/proseo-ui-cli.jar>. The CLI must
be configured with a YAML file to be able to connect to the prosEO Control Instance.
A sample configuration file can be downloaded from <https://proseo-registry.eoc.dlr.de/artifactory/proseo/application.yml.sample>.
The settings in this file are suitable for the Docker Compose configuration shown above.

Start the CLI with `java -jar proseo-ui-cli.jar`. On a newly created prosEO Control Instance
a default user `sysadm` (password `sysadm`) is provided.

To initialize the prosEO instance, run the following CLI commands:
```
login -usysadm -psysadm
user update sysadm password=<new password>
create mission PTM 'name=prosEO Test Mission'
```

# Step 6: Configure the prosEO Processing Facility
Create a JSON file `facility.json` describing the local processing facility:
```
{
  "name" : "localhost",
  "description" : "Docker Desktop Minikube",
  "processingEngineUrl" : "http://host.docker.internal:8001/",
  "processingEngineUser" : "kubeuser1",
  "processingEnginePassword" : "very-secret-password",
  "storageManagerUrl" : "http://host.docker.internal:8001/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1",
  "localStorageManagerUrl" : "http://storage-mgr-service.default.svc.cluster.local:3000/proseo/storage-mgr/v0.1",
  "storageManagerUser" : "smuser",
  "storageManagerPassword" : "smpwd-but-that-would-be-way-too-short",
  "defaultStorageType" : "POSIX"
}
```

On the prosEO CLI issue the following commands:
```
login -usysadm -p<your password> PTM
facility create --file=facility.json
```

You are now ready to configure your first mission. See <https://github.com/dlr-eoc/prosEO/tree/master/samples/testdata> for
an example, which works with the prosEO Sample Processor. Test input data and processing orders
can be generated with the above mentioned script `single-node-deploy/testdata/create_data_local.sh`.
Note that this script deliberately creates an order set, which does not result in a fully completed
processing (one job will remain in `RELEASED` state due to missing input, this requires generation
of an additional order to process from L0 to L2A/B data - this is left as an exercise to the reader ;-) ).