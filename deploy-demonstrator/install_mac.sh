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

echo "[1/10] Checking prerequisites"
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

#    # Kubernetes enabled?
#    if ! kubectl cluster-info >/dev/null 2>&1; then
#        echo "ERROR: Kubernetes is not available."
#        echo "Please enable Kubernetes in Docker Desktop and wait until it is ready."
#        exit 1
#    fi
#    echo "OK: Kubernetes is available."

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
}
check_prerequisites
echo ""

echo "[2/10] Configure Kubernetes"
function configure_kubernetes() {
	# TODO R
	echo "OK: Kubernetes Dashboard is running"
	
	# TO DO
	echo "OK: Planner account created"
	
	# TO DO
	echo "OK: Storage prepared"
}
configure_kubernetes
echo ""

echo "[3/10] Updating configuration according to prosEO version ${PROSEO_VERSION}"
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

echo "[4/10] Prepare docker images"
function prepare_images(){
	cd proseo-images
	./build_images.sh ${REGISTRY_URL} 
	./push_images.sh ${REGISTRY_URL}
	
	cd ..
	echo "OK: Images built and pushed successfully"
}
prepare_images
echo ""

echo "[5/10] Run prosEO"
function run_proseo() {
	cd proseo-images
	export POSTGRES_PASSWORD="demo-only"
	docker-compose -p proseo up -d
	
	cd ..
	echo "OK: prosEO is running"
}
run_proseo
echo ""

echo "[6/10] Prepare database"
function prepare_database() {
	# TODO
	echo "OK: Database prepared"
}
prepare_database
echo ""

echo "[7/10] Run the CLI"
function install_cli() {
	# TODO
	echo "OK: CLI running"
}
install_cli
echo ""

echo "[8/10] Configure the test mission"
function configure_ptm() {
	# TODO
	echo "OK: facility configured"
	
	# TODO
	echo "OK: test mission configured"
}
configure_ptm
echo ""

echo "The demonstrator is up and running:" 
echo "- The CLI is available at #TODO"
echo "- The GUI is available at #TODO"
echo ""
echo "Note: The demonstrator can be stopped by running stop_control_instance.sh from the proseo-images directory."