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


## Step 2: Deploy the prosEO Control Instance

Create the prosEO Control Instance from a `docker-compose.yml` file like the following:
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
docker-compose -p proseo up -d
```

The `testdata` directory contains convenience scripts for these steps. A template for the `docker-compose.yml` file
can be found at `<project root>/deploy/brain/prepare_proseo/files`.


# Step 3: Setup the Kubernetes Cluster with Storage Manager and File System Cache

For the single-node installation we will use POSIX as the default file system, thereby
avoiding the overhead of running an S3 object storage provider. We will not make Alluxio
available either. This does not place any functional constraints on prosEO, since these
storage options are meant for externally provided installations only, where online storage
is expensive (a multi-TB USB-3 disk on a laptop is not).

This step requires configuring a "host path" file server to serve the common storage area
to both the Storage Manager and the Processing Engine. Assuming a configuration as in the files
given in the current (example) directory, the following commands must be issued
(also available as part of the script `testdata/create_data_local.sh`):
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


# Step 4: Install and Run the prosEO Command Line Interface

A build of prosEO will create the JAR file for the Command Line Interface in `<project root>/ui/cli/target`. A sample
`application.yml` file can be found at `<project root>/ui/cli/src/main/resources`. In this file the hostnames point to
`localhost`, which is where the Docker Desktop engine is presumably located.

Start the CLI with `java -jar proseo-ui-cli.jar`. On a newly created prosEO Control Instance
a default user `sysadm` (password `sysadm`) is provided.

To initialize the prosEO instance, run the following CLI commands:
```
login -usysadm -psysadm
user update sysadm password=<new password>
create mission PTM 'name=prosEO Test Mission'
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
kind: Role
metadata:
  name: proseo-planner-role
  namespace: default
rules:
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["jobs", "pods"]
  verbs: ["create", "get", "list", "watch", "update", "patch", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: proseo-planner-binding
  namespace: default
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: proseo-planner-role
subjects:
- kind: ServiceAccount
  name: proseo-planner
  namespace: default
```

Create the account, role and role binding, and retrieve the authentication token for the new account:
```bash
kubectl apply -f planner-account.yaml
kubectl describe secret/$(kubectl get secrets | grep proseo-planner | cut -d ' ' -f 1)
```

# Step 6: Configure the prosEO Processing Facility

Create a JSON file `facility.json` describing the local processing facility:
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
login -usysadm -p<your password> PTM
facility create --file=facility.json
```

You are now ready to configure your first mission. See <https://github.com/dlr-eoc/prosEO/tree/master/samples/testdata> for
an example, which works with the prosEO Sample Processor. Test input data and processing orders
can be generated with the above mentioned script `single-node-deploy/testdata/create_data_local.sh`.
Note that this script deliberately creates an order set, which does not result in a fully completed
processing (one job will remain in `RELEASED` state due to missing input, this requires generation
of an additional order to process from L0 to L2A/B data - this is left as an exercise to the reader ;-) ).