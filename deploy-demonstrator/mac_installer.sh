#!/bin/bash
# Script-based rollout of a single-node prosEO deployment

# configure error handling and logging
set -euo pipefail

# -------
# Helpers
# -------
function start_port_forward() {
    local namespace="$1"
    local service="$2"
    local local_port="$3"
    local remote_port="$4"

    echo "Starting port-forward ${service} ${local_port}:${remote_port}..."

    nohup kubectl port-forward \
        -n "$namespace" \
        "service/${service}" \
        "${local_port}:${remote_port}" \
        >"port-forward-${service}.log" 2>&1 &

    local pid=$!

    # Give kubectl a moment to establish the connection.
    sleep 2

    if ! kill -0 "$pid" 2>/dev/null; then
        echo "ERROR: Failed to start port-forward for ${service}."
        cat "port-forward-${service}.log"
        return 1
    fi
    
    PORT_FORWARD_PIDS+=("$pid")

    echo "OK: Port-forward started (PID ${pid})"
}

PORT_FORWARD_PIDS=()
INSTALLATION_SUCCESSFUL=false

function cleanup_port_forwards() {
    if [[ "$INSTALLATION_SUCCESSFUL" != true ]]; then
        for pid in "${PORT_FORWARD_PIDS[@]}"; do
            kill "$pid" 2>/dev/null || true
        done
    fi
}

trap cleanup_port_forwards EXIT

function escape_sed_replacement() {
	printf '%s' "$1" | sed 's/[&|\\]/\\&/g'
}

# -------

TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_FILE="${SCRIPT_DIR}/install-${TIMESTAMP}.log"
exec > >(tee "$LOG_FILE") 2>&1

cat <<'EOF'

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

read -rp "Please enter prosEO version: " PROSEO_VERSION
    if [[ -z "$PROSEO_VERSION" ]]; then
        echo "ERROR: prosEO version must not be empty."
        exit 1
    fi
echo "OK: prosEO version set to ${PROSEO_VERSION}"
echo ""

echo "[1/8] Checking prerequisites"
function check_prerequisites() {
    # Homebrew
    if ! command -v brew >/dev/null 2>&1; then
        echo "ERROR: Homebrew is required."
        echo "Please install Homebrew first: https://brew.sh/"
        exit 1
    fi
    echo "OK: Homebrew is installed."

    # Docker Desktop installed?
    if [[ ! -d "/Applications/Docker.app" ]]; then
        echo "Docker Desktop is not installed. Installing..."
        brew install --cask docker
        echo "OK: Docker Desktop installed."
    else
        echo "OK: Docker Desktop is installed."
    fi

    # Docker Desktop running?
    if ! docker info >/dev/null 2>&1; then
        echo "Starting Docker Desktop..."
        open -a Docker
        echo "Waiting for Docker Desktop to start..."
        for i in {1..60}; do
            if docker info >/dev/null 2>&1; then
                break
            fi
            sleep 2
        done
        if ! docker info >/dev/null 2>&1; then
            echo "ERROR: Docker Desktop did not start successfully."
            exit 1
        fi
    fi
    echo "OK: Docker is running."

    # kubectl installed?
    if ! command -v kubectl >/dev/null 2>&1; then
        echo "kubectl is not installed. Installing..."
        brew install kubectl
        echo "OK: kubectl installed."
    else
        echo "OK: kubectl is installed."
    fi

    # Kubernetes cluster
    if ! kubectl cluster-info >/dev/null 2>&1; then
        echo "Kubernetes is not enabled or not running in Docker Desktop."
        echo ""
        echo "Please enable Kubernetes in Docker Desktop:"
        echo "Docker Desktop -> Settings -> Kubernetes -> Enable Kubernetes"
        echo ""
        read -rp "Press Enter after enabling Kubernetes..."
        echo "Waiting for Kubernetes..."
        for i in {1..90}; do
            if kubectl cluster-info >/dev/null 2>&1; then
                break
            fi
            sleep 2
        done
        if ! kubectl cluster-info >/dev/null 2>&1; then
            echo "ERROR: Kubernetes cluster is still not reachable."
            exit 1
        fi
    fi
    echo "OK: Kubernetes cluster is reachable."

    # Java 21+ installed?
    JAVA_MAJOR_VERSION=0
    JAVA_VERSION=""
    if command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        if [[ -n "$JAVA_VERSION" ]]; then
            JAVA_MAJOR_VERSION=$(printf '%s\n' "$JAVA_VERSION" | awk -F. '{
                if ($1 == 1) print $2;
                else print $1;
            }')
        fi
    fi

    if (( JAVA_MAJOR_VERSION < 21 )); then
        echo "Java 21 or later is required."
        echo "Installing OpenJDK 21..."
        brew install openjdk@21
        # Make OpenJDK 21 visible to macOS.
        sudo ln -sfn \
            "$(brew --prefix openjdk@21)/libexec/openjdk.jdk" \
            "/Library/Java/JavaVirtualMachines/openjdk-21.jdk"
        # Select Java 21 for this script.
        export JAVA_HOME=$(/usr/libexec/java_home -v 21)
        export PATH="$JAVA_HOME/bin:$PATH"

    else
        echo "OK: Java $JAVA_VERSION is installed."
        # Set JAVA_HOME if possible.
        if /usr/libexec/java_home -v "$JAVA_MAJOR_VERSION" >/dev/null 2>&1; then
            export JAVA_HOME=$(/usr/libexec/java_home -v "$JAVA_MAJOR_VERSION")
            export PATH="$JAVA_HOME/bin:$PATH"
        fi
    fi

    # Verify Java
    if ! command -v java >/dev/null 2>&1; then
        echo "ERROR: Java could not be found after installation."
        exit 1
    fi
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ -z "$JAVA_VERSION" ]]; then
        echo "ERROR: Could not determine Java version."
        exit 1
    fi
    JAVA_MAJOR_VERSION=$(printf '%s\n' "$JAVA_VERSION" | awk -F. '{
        if ($1 == 1) print $2;
        else print $1;    }')
    if (( JAVA_MAJOR_VERSION < 21 )); then
        echo "ERROR: Java 21 or later is required."
        echo "Detected Java version: $JAVA_VERSION"
        exit 1
    fi
    echo "OK: Java $JAVA_VERSION is available."
    echo "JAVA_HOME: ${JAVA_HOME:-not set}"
}
check_prerequisites
echo ""

echo "[2/8] Updating configuration according to prosEO version ${PROSEO_VERSION}"
function update_configuration() {
	escaped_proseo_version=$(escape_sed_replacement "$PROSEO_VERSION")	
    find "${SCRIPT_DIR}" -type f -name "*.template" | while IFS= read -r template; do
        target="${template%.template}"

        cp "$template" "$target"


        sed -i '' "s/proseoVersionPlaceHolder/${escaped_proseo_version}/g" "$target"
        rm -f "${target}.bak"

        echo "OK: Created $target"
    done
}
update_configuration
echo ""

echo "[3/8] Configure Kubernetes"
function configure_kubernetes() {	
    # Registry
    read -rp "Please enter your preferred local prosEO registry (e.g. localhost:5000): " REGISTRY_URL
    if [[ -z "$REGISTRY_URL" ]]; then
        echo "ERROR: Registry must not be empty."
        exit 1
    fi
    if [[ "$REGISTRY_URL" =~ [[:space:]] ]]; then
    	echo "ERROR: Registry must not contain whitespace." 
    	exit 1 
    fi
    if [[ "$REGISTRY_URL" =~ ^https?:// ]]; then
    	echo "ERROR: Enter the registry as host[:port], not as a URL."
    	echo "Example: localhost:5000" 
    	exit 1 
    fi
    if [[ "$REGISTRY_URL" == */ ]]; then
    	echo "ERROR: Registry must not end with '/'." 
    	echo "Example: localhost:5000" 
    	exit 1 
    fi
    echo "OK: prosEO registry set to '${REGISTRY_URL}'"
    
    # Storage
    echo "Please configure the shared storage path."
    printf '%s\n' \
	    "Note: " \
    	"- On macOS, the directory must be located below any of the paths available for sharing by default" \
    	"  (e. g. `/Users`), using other paths (e. g. `/opt`) does not work, even if they are declared as" \
    	"  sharable in the Docker Desktop preferences." \
	    "- On Windows it appears that the paths to use are somewhat weird, see for example this discussion:" \
  		"  https://stackoverflow.com/questions/54073794/kubernetes-persistent-volume-on-docker-desktop-windows" \
		" (However this has not been verified by the author of this script.)" 
    read -rp "Please enter where to store the prosEO data, e.g. /Users/you/prosEO/data: " SHARED_STORAGE_PATH 
    if [[ -z "$SHARED_STORAGE_PATH" ]]; then 
    	echo "ERROR: Shared storage path must not be empty."
    	 exit 1 
    fi 
    echo "OK: Shared storage path set to '${SHARED_STORAGE_PATH}'"
	
	# Headlamp dashboard
	echo "Installing and starting Headlamp..."
	kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/headlamp/main/kubernetes-headlamp.yaml
	kubectl rollout status deployment/headlamp \
	    -n kube-system \
	    --timeout=120s
	start_port_forward "kube-system" "headlamp" 8002 80
	kubectl apply -f "${SCRIPT_DIR}/kubernetes/kube-admin.yaml"
	kubectl describe secret/admin-user-secret --namespace kube-system
	echo "OK: Headlamp can be accessed at http://localhost:8002/ with the secret provided above"
	read -rp "Press Enter to confirm that you have saved the secret above."
	echo ""
	
	echo "Creating planner account..."
	kubectl apply -f "${SCRIPT_DIR}/kubernetes/planner-account.yaml"
	echo "OK: Planner account created"	
	echo ""
	
	echo "Planner authentication token:"
	kubectl describe secret/proseo-planner-secret --namespace default
	read -rp "Press Enter to confirm that you have saved the secret above."
	echo ""
	
	cd "${SCRIPT_DIR}/kubernetes"
	escaped_storage_path=$(escape_sed_replacement "$SHARED_STORAGE_PATH")
	sed "s|%SHARED_STORAGE_PATH%|${escaped_storage_path}|" < nfs-pv.yaml.template > nfs-pv.yaml
	kubectl apply -f "${SCRIPT_DIR}/kubernetes/nfs-pv.yaml"
	mkdir -p "${SHARED_STORAGE_PATH}/proseodata" "${SHARED_STORAGE_PATH}/transfer"
	cd "${SCRIPT_DIR}"
	echo "OK: Storage prepared"
}
configure_kubernetes
echo ""

echo "[4/8] Prepare docker images"
function prepare_images(){
	printf '%s\n' \
		"Note: the base images must be available in the specified registry and are not" \
		"built here, as this is a demonstrator only."
	cd "${SCRIPT_DIR}/proseo-images"
	./build_images.sh "${REGISTRY_URL}"
	./push_images.sh "${REGISTRY_URL}"
	
	cd "${SCRIPT_DIR}"
	echo "OK: Dedicated images built and pushed successfully"
}
prepare_images
echo ""

echo "[5/8] Run prosEO"
function run_proseo() {
	kubectl apply -f "${SCRIPT_DIR}/kubernetes/storage-mgr-local.yaml"
	start_port_forward "default" "storage-mgr-service" 8080 3000

	cd "${SCRIPT_DIR}/proseo-images"
	export POSTGRES_PASSWORD="demo-only"
	docker compose -p proseo up -d
	
	cd "${SCRIPT_DIR}"
	echo "OK: prosEO is running"
}
run_proseo
echo ""

echo "[6/8] Prepare database"
function prepare_database() {
	docker exec proseo-proseo-db-1 su - postgres -c 'psql proseo < /proseo/populate_mon_service_state.sql'
	echo "OK: Database prepared"
}
prepare_database
echo ""

echo "[7/8] Check the CLI"
function check_cli() {
	DEFAULT_CLI="${SCRIPT_DIR}/../ui/cli/target/proseo-ui-cli.jar" 
	CLI_PATH="$DEFAULT_CLI" 
	
	# Check whether CLI is available at the default location 
	if [[ ! -f "$DEFAULT_CLI" ]]; then 
		echo "CLI not found at: $DEFAULT_CLI" 
		echo "You can download it from:" 
		echo "https://proseo-registry.eoc.dlr.de/artifactory/prosEO/" 
		echo "" 
		read -rp "Enter the path to the proseo-ui-cli.jar and press Enter: " CLI_PATH		
		if [[ ! -f "$CLI_PATH" ]]; then 
			echo "ERROR: CLI not found at: $CLI_PATH" 
			return 1 
		fi 
	fi 
	echo "OK: CLI available at $CLI_PATH"
}
check_cli
echo ""

echo "[8/8] Configure the test mission"
function configure_ptm() {
	${SCRIPT_DIR}/proseo-images/ptm-config/create_data_local.sh "${SHARED_STORAGE_PATH}"
	java -jar "${CLI_PATH}" < "${SCRIPT_DIR}/ptm-config/cli_data_demonstrator_mac.txt"
	java -jar "$CLI_PATH" <<< "facility update localhost processingEngineToken=someverysecrettoken"
	echo "OK: test mission configured"
}
configure_ptm
echo ""

INSTALLATION_SUCCESSFUL=true

echo "The demonstrator is up and running:" 
echo "- The CLI is available at ${CLI_PATH}"
echo "- The GUI is available at localhost:8088"
echo ""
echo "Note: The demonstrator can be stopped by running stop_control_instance.sh from the proseo-images directory."