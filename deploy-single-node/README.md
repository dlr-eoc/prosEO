# Deploying prosEO on a Single Node for Testing and Development

For development and testing purposes, a single-node setup can be used to host both the **prosEO Control Instance** (brain) and the **prosEO Processing Facility** (hands) , provided the machine has sufficient RAM and disk capacity.

## Pre-requisites

The target machine must have unrestricted access to the internet and meet the following minimum hardware requirements:

- **CPU:** At least 4 cores
- **RAM:** At least 16 GB
- **Disk:** At least 200 GB of available storage

## Disclaimer

> **Caution:** Only execute the following commands if you fully understand their purpose and the current state of your system. This guide assumes a single-node (VM) deployment on **Rocky Linux 9.5 (Blue Onyx)** using:

- Docker: v28.1.1
- Docker Compose: v2.35.1
- Minikube: v1.35.0
- Java: OpenJDK 17.0.15 (LTS)
- Maven: Apache Maven 3.6.3 (Red Hat 3.6.3-22)
- Raml2html: 7.8.0

> **Note:** Minikube currently does **not** fully support the Podman container engine. (Tested versions: Podman 5.4.0, Podman-Compose 1.0.6)

# 1. Prepare Your Environment

This tutorial shows how to deploy prosEO on a single node using [Docker](https://www.docker.com/) as the container engine and [Minikube](https://minikube.sigs.k8s.io/docs/) as the [Kubernetes](https://kubernetes.io/) cluster manager.

> **Note:** The commands below may change over time. Refer to the official documentation when installing the required dependencies.

## 1.1 Define Storage Locations

To keep the environment organized, it's recommended to define custom storage paths for Docker and prosEO-related data. This is especially important if your system's default Docker storage location has space constraints.

**Recommended directories:**
- `/docker-storage`: Docker's data directory
- `/registry-bind-mount`: For the local Docker registry
- `/proseo-data`: Stores prosEO-generated data and logs
- `/proseo-shared-storage`: Shared input/output directory used by the prosEO storage manager microservice

Create the directories:

```bash
mkdir -p /path-to-your-desired-location/{docker-storage,proseo-data,proseo-shared-storage,registry-bind-mount}
```

Ensure **Minikube** has read/write access to the shared storage directory:

```bash
chmod 757 -R /path-to-your-desired-location/proseo-shared-storage/
```

## 1.2 Install Docker

On your Rocky Linux machine:

```bash
sudo dnf config-manager --add-repo https://download.docker.com/linux/rhel/docker-ce.repo

sudo dnf -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

### 1.2.1 Configure Docker Storage

If you're using a custom storage path, configure Docker accordingly:

1. **Stop Docker:**

```bash
sudo systemctl stop docker.service
sudo systemctl stop docker.socket
```

2. **Edit `/etc/docker/daemon.json`**:

```json
{
  "insecure-registries": ["localhost:5000"],
  "data-root": "/path-to-your-desired-location/docker-storage"
}
```

- `insecure-registries`: Allows local registries without SSL (used by prosEO for local images).
- `data-root`: Redirects Docker's default data directory to the custom storage path.

3. **Restart Docker:**

```bash
sudo systemctl start docker.socket
sudo systemctl start docker.service
sudo systemctl status docker.socket
sudo systemctl --no-pager status docker.service
```

### 1.2.2 Set Up a Local Docker Registry

prosEO requires a container registry for storing its microservices:

```bash
docker run -d -p 5000:5000 --restart always \
  -e STORAGE_DELETE_ENABLED=true \
  -v /path-to-your-desired-location/registry-bind-mount:/var/lib/registry \
  --name registry registry:2
```

The `-e` and `-v` options are recommended for better maintenance of the local registry.

**Optional test:**

```bash
docker image pull openjdk:11
docker image tag openjdk:11 localhost:5000/openjdk:11
docker image push localhost:5000/openjdk:11
docker image rm openjdk:11
docker image rm localhost:5000/openjdk:11
docker image pull localhost:5000/openjdk:11
```

## 1.3 Install kubectl

Follow the official instructions:  
[Install kubectl on Linux](https://kubernetes.io/docs/tasks/tools/install-kubectl-linux/)

## 1.4 Install Minikube

Download and install the latest release:  
[Install Minikube](https://minikube.sigs.k8s.io/docs/start/)

### 1.4.1 Start Minikube

Launch Minikube with suitable resource limits and mount the shared storage directory:

```bash
minikube start \
  --cpus 6 \
  --memory 32GB \
  --driver docker \
  --mount=true \
  --mount-string='/path-to-your-desired-location/proseo-shared-storage:/minikube-host' \
  --insecure-registry="host.minikube.internal:5000"
```

This command allocates sufficient resources to Minikube for running prosEO (6 CPUs and 32 GB of memory), though you should adjust these values based on your hardware capacity. The driver is set to docker. The mount option is enabled, and the mount-string points to the storage location specified in [[01-projects/proseo/work-packages/integration-wp/02-setup-k8s-cluster/deploy-single-node/single-node-deployment-guide#1.1 Storage location|step 1.1]] of this guide. If you haven't set a storage location, please specify an appropriate path. Note that the important part is the path inside Minikube (e.g., /minikube-host). Lastly, the --insecure-registry option must be set to allow Minikube access to the local container registry where prosEO’s images are stored and required for processing.

If you encounter the error:  
**`StartHost failed... failed to acquire bootstrap client lock: bad file descriptor`**,  
try setting a custom Minikube home:

```bash
minikube stop
minikube delete --all --purge
echo 'export MINIKUBE_HOME=/path-to-your-desired-location' >> ~/.bashrc
source ~/.bashrc
minikube start ... # use the full command again
```

### 1.4.2 Configure Minikube DNS

To enable proper networking between Kubernetes services and prosEO:

1. Get Minikube IP:

```bash
minikube ip
```

2. Add entry to `/etc/hosts`:

```bash
echo "192.168.49.2    host.minikube.internal" | sudo tee -a /etc/hosts
```

_Replace the IP with your actual Minikube IP._


## 1.5 Install Java and Maven

Install Maven (OpenJDK will be installed as a dependency):

```bash
sudo dnf install maven
```

Verify that your user can run the `mvn` command. If not, check the PATH and permissions.

### 1.5.1 Configure Maven

Add the following to your Maven settings file (usually at `$HOME/.m2/settings.xml`):

```xml
<settings>
  <profiles>
    <profile>
      <id>dev-local</id><!-- or any other id you prefer -->
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <docker.registry>localhost:5000</docker.registry>
      </properties>
    </profile>
  </profiles>
</settings>
```

If you already have a `properties` element in your settings file, it is of course sufficient to just add the `docker.registry` property there.

## 1.6 Install raml2html

Install Node.js and `raml2html`:

```bash
sudo dnf install nodejs
npm i -g raml2html
```

Verify that your user can run `raml2html`. If not, inspect the install location and permissions.

# 2. Build the prosEO Control Instance

## 2.1 Clone the Git Repository

To begin, clone the prosEO repository from GitHub by running the following command:

```bash
git clone https://github.com/dlr-eoc/prosEO.git
```

## 2.2 Compile Code Without Running Unit Tests

Navigate into the cloned prosEO directory and compile the project, skipping the unit tests for faster execution:

```bash
cd prosEO/
mvn clean install -Dmaven.test.skip=true
```
## 2.3 Set Up Kubernetes Configuration

Locate the Kubernetes configuration file that was automatically generated when you [[01-projects/proseo/work-packages/integration-wp/02-setup-k8s-cluster/deploy-single-node/single-node-deployment-guide#1.4.1 Start Minikube|started]] Minikube. This file is typically located in the `${HOME}` directory. Once you find the configuration file, copy it into the `proseo-planner` component directory:

```bash
cd <proseo-root>/deploy-single-node/proseo-images/proseo-components
cp ~/.kube/config proseo-planner/kube_config

```

The `proseo-planner` microservice requires access to the Kubernetes API to manage processing orders, plan, and release tasks. Ensure that the Kubernetes configuration file is in the correct location (`${HOME}/.kube/config`). If it's not, revisit the Minikube setup process to ensure the configuration is properly created.

## 2.4 Set Environment Variables

Next, change to the `proseo-images` directory located within the prosEO repository:

```bash
cd <proseo-root>/deploy-single-node/proseo-images
```

Once you're in this directory, you need to create the `.env` file by copying the template: 

```bash
cp .env.template .env
```

> **Note**: Ensure that the new file is named `.env`, as this file contains essential environment variable configurations.

Open the `.env` file in your preferred text editor and update the necessary environment variables. These variables typically define usernames, passwords, and service connection details for proper functionality:

```bash
vim .env
```

Once the variables are updated (such as access credentials), export them to your environment using the following command:

```bash
export $(cat .env | xargs)
```

>**Note**: To verify that the environment variables were successfully set, you can check the system environment by running:

```bash
env
```

This command will display all the environment variables, including the ones you just configured.

## 2.5 Build Components

Finally, to build all the prosEO microservices, run the `build-components.sh` script. This script automates the process of building the necessary components for prosEO.

```bash
cd <proseo-root>/deploy-single-node/proseo-images/proseo-components
./build-components.sh
```

> **Note**: For a detailed explanation of the steps performed by this script, refer to the comments inside the script file itself.


# 3. Deploy the prosEO Control Instance

To deploy the prosEO Control Instance, simply navigate to the directory containing the appropriate Docker Compose file and start the containers.

```bash
cd <proseo-root>/deploy-single-node/proseo-images/
docker compose -p proseo up -d
```

This command will launch all containers in **detached mode**.  After execution, verify that all containers are running stably and are not in a restart loop. You can use the `watch` command to continuously monitor container status:

```bash
watch docker container ls -a
```

Make sure all containers show a healthy and stable status. If any container repeatedly restarts or fails, consult the logs with:

```bash
docker container logs <container-name>
```

## 3.1 Connect Kubernetes and Docker Networks

Since the **prosEO Control Instance** (the "brain") and the **prosEO Processing Facility** (the "hands") operate in separate network environments, they must be connected to enable communication and allow the scheduling of processing orders.

To bridge these networks, connect the relevant prosEO containers to the `minikube` network using the following Docker commands:

```bash
docker network connect minikube proseo-proseo-ingestor-1
docker network connect minikube proseo-proseo-prodplanner-1
```


# 4. Set Up ProsEO
## 4.1 create view and populate table -  containerized proseo database

After starting the prosEO Control Instance, two SQL scripts must be executed to finalize the database setup. These commands are run inside the `proseo-proseo-db-1` container and initialize essential components required for prosEO to operate correctly.

- The first script creates a database view that presents information about the available product processing facilities.
- The second script populates monitoring-related data used to track the service state of prosEO components.

```bash
$ docker container exec proseo-proseo-db-1 psql -U postgres -d proseo -f /proseo/create_view_product_processing_facilities.sql

$ docker container exec proseo-proseo-db-1 psql -U postgres -d proseo -f /proseo/populate_mon_service_state.sql
```

Ensure both commands complete successfully without errors to confirm the system is properly initialized.

## 4.2 Create Planner Account

To enable the **prosEO Production Planner** to manage workloads, an account with appropriate permissions to the Kubernetes API is required. This account must have read access to general cluster information (e.g., health status, node list) and full control over Kubernetes Jobs and Pods (create, list, update, delete).

A predefined Kubernetes configuration file is provided for this purpose. It includes the necessary definitions for the user account, ClusterRole, ClusterRoleBinding, and secrets.

Apply the configuration using the following command:

```bash
kubectl apply -f <proseo-root>/deploy-single-node/kubernetes/planner-account-single-node.yaml
```

Once applied, retrieve the authentication token for the new `proseo-planner` account:

```bash
kubectl describe secret/$(kubectl get secrets | grep proseo-planner | cut -d ' ' -f 1)
```

> [!info] Info:
> Save the token securely. It will be required by the planner service to authenticate with the Kubernetes cluster.

# 5. Configure a ProseO Mission

A _mission_ in ProseO refers to a specific configuration that includes key components such as users, satellites, orbits, and processor workflows. At this stage, the system is ready to configure its first mission.

An example configuration script is provided at:

```
<proseo-root>/samples/testdata/configure_proseo_test_mission.pl
```

## 5.1 Adapt Access Credentials

This Perl script sets up the **ProseO Test Mission (PTM)** using the **ProseO Sample Processor**. It configures the following components:

- Default values    
- Users for managing the Control Instance    
- A basic satellite mission setup, including the spacecraft and orbit parameters    
- Processor classes, versions, configurations, and associated workflows

> [!info] Note 
> It is crucial to ensure that the values for `PROSEO_WRAPPER_USER` and `PROSEO_WRAPPER_PWD` in the script match those defined in your environment variables. These are initially configured in [[01-projects/proseo/work-packages/integration-wp/02-setup-k8s-cluster/deploy-single-node/single-node-deployment-guide#2.4 Set Environment Variables|Chapter 2.4: Set Environment Variables]].

In particular, update the following lines in the `configure_proseo_test_mission.pl` script (lines 114 and 115):

```perl
113    {
114        name => 'wrapper',        # Update this to match your environment variable PROSEO_WRAPPER_USER
115        pwd => 'ingest&Plan',     # Update this to match your environment variable PROSEO_WRAPPER_PWD
116        authorities => [],
117        groups => [ 'internalprocessor' ]
118    }

```


## 5.2 Updating the Processor Wrapper Location

In addition to setting mission parameters, it is necessary to **adapt the Docker image URL** for the `processor-wrapper` within the Perl script. This wrapper is responsible for executing the processor (i.e., the algorithm that processes satellite data to generate specific output products).

Both the test _processor_ and the corresponding _processor-wrapper_ were already created during the ProseO build process—when the system was compiled and the Docker images for ProseO components were generated.

At this stage, you only need to **specify where the relevant Docker images are stored**, so that **Minikube (Kubernetes)** can pull them and deploy the necessary pods during processing.

Update the Docker image path from:

```
localhost:5000/proseo-sample-wrapper:1.1.0
```

to

```
host.minikube.internal:5000/proseo-sample-wrapper:1.1.0
```

This change is required in three specific places—lines **162**, **171**, and **180** of the `configure_proseo_test_mission.pl` script:

```perl
154	my @processors = (
155	    {
156	    	processorName => 'PTML1B', 
157	    	processorVersion => '1.1.0',
158	    	configuredProcessors => [ 'PTML1B_1.1.0_OPER_2020-03-25' ],
159	    	tasks => [ 
160	    	   { taskName => 'ptm_l01b', taskVersion => '1.1.0' }
161	    	],
162	    	dockerImage => 'localhost:5000/proseo-sample-wrapper:1.1.0'
163	    },
164	    {
165	        processorName => 'PTML2', 
166	        processorVersion => '1.1.0',
167	        configuredProcessors => [ 'PTML2_1.1.0_OPER_2020-03-25' ],
168	        tasks => [ 
169	           { taskName => 'ptm_l2', taskVersion => '1.1.0' }
170	        ],
171	        dockerImage => 'localhost:5000/proseo-sample-wrapper:1.1.0'
172	    },
173	    {
174	        processorName => 'PTML3', 
175	        processorVersion => '1.1.0',
176	        configuredProcessors => [ 'PTML3_1.1.0_OPER_2020-03-25' ],
177	        tasks => [ 
178	           { taskName => 'ptm_l3', taskVersion => '1.1.0' }
179	        ],
180	        dockerImage => 'localhost:5000/proseo-sample-wrapper:1.1.0'
181	    }
182	);
```

`localhost` inside the Kubernetes cluster does **not** refer to your host machine. Replacing it with `host.minikube.internal` allows Minikube pods to correctly resolve and pull the Docker image from the host system's registry.

This ensures seamless integration between the mission configuration and the processing environment.

Finally, execute the perl script
```bash
cd <proseo-root>/samples/testdata/
./configure_proseo_test_mission.pl
```

## 5.1 Create the Mission

Once you've executed the Perl script from the previous step, a file named `cli_script.txt` is generated. This file contains the necessary instructions to create the mission using the **prosEO CLI tool**.

Before this file can be used and to automate this process, you must first **authenticate** with the prosEO CLI.  This is done by creating a credentials file containing the system administrator's username and password.

```bash
cd <proseo-root>/samples/testdata/
cat >> sysadm.cred
sysadm
sysadm

# ( to exit from 'cat standard input' just press crtl+c )
```

This creates a file named `sysadm.cred` containing the default **username and password** for the prosEO system administrator—both set to `sysadm` by default.

It is important to set the permission of `sysadm.cred` to only read and write for the owner.

```bash
cd <proseo-root>/samples/testdata/
chmod 600 sysadm.cred
```

You can change the administrator password later using the prosEO CLI if needed.

With the credentials in place and `cli_script.txt` generated, run the prosEO CLI to create the mission. Ensure you're still in the `testdata` directory:

```bash
$ cd <proseo-root>/samples/testdata/
$ java -jar <proseo-root>/ui/cli/target/proseo-ui-cli.jar < cli_script.txt
```

After successful execution, the **prosEO mission will be created**, and the system will be ready to handle satellite data processing as configured.

>[!info] Info
> Another way to verify that the mission creation was successful is to check whether the **prosEO user interface** is accessible.  To do this, open a web browser and navigate to `http://<ip-or-domain>:8088`
> If the UI loads correctly, it indicates that the prosEO UI is up and running. The **PTM** should now be displayed in the drop-down menu **Missionen**. You should now be able to login in the UI for the PTM mission with the proseo username and password defined in the `configure_proseo_test_mission.pl` (i.e. username=proseo and password=proseo.789)


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
