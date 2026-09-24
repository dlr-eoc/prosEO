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
    echo ""
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

# search for prosEO error codes
function check_for_errors() {
    local output
    output=$("$@" 2>&1)
    printf '%s\n' "$output"
    if grep -qE '\(E[0-9]+\)' <<< "$output"; then
        echo "ERROR: CLI reported an error."
        return 1
    fi
    return 0
}

# -------

TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_FILE="${SCRIPT_DIR}/install-${TIMESTAMP}.log"
exec > >(tee "$LOG_FILE") 2>&1

# A force option is provided to skip prompts where reasonable
FORCE=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        -f|--force)
            FORCE=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            return 1
            ;;
    esac
done


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

echo ""

echo "============================================================"
echo "[1/7] Checking prerequisites"
echo "============================================================"
function check_prerequisites() {
    # Homebrew
    if ! command -v brew >/dev/null 2>&1; then
        echo "ERROR: Homebrew is required."
        echo "Please install Homebrew first: https://brew.sh/"
        return 1
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
            return 1
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
	    echo "Kubernetes is not currently reachable."
	    echo "Waiting for Kubernetes..."
	    kubernetes_ready=false
	
	    for i in {1..30}; do
	        if kubectl cluster-info >/dev/null 2>&1; then
	            kubernetes_ready=true
	            break
	        fi	
	        printf '.'
	        sleep 2
	    done
	
	    if $kubernetes_ready; then
	        echo "OK: Kubernetes cluster is reachable."
	    else
	        echo "Kubernetes is not enabled or not running in Docker Desktop."
	        echo
	        echo "Please enable Kubernetes in Docker Desktop:"
	        echo "Docker Desktop -> Settings -> Kubernetes -> Enable Kubernetes -> Install"
	        echo "-> Create a single-node cluster of type kubeadm"	
	        read -rp "Press Enter after enabling Kubernetes..."
	
	        echo "Waiting for Kubernetes..."	
	        for i in {1..90}; do
	            if kubectl cluster-info >/dev/null 2>&1; then
	                kubernetes_ready=true
	                break
	            fi	
	            printf '.'
	            sleep 2
	        done
	
	        if ! $kubernetes_ready; then
	            echo "ERROR: Kubernetes cluster is still not reachable."
	            return 1
	        fi	
	        echo "OK: Kubernetes cluster is reachable."
	    fi
	fi

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
        return 1
    fi
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ -z "$JAVA_VERSION" ]]; then
        echo "ERROR: Could not determine Java version."
        return 1
    fi
    JAVA_MAJOR_VERSION=$(printf '%s\n' "$JAVA_VERSION" | awk -F. '{
        if ($1 == 1) print $2;
        else print $1;    }')
    if (( JAVA_MAJOR_VERSION < 21 )); then
        echo "ERROR: Java 21 or later is required."
        echo "Detected Java version: $JAVA_VERSION"
        return 1
    fi
    echo "OK: Java $JAVA_VERSION is available."
    echo "JAVA_HOME: ${JAVA_HOME:-not set}"
}
check_prerequisites
echo ""
sleep 1

echo "============================================================"
echo "[2/7] Configure Kubernetes"
echo "============================================================"
function configure_kubernetes() {
	# Registry
	if docker container inspect registry >/dev/null 2>&1; then
	    echo "A local docker registry already exists."	
	    if [[ "$(docker inspect -f '{{.State.Running}}' registry)" != "true" ]]; then
	        echo "Starting existing Docker registry..."
	        docker start registry >/dev/null
	    fi
	
	    # Extract the host address and port to which the registry's container port 5000/tcp is published.
	    REGISTRY_PORT_MAPPING="$(
	        docker inspect -f '{{with (index .NetworkSettings.Ports "5000/tcp")}}{{(index . 0).HostIp}}:{{(index . 0).HostPort}}{{end}}' \
	            registry
	    )"	
	    if [[ -z "$REGISTRY_PORT_MAPPING" ]]; then
	        echo "ERROR: Existing registry container has no published port for 5000/tcp."
	        return 1
	    fi	
	    REGISTRY_HOST="${REGISTRY_PORT_MAPPING%:*}"
	    REGISTRY_PORT="${REGISTRY_PORT_MAPPING##*:}"
	
	    # Docker may report 0.0.0.0 or :: for a port published on all interfaces. Use localhost for the local-registry URL.
	    case "$REGISTRY_HOST" in
	        0.0.0.0|::|\[::\])
	            REGISTRY_HOST="localhost"
	            ;;
	    esac
	    REGISTRY_URL="${REGISTRY_HOST}:${REGISTRY_PORT}"	
	else
		REGISTRY_URL="localhost:5000"
	    if [[ -z "$REGISTRY_DIR" ]]; then
	        read -rp "A local docker registry will be configured at localhost:5000. Please supply a storage directory: \
	        	(Can also be exported as REGISTRY_DIR.)" REGISTRY_DIR
	    fi
	    if [[ -z "$REGISTRY_DIR" ]]; then
	        echo "ERROR: Directory path must not be empty."
	        return 1
	    fi
	    REGISTRY_DIR="${REGISTRY_DIR%/}" # Remove trailing '/' characters.
	    REGISTRY_DIR="${REGISTRY_DIR/#\~/$HOME}" # Expand '~' if necessary.
	    if ! mkdir -p "$REGISTRY_DIR"; then
	        echo "ERROR: Could not create registry storage directory: $REGISTRY_DIR"
	        return 1
	    fi	
	    echo "Using registry storage directory: $REGISTRY_DIR"

	    if ! docker run -d \
		    -p 5000:5000 \
		    --restart always \
	        -e REGISTRY_STORAGE_DELETE_ENABLED=true \
	        -v "${REGISTRY_DIR}:/var/lib/registry" \
	        --name registry \
	        registry:2; then
	        echo "ERROR: Could not start Docker registry."
	        return 1
	    fi	
	    echo "Waiting for Docker registry..."
	
	    for i in {1..30}; do
	        if curl -fsS "http://${REGISTRY_URL}/v2/" >/dev/null 2>&1; then
	            break
	        fi
	        sleep 1
	    done
	    if ! curl -fsS "http://${REGISTRY_URL}/v2/" >/dev/null 2>&1; then
	        echo "ERROR: Docker registry could not be reached at ${REGISTRY_URL}."
	        return 1
	    fi
	fi	
	echo "OK: prosEO registry available at ${REGISTRY_URL}"
	echo ""
    
    # Storage
    if [[ -z "${SHARED_STORAGE_PATH:-}" ]]; then
    	echo "Please enter where to store the prosEO data, e.g. /Users/you/prosEO/data:"
    	echo "(Can also be exported as SHARED_STORAGE_PATH.)" 
    	echo "	Note: On macOS, the directory must be located below (e. g. '/Users'),"
    	echo "	using other paths (e. g. '/opt') does not work, even if they are declared "
    	echo "	as sharable in the Docker Desktop preferences."
    	read -rp "> " SHARED_STORAGE_PATH
    fi 
    if [[ -z "$SHARED_STORAGE_PATH" ]]; then 
    	echo "ERROR: Shared storage path must not be empty."
    	return 1 
    fi 
    export SHARED_STORAGE_PATH
	export REGISTRY_URL
    echo "OK: Shared storage path set to '${SHARED_STORAGE_PATH}'"
    echo ""
	
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
	echo ""
	if [[ "$FORCE" != true ]]; then
	    echo "Press Enter to confirm that you have saved the Headlamp secret above."
	    echo "Note: Secrets will also be available in the log of this run. By setting"
	    echo "the -f/--force flag you will no longer be prompted to retreive them interactively."
	    read -rp ""
	    echo ""
	fi
	echo ""
	
	# Planner account
	echo "Creating planner account..."
	kubectl apply -f "${SCRIPT_DIR}/kubernetes/planner-account.yaml"
	echo "OK: Planner account created"	
	echo ""
	
	echo "Planner authentication token:"
	kubectl describe secret/proseo-planner-secret --namespace default
	echo ""
	if [[ "$FORCE" != true ]]; then
	    echo "Press Enter to confirm that you have saved the planner secret above."
	    echo "Note: Secrets will also be available in the log of this run. By setting"
	    echo "the -f/--force flag you will no longer be prompted to retreive them interactively."
	    read -rp ""
	    echo ""
	fi

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
sleep 1

echo "============================================================"
echo "[3/7] Prepare docker images"
echo "============================================================"
function prepare_images(){	
	# check whether required base images are available	
	COMPONENT_DIR="$(cd "${SCRIPT_DIR}/proseo-images/proseo-components" && pwd)"
	all_available=true
	for component_dir in "$COMPONENT_DIR"/*; do
	    [ -d "$component_dir" ] || continue	
	    	dockerfile="$component_dir/Dockerfile"	
	
	    # extract the image name
	    image=$(awk '
	        /^[[:space:]]*FROM[[:space:]]/ {
	            print $2
	            exit
	        }
	    ' "$dockerfile")
	
	    echo -n "[CHECK] $(basename "$component_dir"): $image ... "
	
	    if docker pull "$image" >/dev/null 2>&1; then
	        echo "OK"
	    else
	        echo "NOT FOUND"
	        all_available=false
	    fi
	done

	# build the base images if any are unavailable
	if [[ "$all_available" == false ]]; then
		cd "${SCRIPT_DIR}/.."		
		
		PROSEO_VERSION=$(sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p' "${SCRIPT_DIR}/../pom.xml" | head -1)
		
		if [[ "$FORCE" != true ]]; then
			echo ""
			echo "Currently, you have checked out prosEO version $PROSEO_VERSION."
			echo "Does your repository point to the commit corresponding to the desired stable prosEO version?"
			read -rp "Please enter to confirm. Note: This prompt can be skipped by setting the -f/--force flag."
			
		    echo ""
		fi

		echo ""
		echo "Installing base images ..."		
		mvn clean install -Dmaven.test.skip=true > /dev/null
		
		docker images --format '{{.Repository}}:{{.Tag}}' | grep -E ":${PROSEO_VERSION//./\\.}$" |
		while read -r image; do
		    repository="${image%:*}"
		    docker tag "$image" "${repository}:demo"
		    docker push "${repository}:demo"
		done
	fi
		
	export PROSEO_PLATFORM="linux/arm64"
	cd "${SCRIPT_DIR}/proseo-images"
	./build_images.sh "${REGISTRY_URL}"
	./push_images.sh "${REGISTRY_URL}"
	
	cd "${SCRIPT_DIR}"
	echo "OK: Configuration-specific images built and pushed successfully"
}
prepare_images
echo ""
sleep 1

echo "============================================================"
echo "[4/7] Run prosEO"
echo "============================================================"
function run_proseo() {
	# Run storage manager
	kubectl delete pod -n default -l name=storage-mgr --ignore-not-found
	kubectl apply -f "${SCRIPT_DIR}/kubernetes/storage-mgr-local.yaml"
	echo "Waiting for the storage manager to become available ..."
	kubectl wait --for=condition=ready pod -l name=storage-mgr -n default --timeout=120s
	start_port_forward "default" "storage-mgr-service" 8080 3000

	# Prepare log directory
	PROSEO_LOG_DIR="${SHARED_STORAGE_PATH}/log"
	mkdir -p "$PROSEO_LOG_DIR"
	export PROSEO_LOG_DIR
	
	# Prepare pgdata directory
	PROSEO_PGDATA_DIR="${SHARED_STORAGE_PATH}/pgdata"
	mkdir -p "$PROSEO_PGDATA_DIR"
	export PROSEO_PGDATA_DIR
	
	# Run other microservices
	cd "${SCRIPT_DIR}/proseo-images"
	export POSTGRES_PASSWORD="demo-only"
	docker compose -p proseo up -d
	
	cd "${SCRIPT_DIR}"
	echo "OK: prosEO is running"
}
run_proseo
echo ""
sleep 1

echo "============================================================"
echo "[5/7] Prepare database"
echo "============================================================"
function prepare_database() {
	echo "Waiting for PostgreSQL..."
	for i in {1..60}; do
	    if docker compose -p proseo exec -T proseo-db pg_isready -U postgres >/dev/null 2>&1; then
	        break
	    fi	
	    printf '.'
	    sleep 1
	done	
	if ! docker compose -p proseo exec -T proseo-db pg_isready -U postgres >/dev/null 2>&1; then
	    echo "ERROR: PostgreSQL did not become ready after 60 seconds."
	    return 1
	fi
	
	if ! docker compose -p proseo exec -T proseo-db su - postgres -c 'psql proseo < /proseo/populate_mon_service_state.sql'
	then
	    echo "ERROR: Database preparation failed."
	    return 1
	fi
	echo "OK: Database prepared"
}
prepare_database
echo ""
sleep 1

echo "============================================================"
echo "[6/7] Check the CLI availability"
echo "============================================================"
function check_cli() {
	CLI_PATH="${SCRIPT_DIR}/../ui/cli/target/proseo-ui-cli.jar" 
	if [[ ! -f "$CLI_PATH" ]]; then 
		# build CLI if unavailable
		cd "${SCRIPT_DIR}/../ui"		
		
		PROSEO_VERSION=$(sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p' "${SCRIPT_DIR}/../pom.xml" | head -1)
		
		if [[ "$FORCE" != true ]]; then
			echo ""
			echo "CLI will be installed. Currently, you have checked out prosEO version $PROSEO_VERSION."
			echo "Does your repository point to the commit corresponding to the desired stable prosEO version?"
			read -rp "Please enter to confirm. Note: This prompt can be skipped by setting the -f/--force flag."
			
		    echo ""
		fi

		echo ""
		echo "Installing CLI ..."		
		mvn clean install -Dmaven.test.skip=true > /dev/null
		
		if [[ ! -f "$CLI_PATH" ]]; then 
			echo "ERROR: CLI not found at: $CLI_PATH" 
			return 1 
		fi 
	fi
	export CLI_PATH
	echo "OK: CLI available at $CLI_PATH"
}
check_cli
echo ""
sleep 1

echo "============================================================"
echo "[7/7] Configure the test mission"
echo "============================================================"
function configure_ptm() {
	cd ${SCRIPT_DIR}/ptm-config
	if ! "./configure_proseo_test_mission.pl"; then
		return 1
	fi
	if ! check_for_errors java -jar "${CLI_PATH}" <cli_script.txt; then
		return 1
	fi
	if ! "${SCRIPT_DIR}/ptm-config/create_data_demonstrator_mac.sh" "${SHARED_STORAGE_PATH}"; then
		return 1
	fi
	if ! check_for_errors java -jar "${CLI_PATH}" -i"${SCRIPT_DIR}/ptm-config/testfiles/proseo.cred" -mPTM < "${SCRIPT_DIR}/ptm-config/cli_data_demonstrator_mac.txt"; then
		return 1
	fi
	if ! check_for_errors java -jar "$CLI_PATH" -i"${SCRIPT_DIR}/ptm-config/testfiles/proseo.cred" -mPTM <<< "facility update localhost processingEngineToken=someverysecrettoken"; then
		return 1
	fi
	cd ${SCRIPT_DIR}
	echo "OK: test mission configured"
}
configure_ptm
echo ""
sleep 1

INSTALLATION_SUCCESSFUL=true

echo "The demonstrator is up and running:" 
echo "- The CLI is available at ${CLI_PATH}"
echo "- The GUI is available at localhost:8088"
echo ""
echo "Note: An uninstaller is available. Restore the environment as below if you have" 
echo "terminated the console in between. All commands are available in the log."

function print_environment() {
    echo ""
    echo "============================================================"
    echo " prosEO environment"
    echo "============================================================"
    echo ""
    echo "The following commands can be used to restore the prosEO"
    echo "environment in another terminal, e.g. for the uninstaller:"
    echo ""

    printf 'export SHARED_STORAGE_PATH=%q\n' "${SHARED_STORAGE_PATH:-}"
    printf 'export REGISTRY_URL=%q\n' "${REGISTRY_URL:-}"
    printf 'export PROSEO_LOG_DIR=%q\n' "${PROSEO_LOG_DIR:-}"
    printf 'export PROSEO_PGDATA_DIR=%q\n' "${PROSEO_PGDATA_DIR:-}"
    printf 'export POSTGRES_PASSWORD=%q\n' "${POSTGRES_PASSWORD:-}"
    printf 'export PROSEO_PLATFORM=%q\n' "${PROSEO_PLATFORM:-}"
    printf 'export JAVA_HOME=%q\n' "${JAVA_HOME:-}"
    printf 'export CLI_PATH=%q\n' "${CLI_PATH:-}"

    echo ""
    echo "============================================================"
    echo ""
}
print_environment