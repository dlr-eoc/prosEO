Example: Setting up a Single-Node Processing Facility
=====================================================

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

*Caution:* Configurations and version numbers below are based on Docker Desktop 4.15.0 and Kubernetes 1.25.2. Newer (or older) 
versions may require modified approaches.

Deploy and run a Kubernetes dashboard:
1) Download the recommended dashboard configuration from 
   <https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml>
   to a new file `kubernetes/kubernetes-dashboard.yaml` (a copy may already be provided in the `kubernetes` directory).
2) Run the dashboard:
   ```
   kubectl apply -f kubernetes/kubernetes-dashboard.yaml
   nohup kubectl proxy --accept-hosts='.*' &
   ```
3) Create a user with administrative privileges for the dashboard as per
   <https://github.com/kubernetes/dashboard/blob/master/docs/user/access-control/creating-sample-user.md> and (from Kubernetes 1.22)
   an associated secret as per 
   <https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/#manually-create-a-long-lived-api-token-for-a-serviceaccount>
   (you may use the configuration provided in the file `kubernetes/kube-admin.yaml`).
4) Retrieve the secret token for this user:
   ```
   kubectl describe secret/$(kubectl get secrets --namespace kube-system | grep admin-user | cut -d ' ' -f 1) --namespace kube-system
   ```
5) Access the dashboard at <http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/>
   using the token retrieved in step 4.


## Step 2: Build the prosEO Control Instance

First build prosEO from source code as described in `<project root>/README.md`. Thereafter, create specific images for your
environment using the Dockerfiles and `application.yml.template` files in `proseo-images/proseo-components/*`. Create files
named `application.yml` by copying from `application.yml.template` and adapt the following items:

- In all files: `spring.datasource.password` (must match the PostgreSQL password used in step 3 below)
- In `proseo-ingestor/application.yml` and `proseo-storage-mgr/application.yml`: `proseo.storageManager.secret`
  (must be the same in both files)
- In `proseo-planner/application.yml`: `proseo.wrapper.password` (must match the setting for the `wrapper` user in the
  mission configuration)

Other parameters (esp. logging settings) may be changed as deemed suitable for the installation at hand.

When all `application.yml` files have been created, for each component a configured image (containing the update `application.yml`
file) must be created and pushed to the Docker registry to be used:
```
export REGISTRY_URL=<your preferred prosEO repository, e. g. localhost:5000>

cd proseo-images/proseo-components

for component in proseo-* ; do
  cd $component

  COMPONENT_NAME=$(cat Dockerfile | grep FROM | awk '{gsub("localhost:5000/",""); split($0,a," "); print a[2]}')
  TAGGED_NAME=${REGISTRY_URL}/${COMPONENT_NAME}-proseo

  docker build -t ${TAGGED_NAME}
  docker push ${TAGGED_NAME}

  cd -
done
```
The `proseo-images` directory may be populated with convenience scripts for these steps (see `proseo-images/README.md`).


## Step 3: Deploy the prosEO Control Instance

Create the prosEO Control Instance from a `docker-compose.yml` file. A `docker-compose.yml.template`
file is provided in the `proseo-images` directory, a simplified file would look something like the following:
```yaml
version: '3'
services:
  proseo-db:
    image: ${REGISTRY_URL}/postgres:11-proseo
    environment:
      - POSTGRES_DB=proseo
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5432:5432"
  proseo-ingestor:
    image: ${REGISTRY_URL}/proseo-ingestor:${VERSION}-proseo
    ports:
      - "8081:8080"
    depends_on:
      - proseo-db
  proseo-order-mgr:
    image: ${REGISTRY_URL}/proseo-order-mgr:${VERSION}-proseo
    ports:
      - "8082:8080"
    depends_on:
      - proseo-db
  proseo-prodplanner:
    image: ${REGISTRY_URL}/proseo-planner:${VERSION}-proseo
    ports:
      - "8083:8080"
    depends_on:
      - proseo-db
  proseo-processor-mgr:
    image: ${REGISTRY_URL}/proseo-processor-mgr:${VERSION}-proseo
    ports:
      - "8084:8080"
    depends_on:
      - proseo-db
  proseo-productclass-mgr:
    image: ${REGISTRY_URL}/proseo-productclass-mgr:${VERSION}-proseo
    ports:
      - "8085:8080"
    depends_on:
      - proseo-db
  proseo-user-mgr:
    image: ${REGISTRY_URL}/proseo-user-mgr:${VERSION}-proseo
    ports:
      - "8086:8080"
    depends_on:
      - proseo-db
  proseo-facility-mgr:
    image: ${REGISTRY_URL}/proseo-facility-mgr:${VERSION}-proseo
    ports:
      - "8087:8080"
    depends_on:
      - proseo-db
  proseo-gui:
    image: ${REGISTRY_URL}/proseo-ui-gui:${VERSION}-proseo
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
export VERSION=<the prosEO version to install>
export POSTGRES_PASSWORD=<password for postgres user as configured in prosEO images, see step 2 above>
export PGADMIN_EMAIL=<email address for pgAdmin authentication>
export PGADMIN_PASSWORD=<some password for pgAdmin authentication>
docker-compose -p proseo up -d
```

The `proseo-images` directory may be populated with convenience scripts for these steps (see `proseo-images/README.md`).

After starting the prosEO control instance two SQL scripts need to be executed. First login to the proseo-db container,
either via the Docker Desktop dashboard (and the command `su - postgres`) or from the command line:
```
docker exec -it proseo-proseo-db-1 su - postgres
```
From within the container (which should now show a prompt like `postgres@...:~$ `) execute the SQL command files provided:
```
psql proseo </proseo/create_view_product_processing_facilities.sql
psql proseo </proseo/populate_mon_service_state.sql
```


# Step 4: Install and Run the prosEO Command Line Interface

A build of prosEO will create the JAR file for the Command Line Interface in `<project root>/ui/cli/target`. A sample
`application.yml` file can be found at `<project root>/ui/cli/src/main/resources`. In this file the hostnames point to
`localhost`, which is where the Docker Desktop engine is presumably located.

Start the CLI with `java -jar proseo-ui-cli.jar`. On a newly created prosEO Control Instance
a default user `sysadm` (password `sysadm`) is provided.

To initialize the prosEO instance, run the following CLI commands:
```
login
# at the prompt enter the username and password as given above
password
# at the prompts enter and repeat the new password for the user
exit
```


# Step 5: Create a Planner Account

For the Production Planner, an account with access to the Kubernetes API is required. The account must be able to read general
information about the Kubernetes cluster (health state, node list) and to fully manage jobs and pods (create, update, list, delete).
Create a file `planner-account.yaml` like this (or use the file in `<project root>/deploy/hands/kubernetes`):
```
apiVersion: v1
kind: ServiceAccount
metadata:
  name: proseo-planner
  namespace: default
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: proseo-planner-role
  # namespace not applicable for ClusterRole
rules:
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["events"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["create", "get", "list", "watch", "update", "patch", "delete"]
- apiGroups: [""]
  resources: ["pods/log"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["batch"]
  resources: ["jobs"]
  verbs: ["create", "get", "list", "watch", "update", "patch", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: proseo-planner-binding
  # namespace not applicable for ClusterRoleBinding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: proseo-planner-role
subjects:
- kind: ServiceAccount
  name: proseo-planner
  namespace: default
---
apiVersion: v1
kind: Secret
metadata:
  name: proseo-planner-secret
  namespace: default
  annotations:
    kubernetes.io/service-account.name: proseo-planner
type: kubernetes.io/service-account-token
```

Create the account, role and role binding, and retrieve the authentication token for the new account:
```bash
kubectl apply -f ../deploy/hands/kubernetes/planner-account.yaml
kubectl describe secret/$(kubectl get secrets | grep proseo-planner | cut -d ' ' -f 1)
```


# Step 6: Configure a prosEO Mission

You are now ready to configure your first mission. See `<project root>/samples/testdata` for
an example, which works with the prosEO Sample Processor. Test input data and processing orders
can be generated as described below using the script `deploy-single-node/ptm-config/create_data_local.sh`.
Note that this script deliberately creates an order set, which does not result in a fully completed
processing (one job will remain in `RELEASED` state due to missing input, this requires generation
of an additional order to process from L0 to L2A/B data - this is left as an exercise to the reader ;-) ).


# Step 7: Setup the Kubernetes Cluster with Storage Manager and File System Cache

For the single-node installation we will use POSIX as the default file system, thereby
avoiding the overhead of running an S3 object storage provider. We will not make Alluxio
available either. This does not place any functional constraints on prosEO, since these
storage options are meant for externally provided installations only, where online storage
is expensive (a multi-TB USB-3 disk on a laptop is not).

This step requires configuring a "host path" file server to serve the common storage area
to both the Storage Manager and the Processing Engine. Note that Docker Desktop imposes certain
*restrictions on the location of the data:*
- On macOS, the directory must be located below any of the paths available for sharing *by default* (e. g. `/Users`),
  using other paths (e. g. `/opt`) does not work, even if they are declared as sharable in the Docker Desktop
  preferences.
- On Windows it appears that the paths to use are somewhat weird, see for example this discussion: 
  <https://stackoverflow.com/questions/54073794/kubernetes-persistent-volume-on-docker-desktop-windows>
  (However this has not been verified by the author of this documentation)

Assuming a configuration as in the files given in the current (example) directory, the following commands must be issued
(also available as part of the script `ptm-config/create_data_local.sh`):
```sh
# Define actual path to storage area
SHARED_STORAGE_PATH=<path on Docker Desktop host>

# Update the path in the Persistent Volume configuration
sed "s|%SHARED_STORAGE_PATH%|${SHARED_STORAGE_PATH}|" <nfs-pv.yaml.template >nfs-pv.yaml

# Create the Persistent Volumes
kubectl apply -f nfs-pv.yaml

# Create the export directories
mkdir -p ${SHARED_STORAGE_PATH}/proseodata ${SHARED_STORAGE_PATH}/transfer
```

Locate the file `storage-mgr.yaml` and edit the image reference (around line 20) to point
to your preferred prosEO repository, then create the Storage Manager:
```
kubectl apply -f storage-mgr-local.yaml
```


# Step 8: Configure the prosEO Processing Facility

Unless the configuration script was used create a JSON file `facility.json` describing the local processing facility:
```
{
  "name" : "localhost",
  "description" : "Docker Desktop Minikube",
  "processingEngineUrl" : "http://host.docker.internal:8001/",
  "processingEngineToken" : "<authentication token from step 5>",
  "storageManagerUrl" : "http://host.docker.internal:8001/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1",
  "localStorageManagerUrl" : "http://storage-mgr-service.default.svc.cluster.local:3000/proseo/storage-mgr/v0.1",
  "storageManagerUser" : "smuser",
  "storageManagerPassword" : "smpwd-but-that-would-be-way-too-short",
  "defaultStorageType" : "POSIX"
}
```

On the prosEO CLI issue the following commands:
```
login PTM
# at the prompt enter your username and password
facility create --file=facility.json
```

If the facility was already created using the configuration script, the authentication token must be updated:
```
facility update localhost processingEngineToken=<authentication token from step 5>
```

In any case the processing facility will be in DISABLED state, so it must be "started" to be available for use:

```
facility update localhost facilityState=STOPPED
facility update localhost facilityState=STARTING
facility update localhost facilityState=RUNNING
```
