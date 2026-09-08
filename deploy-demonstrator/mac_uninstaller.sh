#!/bin/bash
# Script-based rollout of a single-node prosEO deployment

# configure error handling and logging
set -euo pipefail

TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
LOG_FILE="install-${TIMESTAMP}.log"
exec > >(tee "$LOG_FILE") 2>&1

cat <<EOF

                            /-------\ /-------\\
                            |       | |       |
                            |   /---/ |  /-\  |
                            |   \---\ |  | |  |   prosEO - The Processing System for Earth Observation Data
/----\ /----\ /----\ /----\ |       | |  | |  |
|    | |    | |    | | ---+ |   /---/ |  | |  |   Single Node Demonstrator
| {} | |  /-/ | {} | \    \ |   \---\ |  \-/  |
|    | |  |   |    | +--- | |       | |       |   
|  /-/ \--/   \----/ \----/ \-------/ \-------/
|  |
\--/

Starting prosEO demonstrator. Refer to README for further instructions.

EOF

echo "Please enter prosEO version: "
read -r PROSEO_VERSION
echo "prosEO version set to ${PROSEO_VERSION}"
echo ""

echo "[1/8] Checking prerequisites"
function check_prerequisites() {

    # Docker installed?
    if ! command -v docker >/dev/null 2>&1; then
        echo "ERROR: Docker is not installed."
        echo "Please install Docker Desktop and try again."
        exit 1
    fi
    echo "OK: Docker is installed."

    # Docker running?
    if ! docker info >/dev/null 2>&1; then
        echo "ERROR: Docker is not running."
        echo "Please start Docker Desktop and wait until it is fully initialized."
        exit 1
    fi
    echo "OK: Docker is running."

    # kubectl installed?
    if ! command -v kubectl >/dev/null 2>&1; then
        echo "ERROR: kubectl is not installed or not in your PATH."
        echo "Please install kubectl and try again."
        exit 1
    fi
    echo "OK: kubectl is available."

    # Java installed?
    if ! command -v java >/dev/null 2>&1; then
        echo "ERROR: Java is not installed."
        echo "Please install Java (JDK 21 recommended)."
        exit 1
    fi
    echo "OK: Java is installed."

    echo ""

    # Registry
    read -rp "Please enter your preferred prosEO registry (e.g. localhost:5000): " REGISTRY_URL
    if [[ -z "$REGISTRY_URL" ]]; then
        echo "ERROR: Registry must not be empty."
        exit 1
    fi
    echo "OK: prosEO registry set to '${REGISTRY_URL}'"
    
    # Storage
    echo "Please configure the shared storage path."
    echo "Note: \n
    	- On macOS, the directory must be located below any of the paths available for sharing by default \n
    	  (e. g. `/Users`), using other paths (e. g. `/opt`) does not work, even if they are declared as \n
    	  sharable in the Docker Desktop preferences.\n
	    - On Windows it appears that the paths to use are somewhat weird, see for example this discussion: \n
  		  https://stackoverflow.com/questions/54073794/kubernetes-persistent-volume-on-docker-desktop-windows \n
		  (However this has not been verified by the author of this script.) \n"
    read -rp "Please enter where to store the prosEO data, e.g. /Users/you/prosEO data: " SHARED_STORAGE_PATH     
    if [[ -z "$SHARED_STORAGE_PATH" ]]; then 
    	echo "ERROR: Shared storage path must not be empty."
    	 exit 1 
    fi 
    echo "OK: Shared storage path set to '${SHARED_STORAGE_PATH}'"
    
}
check_prerequisites
echo ""

echo "[2/8] Configure Kubernetes"
function configure_kubernetes() {
	
	# Headlamp dashboard
	echo "Installing and starting Headlamp..."
	kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/headlamp/main/kubernetes-headlamp.yaml
	bash -c 'nohup kubectl port-forward -n kube-system service/headlamp 8002:80 2>&1 &'
	kubectl apply -f kubernetes/kube-admin.yaml
	kubectl describe secret/admin-user-secret --namespace kube-system
	echo "OK: Headlamp can be accessed at http://localhost:8002/ with the secret provided above"
	read -p "Press Enter to confirm that you have saved the secret above."
	echo ""
	
	echo "Creating planner account..."
	kubectl apply -f deploy/hands/kubernetes/planner-account.yaml
	echo "OK: Planner account created"	
	echo ""
	
	echo "Planner authentication token:"
	kubectl describe secret/proseo-planner-secret --namespace default
	read -p "Press Enter to confirm that you have saved the secret above."
	echo "OK: Planner account created"
	echo ""
	
	sed "s|%SHARED_STORAGE_PATH%|${SHARED_STORAGE_PATH}|" <nfs-pv.yaml.template >nfs-pv.yaml
	kubectl apply -f nfs-pv.yaml
	mkdir -p ${SHARED_STORAGE_PATH}/proseodata ${SHARED_STORAGE_PATH}/transfer
	echo "OK: Storage prepared"
}
configure_kubernetes
echo ""

echo "[3/8] Updating configuration according to prosEO version ${PROSEO_VERSION}"
function update_configuration() {
    find . -type f -name "*.template" | while IFS= read -r template; do
        target="${template%.template}"

        cp "$template" "$target"

        sed -i.bak "s/proseoVersionPlaceHolder/${PROSEO_VERSION}/g" "$target"
        rm -f "${target}.bak"

        echo "OK: Created $target"
    done
}
update_configuration
echo ""

echo "[4/8] Prepare docker images"
function prepare_images(){
	echo "Note: the base images must be available in the specified registry and are not \n
		  built here, as this is a demonstrator only."
	cd proseo-images
	./build_images.sh ${REGISTRY_URL} 
	./push_images.sh ${REGISTRY_URL}
	
	cd ..
	echo "OK: Dedicated images built and pushed successfully"
}
prepare_images
echo ""

echo "[5/8] Run prosEO"
function run_proseo() {
	kubectl apply -f storage-mgr-local.yaml
	bash -c 'nohup kubectl port-forward service/storage-mgr-service 8080:3000 2>&1 &'

	cd proseo-images
	export POSTGRES_PASSWORD="demo-only"
	docker-compose -p proseo up -d
	
	cd ..
	echo "OK: prosEO is running"
}
run_proseo
echo ""

echo "[6/8] Prepare database"
function prepare_database() {
	docker exec -it proseo-proseo-db-1 su - postgres -c 'psql proseo </proseo/populate_mon_service_state.sql'
	echo "OK: Database prepared"
}
prepare_database
echo ""

echo "[7/8] Check the CLI"
function install_cli() {
	DEFAULT_CLI="../ui/cli/target/proseo-ui-cli.jar" 
	CLI_PATH="$DEFAULT_CLI" 
	
	# Check whether CLI is available at the default location 
	if [[ ! -f "$DEFAULT_CLI" ]]; then 
		echo "CLI not found at: $DEFAULT_CLI" 
		echo "You can download it from:" 
		echo "https://proseo-registry.eoc.dlr.de/artifactory/prosEO/" 
		echo "" 
		read -r -p "Enter the path to the proseo-ui-cli.jar and press Enter: " CLI_PATH 
		
		if [[ ! -f "$CLI_PATH" ]]; then 
		echo "ERROR: CLI not found at: $CLI_PATH" 
		return 1 fi 
	fi 
	echo "OK: CLI available at $CLI_PATH"
}
install_cli
echo ""

echo "[8/8] Configure the test mission"
function configure_ptm() {
	./ptm-config/create_data_local.sh ${SHARED_STORAGE_PATH} 
	java -jar ${CLI_PATH} < cli_data_demonstrator_mac.txt	
	java -jar ${CLI_PATH} < "facility update localhost processingEngineToken=<authentication token from step 5>"
	echo "OK: test mission configured"
}
configure_ptm
echo ""

echo "The demonstrator is up and running:" 
echo "- The CLI is available at ${CLI_PATH}"
echo "- The GUI is available at localhost:8088"
echo ""
echo "Note: The demonstrator can be stopped by running stop_control_instance.sh from the proseo-images directory."