# Deploying prosEO on a Single Node for Testing and Development

For development and testing purposes, a single-node setup can be used to host both the **prosEO Control Instance** (the "brain") and the **prosEO Processing Facility** (the "hands") , provided the machine has sufficient RAM and disk capacity.

## Pre-requisites

The target machine must have unrestricted access to the internet and meet the following minimum hardware requirements:

- **CPU:** At least 4 cores
- **RAM:** At least 16 GB
- **Disk:** At least 200 GB of available storage

> **Disclaimer**: Only execute the following commands if you fully understand their purpose and the current state of your system. This guide assumes a single-node (VM) deployment on **Rocky Linux 9.5 (Blue Onyx)** using:
> 
> - Docker: v28.1.1
> - Docker Compose: v2.35.1
> - Minikube: v1.35.0
> - Java: OpenJDK 17.0.15 (LTS)
> - Maven: Apache Maven 3.6.3 (Red Hat 3.6.3-22)
> - Raml2html: 7.8.0
> 
> **Note:** Minikube currently does **not** fully support the Podman container engine. (Tested versions: Podman 5.4.0, Podman-Compose 1.0.6)

# 1. Prepare Your Environment

This tutorial shows how to deploy prosEO on a single node using [Docker](https://www.docker.com/) as the container engine and [Minikube](https://minikube.sigs.k8s.io/docs/) as the [Kubernetes](https://kubernetes.io/) cluster manager.

> **Note:** The commands below may change over time. Refer to the official documentation when installing the required dependencies.

## 1.1 Define Storage Locations

To keep the environment organized and avoid storage constrains, define custom storage paths for Docker and prosEO related data. This is especially important if your system's default Docker storage location has space constraints.

**Naming directories:**
- `/docker-storage`: Docker's data directory
- `/registry-bind-mount`: For the local Docker registry
- `/proseo-data`: Stores prosEO-generated data and logs
- `/proseo-shared-storage`: Shared input/output directory used by the prosEO storage manager microservice

Create the directories:

```bash
mkdir -p /data/proseo-environment/{docker-storage,proseo-data,proseo-shared-storage,registry-bind-mount}
```

Ensure the directory `proseo-shared-storage` has read/write access to *others*. *Minikube* has to have permissions to get and store data from this directory:

```bash
chmod 757 -R /data/proseo-environment/proseo-shared-storage/
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
  "data-root": "/data/proseo-environment/docker-storage"
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
  -v /data/proseo-environment/registry-bind-mount:/var/lib/registry \
  --name registry registry:2
```

The `-e` and `-v` options are recommended for better maintenance of the local registry.

#### **Optional Test: Verifying Local Docker Registry Workflow**

In this test, the process begins by downloading a fresh `openjdk:11` image from [Docker Hub](https://hub.docker.com/). The image is then retagged and stored in the local Docker registry for further validation. To ensure that the image is pulled exclusively from the local registry, all Docker Hub images are removed, and the image is re-pulled from the local registry. The goal is for all commands to execute successfully, demonstrating the proper flow of images within the local registry.

 **Steps**:

```bash
# 1. Pull the openjdk:11 image from Docker Hub:
docker image pull openjdk:11

# 2. Retag the image for storage in the local registry (localhost:5000):
docker image tag openjdk:11 localhost:5000/openjdk:11

# 3. Push the retagged image to the local registry:
docker image push localhost:5000/openjdk:11

# 4. Remove the original openjdk:11 image from Docker Hub:
docker image rm openjdk:11

# 5. Remove the localhost:5000/openjdk:11 image from the local registry (optional, for cleanup):
docker image rm localhost:5000/openjdk:11

# 6. Pull the image again from the local registry:
docker image pull localhost:5000/openjdk:11
```

By following these steps, you can ensure that Docker is pulling the image from the local registry, confirming that the image flow from Hub to local registry and back works as expected.

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
  --mount-string='/data/proseo-environment/proseo-shared-storage:/minikube-host' \
  --insecure-registry="host.minikube.internal:5000"
```

This command allocates sufficient resources to Minikube for running prosEO (6 CPUs and 32 GB of memory), though you should adjust these values based on your hardware capacity. The driver is set to docker. The mount option is enabled, and the mount-string points to the storage location specified in **Chapter 1.1** of this guide. Please specify an appropriate path for your case accordingly. Note that the important part is the path inside Minikube (e.g., /minikube-host). Lastly, the --insecure-registry option must be set to allow Minikube access to the local container registry where prosEO’s images are stored and required for processing.

If you encounter the error:  
**`StartHost failed... failed to acquire bootstrap client lock: bad file descriptor`**,  
try setting a custom Minikube home:

```bash
minikube stop
minikube delete --all --purge
echo 'export MINIKUBE_HOME=/data/proseo-environment' >> ~/.bashrc
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

For now on the string `<proseo-root>` will refer to the prosEO github root directory.

## 2.2 Compile Code Without Running Unit Tests

Navigate into the cloned prosEO directory and compile the project, skipping the unit tests for faster execution:

```bash
cd prosEO/
mvn clean install -Dmaven.test.skip=true
```
## 2.3 Set Up Kubernetes Configuration

Locate the Kubernetes configuration file that was automatically generated when you started (i.e. **Chapter 1.4.1**) Minikube. This file is typically located in the `${HOME}` directory. Once you find the configuration file, copy it into the `proseo-planner` component directory:

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

> **Note**: Make sure all containers show a healthy and stable status. If any container repeatedly restarts or fails, consult the logs with:

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


## 4.2 Install and Run the prosEO Command Line Interface

After building prosEO, the Command Line Interface (CLI) JAR file will be created at:

```
<proseo-root>/ui/cli/target/proseo-ui-cli.jar
```


This CLI tool can be executed on any system. However, for it to work, you need to define a configuration file named `application.yaml` in the **same directory** where the `proseo-ui-cli.jar` file resides.

A sample `application.yaml` file is available here:

```
<proseo-root>/ui/cli/src/main/resources/application.yaml
```

By default, this sample configuration points to `localhost` for all prosEO component hostnames, assuming Docker is running locally. If your setup uses different hosts or URLs, you must update the configuration file accordingly.

### Running the prosEO CLI

1. Navigate to the directory containing `proseo-ui-cli.jar` and your `application.yaml` file.
2. Start the CLI using the command:
```bash
java -jar proseo-ui-cli.jar
```

On a fresh prosEO Control instance, a default user is provided:

- **Username:** `sysadm`
- **Password:** `sysadm`

### Initializing the prosEO Instance

Run the following commands in the CLI to log in and change the default password:

```bash
login
# When prompted, enter the username and password above

password
# Enter the new password twice when prompted

exit
```

> **Tip:** Use the `help` command within the prosEO CLI to explore available utilities and commands.

## 4.3 Create Planner Account

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

> **Info**: Save the token securely. It will be required by the planner service to authenticate with the Kubernetes cluster.

# 5. Configure a ProsEO Mission

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

> **Note**: It is crucial to ensure that the values for `PROSEO_WRAPPER_USER` and `PROSEO_WRAPPER_PWD` in the script match those defined in your environment variables. These are initially configured in **Chapter 2.4**.

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

Both the test _processor_ and the corresponding _processor-wrapper_ were already created during the ProseO build process (**Chapter 2.2**) when the system was compiled and the Docker images for ProseO components were generated.

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

> **Info**: Another way to verify that the mission creation was successful is to check whether the **prosEO user interface (UI)** is accessible.  To do this, open a web browser and navigate to `http://<ip-or-domain>:8088`
> 
> If the UI loads correctly, it indicates that the prosEO UI is up and running. The **PTM** should now be displayed in the drop-down menu **Missionen**. You should now be able to login in the UI for the PTM mission with the proseo username and password defined in the `configure_proseo_test_mission.pl` (i.e. username=proseo and password=proseo.789)


# 6. Setup the Kubernetes Cluster with Storage Manager and File System Cache

Input and output data must be ingested and stored in a designated location, which needs to be configured beforehand. For the single-node installation, we will use POSIX as the default file system. This choice avoids the overhead of running an S3 object storage provider and does not impose any functional limitations on prosEO. Note that storage options like S3 are intended primarily for externally provided installations where online storage costs are significant.

This step involves configuring a _hostPath_ file server to provide a shared storage area accessible by both the Storage Manager and the Processing Engine. Keep in mind that Docker enforces restrictions on where data can be stored. Please verify your system’s storage constraints; **Chapter 1.1** in this guide covers this topic in detail.

## 6.1 Set up Kubernetes Persistent Volumes

Kubernetes uses the `PersistentVolume` resource to locate input data and store processed output. Inside the directory:

```bash
<proseo-root>/deploy-single-node/kubernetes
```

you will find a file named `nfs-pv.yaml.template`. Copy this file and rename it as `nfs-pv.yaml`:

```bash
cd <proseo-root>/deploy-single-node/kubernetes
cp nfs-pv.yaml.template nfs-pv.yaml
vim nfs-pv.yaml
```

Using your preferred text editor, update the hostPath entries for proseo-ingest-share and proseo-nfs-share. Specifically, edit lines **16** and **24**. Since we are using Minikube as our Kubernetes cluster, set the paths to /minikube-host/.... The relevant section should look like this:

```yaml
..	...
15	  hostPath:
16	    path: /minikube-host/transfer
17	    type: Directory
..	...
23	  hostPath:
24	    path: /minikube-host/proseodata
25	    type: Directory
..	...
```

## 6.2 Configure Storage Manager Volumes

Among other responsabilities, the prosEO Storage Manager is in charge of transferring processed data from the local file system to an external location (such as an S3 bucket). For this test setup, a POSIX filesystem serves as both the internal and external storage.

In the directory:
```bash
<proseo-root>/deploy-single-node/kubernetes
```
locate the file `storage-mgr-local.yaml`'

It is important to confirm that the Storage Manager container’s image reference is correct. Open the file and check line **51**, it should specify host.minikube.internal as the image registry, like this:

```yaml
..    ...
49    containers:
50      - name: proseo-storage-mgr
51        image: host.minikube.internal:5000/proseo-storage-mgr:latest
52        resources:
53          requests:
..    ...
```

## 6.3 Deploy Kubernetes Resources 

After configuring the persistent volumes (`nfs-pv.yaml`) and the Storage Manager manifest (`storage-mgr-local.yaml`), you can deploy these resources to your Minikube cluster by running the provided script `create_k8s_resources.sh`:

```bash
cd <proseo-root>/deploy-single-node/kubernetes

# ./create_k8s_resources.sh <Storage Manager image tag> <path to shared storage>"

./create_k8s_resources.sh 1.1.0-proseo /data/proseo-environment/proseo-shared-storage
```

This script performs the following steps:

1. Builds a new container image for the prosEO Storage Manager service, tagging it as `latest`.
2. Creates the necessary directories where the data will be stored.
3. Applies the persistent volume definitions and deploys the Storage Manager pod to the Kubernetes cluster.


# 7. Using ProsEO

## 7.1 Set Up the Processing Facility, Prepare Data, and Create Processing Orders

The processing facility (the "hands") is already set up, but it must now be recognized and configured within the prosEO Control instance (the "brain"). This is the final configuration step before processing any data. Afterward, test data will be ingested, and processing orders will be created.

A helper script is provided to generate the necessary configuration files (in JSON format). These files are then passed as standard input to the prosEO CLI. The script is located at:

```
cd <proseo-root>/deploy-single-node/ptm-config
```


### What the Script Does

The `create_data_local.sh` script performs the following tasks:
1. Creates dynamic test data for the prosEO test mission:
    - Level 0 (L0) input data
    - IERSB AUX input data
2. Sets up a processing facility in Docker Desktop
3. Creates processing orders for:
    - Level 2 (L2) products
    - Level 3 (L3) products


### Running the Script

Execute the script by providing the Storage Manager image tag and the path to the shared storage:

```bash
# ./create_data_local.sh <Storage Manager image tag> <path to shared storage>"

./create_data_local.sh 1.1.0-proseo /data/proseo-environment/proseo-shared-storage
```


After running the script, a new directory named `testproducts` is created inside `ptm-config`. Its contents include:

```bash
testproducts/
├── facility.json              # Configuration for creating the facility
├── ingest_products.json       # Configuration for ingesting products
├── order_l2.json              # Processing order for L2 data
├── order_l3.json              # Processing order for L3 data
└── transfer/
    └── import/
        └── products/          # L0 products to be ingested
            ├── bulletinb-380.xml
            ├── PTM_L0_20191104090000_20191104094500_20191104120000.RAW
            ├── ...

```

> **Note:** The script intentionally creates an incomplete processing setup—one job will remain in the `RELEASED` state due to missing inputs. Creating an additional processing order (from L0 to L2A/B) is left as an exercise for the reader.



### 7.1.1 Add the Kubernetes Secret to the Facility Configuration

After the script runs, the `facility.json` file still lacks the required Kubernetes secret token. To inject it:

```bash
cd <proseo-root>/deploy-single-node/ptm-config/testproducts

TOKEN=$(kubectl get secret proseo-planner-secret -o jsonpath="{.data.token}" | base64 --decode)

sed -i "s/TBD/${TOKEN}/" facility.json
```

This command retrieves the Kubernetes secret, decodes it, and replaces the placeholder (`TBD`) in the `facility.json` file with the actual token.



## 7.2 Create the Facility, Ingest Data, and Submit Processing Orders

At this stage, you have two options:

### 7.2.1 Option 1: Manual Execution (Recommended for Debugging)

Log in to the prosEO CLI using the `proseo` user and the `PTM` mission. This approach lets you run each command step-by-step for easier troubleshooting.

```bash
cd /users/cort_ni/prosEO/deploy-single-node/ptm-config
java -jar /users/cort_ni/prosEO/ui/cli/target/proseo-ui-cli.jar
```

CLI Login

```bash
                            /-------\ /-------\
                            |       | |       |
                            |   /---/ |  /-\  |
                            |   \---\ |  | |  |   prosEO - The Processing System for Earth Observation Data
/----\ /----\ /----\ /----\ |       | |  | |  |
|    | |    | |    | | ---+ |   /---/ |  | |  |   Command Line Interface  (v1.1.0)
| {} | |  /-/ | {} | \    \ |   \---\ |  \-/  |
|    | |  |   |    | +--- | |       | |       |   :: Spring Boot ::       (v2.7.18)
|  /-/ \--/   \----/ \----/ \-------/ \-------/
|  |
\--/

(Type "help" for help.)

prosEO (no mission)> login PTM
Username (empty field cancels): proseo
Password for user proseo: prosEO.789
(I6094) User proseo logged in
prosEO (PTM)>
```

Execute the Following CLI Commands:

```bash
prosEO (PTM)> facility create --file=testproducts/facility.json
prosEO (PTM)> ingest --file=testproducts/ingest_products.json localhost
prosEO (PTM)> order create --file=testproducts/order_l2.json
prosEO (PTM)> order create --file=testproducts/order_l3.json
```

### 7.2.2 Option 2: Automated Execution

You can also automate the process using a credentials file and a prewritten CLI script.

#### Step 1: Create the Credentials File

This step is the similar as described in **Chapter 5.1**.

```bash
cd <proseo-root>/deploy-single-node/ptm-config

cat >> proseo.cred
proseo
prosEO.789

# ( to exit from 'cat standard input' just press crtl+c )
```

It is important to set the permission of `proseo.cred` to only read and write for the owner.

```bash
cd <proseo-root>/deploy-single-node/ptm-config
chmod 600 proseo.cred
```

#### Step 2: Run the CLI with the Script

Assuming the file `cli_data_script.txt` was successfully generated by executing the `create_data_local.sh` script and with the `proseo` access credentials in place, run the following CLI commands:

```bash
cd <proseo-root>/deploy-single-node/ptm-config

java -jar <proseo-root>/ui/cli/target/proseo-ui-cli.jar < cli_data_script.txt
```

After successful execution, the prosEO system will begin processing the data.

> **Reminder:** As previously noted, one job will remain in the `RELEASED` state due to missing input. You must create an additional processing order (L0 to L2A/B) using the prosEO UI to fully complete processing. This step is intentionally left to the user.


# 8. Final Steps

At this stage, prosEO should be fully operational with the **PTM mission**, and processing orders have been successfully created.

To proceed:
1. Open the prosEO web interface by navigating to:  
    http://ip-or-domain:8088/
2. In the GUI, locate the **processing orders**.
3. For each order, follow these steps:
    - **Approve** the order
    - **Plan** the order
    - **Release** the order

These steps initiate actual data processing within the prosEO environment.
