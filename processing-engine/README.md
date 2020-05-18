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
TBD

## Step 2: Deploy the prosEO Storage Manager
TBD

## Step 3 (optional): Deploy Alluxio as Storage Provider
TBD

## Step 4 (optional): Deploy Minio as S3 Object Storage Provider
TBD

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
docker-compose up -d
```

# Step 4: Install and Run the prosEO Command Line Interface
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


# Step 5: Configure the prosEO Processing Facility
Create a JSON file `facility.json` describing the local processing facility:
```
{
  "name" : "localhost",
  "description" : "Docker Desktop Minikube",
  "processingEngineUrl" : "http://host.docker.internal:8001/",
  "storageManagerUrl" : "http://host.docker.internal:8001/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1",
  "defaultStorageType" : "POSIX"
}
```

On the prosEO CLI issue the following commands:
```
login -usysadm -p<your password> PTM
facility create --file=facility.json
```

You are now ready to configure your first mission. See <https://github.com/dlr-eoc/prosEO/tree/master/samples/testdata> for
an example, which works with the prosEO Sample Processor.