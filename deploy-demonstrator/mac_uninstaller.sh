#!/bin/bash

# Script-based uninstaller for a single-node prosEO deployment.
#
# Data is preserved by default. Use --purge-data to remove the prosEO
# data directories below SHARED_STORAGE_PATH.
# The Docker registry is preserved by default. Use --remove-registry
# to remove the local "registry" container and, optionally, its storage.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FORCE=false
PURGE_DATA=false
REMOVE_REGISTRY=false
REMOVE_REGISTRY_DATA=false

usage() {
    cat <<EOF
Usage: $0 [options]

Options:
  -f, --force            		Do not prompt for destructive operations.
      --purge-data       		Remove prosEO data under SHARED_STORAGE_PATH.
      --remove-registry  		Remove the Docker container named "registry".
      --purge-registry-data 	Also remove the registry storage directory.
  -h, --help              		Show this help.

Environment:
  SHARED_STORAGE_PATH    Storage path used by the prosEO deployment.
  REGISTRY_URL           Registry host:port, e.g. localhost:5000.
  PROSEO_LOG_DIR         prosEO log directory.
  PROSEO_PGDATA_DIR      PostgreSQL data directory.
  POSTGRES_PASSWORD      PostgreSQL password used by docker compose.
  PROSEO_PLATFORM        Docker platform used by the deployment.
  JAVA_HOME              Java installation used by the installer.
  CLI_PATH               Path to the prosEO CLI.

The uninstaller does not remove Homebrew, Docker Desktop, kubectl, Java,
the CLI JAR, or arbitrary user files unless explicitly requested above.
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -f|--force)
            FORCE=true
            shift
            ;;
        --purge-data)
            PURGE_DATA=true
            shift
            ;;
        --remove-registry)
            REMOVE_REGISTRY=true
            shift
            ;;
        --purge-registry-data)
            REMOVE_REGISTRY=true
            REMOVE_REGISTRY_DATA=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "ERROR: Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

confirm() {
    local prompt="$1"

    if [[ "$FORCE" == true ]]; then
        return 0
    fi

    read -r -p "$prompt [y/N] " answer
    [[ "$answer" =~ ^[Yy]([Ee][Ss])?$ ]]
}

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "ERROR: Required command '$1' was not found."
        exit 1
    fi
}

echo "============================================================"
echo " prosEO uninstaller"
echo "============================================================"
echo ""

# Restore the variables printed by the installer if the caller exported them.
# Do not overwrite values supplied by the caller.
SHARED_STORAGE_PATH="${SHARED_STORAGE_PATH:-}"
REGISTRY_URL="${REGISTRY_URL:-}"
PROSEO_LOG_DIR="${PROSEO_LOG_DIR:-}"
PROSEO_PGDATA_DIR="${PROSEO_PGDATA_DIR:-}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-}"
PROSEO_PLATFORM="${PROSEO_PLATFORM:-}"
JAVA_HOME="${JAVA_HOME:-}"
CLI_PATH="${CLI_PATH:-}"

echo "Using environment:"
printf '  SHARED_STORAGE_PATH=%q\n' "$SHARED_STORAGE_PATH"
printf '  REGISTRY_URL=%q\n' "$REGISTRY_URL"
printf '  PROSEO_LOG_DIR=%q\n' "$PROSEO_LOG_DIR"
printf '  PROSEO_PGDATA_DIR=%q\n' "$PROSEO_PGDATA_DIR"
printf '  PROSEO_PLATFORM=%q\n' "$PROSEO_PLATFORM"
printf '  JAVA_HOME=%q\n' "$JAVA_HOME"
printf '  CLI_PATH=%q\n' "$CLI_PATH"
echo ""

require_command kubectl
require_command docker

# The installer creates the compose project from this directory.
COMPOSE_DIR="${SCRIPT_DIR}/proseo-images"

echo "[1/5] Stopping prosEO containers"

if [[ -d "$COMPOSE_DIR" ]]; then
    (
        cd "$COMPOSE_DIR"

        # Use the same project name as the installer. Do not use -v here:
        # named volumes, if any, should not be removed implicitly.
        if docker compose -p proseo ps -q 2>/dev/null | grep -q .; then
            docker compose -p proseo down
        else
            echo "No running/stopped containers found for compose project 'proseo'."
        fi
    )
else
    echo "Compose directory not found: $COMPOSE_DIR"
    echo "Trying to remove the project by name anyway."
    docker compose -p proseo down 2>/dev/null || true
fi

echo ""
echo "[2/5] Removing Kubernetes resources"

# These are the resources installed by configure_kubernetes() and run_proseo().
# Ignore missing resources so the uninstaller remains idempotent.
kubectl delete -f "${SCRIPT_DIR}/kubernetes/storage-mgr-local.yaml" \
    --ignore-not-found=true 2>/dev/null || true

if [[ -f "${SCRIPT_DIR}/kubernetes/nfs-pv.yaml" ]]; then
    kubectl delete -f "${SCRIPT_DIR}/kubernetes/nfs-pv.yaml" \
        --ignore-not-found=true 2>/dev/null || true
else
    echo "Generated nfs-pv.yaml not found; deleting the prosEO NFS PV/PVC by name if present."
    kubectl delete pv proseodata-pv --ignore-not-found=true 2>/dev/null || true
fi

kubectl delete -f "${SCRIPT_DIR}/kubernetes/planner-account.yaml" \
    --ignore-not-found=true 2>/dev/null || true

# Headlamp was installed from the upstream manifest. Delete only the
# Headlamp deployment/service resources and the admin account created by
# the prosEO installer. Do not delete unrelated kube-system resources.
kubectl delete -f \
    "https://raw.githubusercontent.com/kubernetes-sigs/headlamp/main/kubernetes-headlamp.yaml" \
    --ignore-not-found=true 2>/dev/null || true

kubectl delete secret admin-user-secret -n kube-system \
    --ignore-not-found=true 2>/dev/null || true

# Stop the storage-manager pod if a generated/older resource remains.
kubectl delete pod -n default -l name=storage-mgr \
    --ignore-not-found=true 2>/dev/null || true

echo ""
echo "[3/5] Cleaning up port-forward processes"

# Port forwards are started with kubectl and are not Kubernetes resources.
# Identify only kubectl port-forward processes using the ports used by the
# installer, rather than killing every kubectl process.
for port in 8002 8080; do
    pids="$(pgrep -f "kubectl port-forward.*${port}:" 2>/dev/null || true)"

    if [[ -n "$pids" ]]; then
        echo "Stopping port-forward process(es) for local port ${port}: ${pids}"
        kill $pids 2>/dev/null || true
    else
        echo "No prosEO port-forward found for local port ${port}."
    fi
done

echo ""
echo "[4/5] Registry cleanup"

if [[ "$REMOVE_REGISTRY" == true ]]; then
    registry_mount="$(
        docker inspect -f '{{range .Mounts}}{{if eq .Destination "/var/lib/registry"}}{{.Source}}{{end}}{{end}}' \
            registry 2>/dev/null || true
    )"

    if docker container inspect registry >/dev/null 2>&1; then
        if confirm "Remove Docker registry container 'registry'?"; then
            docker rm -f registry
            echo "OK: Registry container removed."
            
		    if [[ "$REMOVE_REGISTRY_DATA" == true && -n "$registry_mount" ]]; then
			    case "$registry_mount" in
			        /|"$HOME"|"$SHARED_STORAGE_PATH")
			            echo "Refusing to remove suspicious registry storage path: $registry_mount"
			            ;;
			        *)
			            if confirm "Remove registry storage directory '$registry_mount'?"; then
			                rm -rf -- "$registry_mount"
			                echo "OK: Registry storage removed."
			            else
			                echo "Keeping registry storage."
			            fi
			            ;;
			    esac
			else
			    echo "No registry data was removed."
			fi
        else
            echo "Keeping registry container."
            REMOVE_REGISTRY=false
        fi
    else
        echo "Docker registry container 'registry' does not exist."
    fi
else
    echo "Keeping Docker registry container and its storage."
    echo "Use --remove-registry to remove the container."
fi

echo ""
echo "[5/5] Data cleanup"

if [[ "$PURGE_DATA" == true ]]; then
    if [[ ! -d "$SHARED_STORAGE_PATH" ]]; then
    	echo "ERROR: SHARED_STORAGE_PATH does not exist or is not a directory."
        exit 1
    fi

	require_command realpath
	resolved_path="$(realpath "$SHARED_STORAGE_PATH")"

	case "$resolved_path" in
	    "/"|"/Users"|"/Volumes"|"/tmp"|"/private"|"/private/tmp")
	        echo "ERROR: Refusing to purge unsafe path: $resolved_path"
	        exit 1
	        ;;
	esac

    echo "The following prosEO data will be removed:"
    echo "  ${resolved_path}/proseodata"
    echo "  ${resolved_path}/transfer"
    echo "  ${resolved_path}/log"
    echo "  ${resolved_path}/pgdata"
    echo ""

	expected_found=false

	for dir in proseodata transfer log pgdata; do
	    if [[ -d "${resolved_path}/${dir}" ]]; then
	        expected_found=true
	        break
	    fi
	done
	
	if [[ "$expected_found" != true ]]; then
	    echo "ERROR: No expected prosEO data directories found under: $resolved_path"
	    exit 1
	fi

    if confirm "Permanently remove these directories?"; then
		rm -rf -- \
		    "${resolved_path}/proseodata" \
		    "${resolved_path}/transfer" \
		    "${resolved_path}/log" \
		    "${resolved_path}/pgdata"
        echo "OK: prosEO data removed."
    else
        echo "Keeping prosEO data."
    fi
else
    echo "Keeping prosEO data."
    echo "Use --purge-data to remove data below SHARED_STORAGE_PATH."
fi

echo ""
echo "prosEO uninstallation completed."
echo ""
