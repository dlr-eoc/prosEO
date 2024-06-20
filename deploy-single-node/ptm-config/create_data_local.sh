#!/bin/bash
#
# create_data_local.sh
# --------------------
#
# Usage: create_data_local.sh <Storage Manager image tag> <path to shared storage>
#
# Create Kubernetes services on a local Docker Desktop instance
# and dynamic test data for the prosEO test mission:
# - L0 input data
# - IERSB AUX input data
# - a processing facility on Docker Desktop
# - a processing order for L2 products
# - a processing order for L3 products
#

# -------------------------
# Check parameters
# -------------------------
STORAGE_MGR_TAG=$1
SHARED_STORAGE_PATH=$2

if [ x$STORAGE_MGR_TAG = x -o x$SHARED_STORAGE_PATH = x ] ; then
	echo "Usage: $0 <Storage Manager image tag> <path to shared storage>"
	exit 1
fi

# -------------------------
# Tag Storage Manager image
# -------------------------
docker tag localhost:5000/proseo-storage-mgr:$STORAGE_MGR_TAG localhost:5000/proseo-storage-mgr:latest
docker push localhost:5000/proseo-storage-mgr:latest

# -------------------------
# Prepare local file server
# -------------------------

# File server is on "hostPath"
# Update the path in the Persistent Volume configuration
sed "s|%SHARED_STORAGE_PATH%|${SHARED_STORAGE_PATH}|" <../kubernetes/nfs-pv.yaml.template >../kubernetes/nfs-pv.yaml
# Create the Persistent Volumes
kubectl apply -f ../kubernetes/nfs-pv.yaml

# Simulated "internal" POSIX storage area (must correspond to the specs in nfs-server-local.yaml)
mkdir -p ${SHARED_STORAGE_PATH}/proseodata

# Simulated "external" mount point for product ingestion (must correspond to the specs in nfs-server-local.yaml)
mkdir -p ${SHARED_STORAGE_PATH}/transfer

# Ingest mount point in storage manager (must correspond to the specs in storage-mgr-local.yaml)
INGEST_MOUNT_POINT=/mnt

# -------------------------
# Create L0/AUX input data
# -------------------------

# Create empty subdirectory for test data
TEST_DATA_DIR=testproducts
mkdir -p $TEST_DATA_DIR
rm -rf $TEST_DATA_DIR/*

INGEST_DIR=$TEST_DATA_DIR/transfer
FILE_PATH=import/products

# Products consist of the fields id, type, start time, stop time, generation time and revision,
# separated by vertical bars
# L0 products are in 45 min slices, starting with orbit 3000
mkdir -p ${INGEST_DIR}/${FILE_PATH}
cd ${INGEST_DIR}/${FILE_PATH}

cat >PTM_L0_20191104090000_20191104094500_20191104120000.RAW <<EOF
1234567|PTM_L0|2019-11-04T09:00:00Z|2019-11-04T09:45:00Z|2019-11-04T12:00:00Z|1
EOF

cat >PTM_L0_20191104094500_20191104103000_20191104120100.RAW <<EOF
1234567|PTM_L0|2019-11-04T09:45:00Z|2019-11-04T10:30:00Z|2019-11-04T12:01:00Z|1
EOF

cat >PTM_L0_20191104103000_20191104111500_20191104120200.RAW <<EOF
1234567|PTM_L0|2019-11-04T10:30:00Z|2019-11-04T11:15:00Z|2019-11-04T12:02:00Z|1
EOF

cat >PTM_L0_20191104111500_20191104120000_20191104120300.RAW <<EOF
1234567|PTM_L0|2019-11-04T11:15:00Z|2019-11-04T12:00:00Z|2019-11-04T12:03:00Z|1
EOF

cat >PTM_L0_20191104120000_20191104124500_20191104150000.RAW <<EOF
1234567|PTM_L0|2019-11-04T12:00:00Z|2019-11-04T12:45:00Z|2019-11-04T15:00:00Z|1
EOF

cat >PTM_L0_20191104124500_20191104133000_20191104150100.RAW <<EOF
1234567|PTM_L0|2019-11-04T12:45:00Z|2019-11-04T13:30:00Z|2019-11-04T15:01:00Z|1
EOF

cat >PTM_L0_20191104133000_20191104141500_20191104150200.RAW <<EOF
1234567|PTM_L0|2019-11-04T13:30:00Z|2019-11-04T14:15:00Z|2019-11-04T15:02:00Z|1
EOF

cat >PTM_L0_20191104141500_20191104150000_20191104150300.RAW <<EOF
1234567|PTM_L0|2019-11-04T14:15:00Z|2019-11-04T15:00:00Z|2019-11-04T15:03:00Z|1
EOF

cat >PTM_L0_20191104150000_20191104154500_20191104180000.RAW <<EOF
1234567|PTM_L0|2019-11-04T15:00:00Z|2019-11-04T15:45:00Z|2019-11-04T18:00:00Z|1
EOF

cat >PTM_L0_20191104154500_20191104163000_20191104180100.RAW <<EOF
1234567|PTM_L0|2019-11-04T15:45:00Z|2019-11-04T16:30:00Z|2019-11-04T18:01:00Z|1
EOF

cat >PTM_L0_20191104163000_20191104171500_20191104180200.RAW <<EOF
1234567|PTM_L0|2019-11-04T16:30:00Z|2019-11-04T17:15:00Z|2019-11-04T18:02:00Z|1
EOF

cat >PTM_L0_20191104171500_20191104180000_20191104180300.RAW <<EOF
1234567|PTM_L0|2019-11-04T17:15:00Z|2019-11-04T18:00:00Z|2019-11-04T18:03:00Z|1
EOF

cat >PTM_L0_20191104180000_20191104184500_20191104210000.RAW <<EOF
1234567|PTM_L0|2019-11-04T18:00:00Z|2019-11-04T18:45:00Z|2019-11-04T21:00:00Z|1
EOF

cat >PTM_L0_20191104184500_20191104193000_20191104210100.RAW <<EOF
1234567|PTM_L0|2019-11-04T18:45:00Z|2019-11-04T19:30:00Z|2019-11-04T21:01:00Z|1
EOF

cd -

# IERSB AUX products are real-world data:
# Using bulletinb-380.xml
cp -p bulletinb-380.xml ${INGEST_DIR}/${FILE_PATH}

# Copy test data into file server
cp -pR ${INGEST_DIR}/* ${SHARED_STORAGE_PATH}/transfer/


# -------------------------
# Create Storage Manager
# -------------------------

# Create the storage manager in the local Minikube
kubectl apply -f ../kubernetes/storage-mgr-local.yaml


# -------------------------
# Create Kubernetes Dashboard
# -------------------------

# Create a dashboard at http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
kubectl apply -f ../kubernetes/kubernetes-dashboard.yaml
kubectl proxy --accept-hosts='.*' &


# -------------------------
# Create prosEO config files
# -------------------------

# Create a new CLI command script
CLI_SCRIPT=cli_data_script.txt
echo "" >$CLI_SCRIPT

# Create a processing facility
cat >$TEST_DATA_DIR/facility.json <<EOF
{
    "name": "localhost",
    "description": "Docker Desktop Minikube",
    "facilityState": "RUNNING",
    "processingEngineUrl": "http://host.docker.internal:8001/",
    "processingEngineToken": "TBD",
    "storageManagerUrl": 
    	"http://host.docker.internal:8001/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1",
    "localStorageManagerUrl": "http://storage-mgr-service.default.svc.cluster.local:3000/proseo/storage-mgr/v0.1",
    "externalStorageManagerUrl": 
        "http://localhost:8001/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1",
    "storageManagerUser": "smuser",
    "storageManagerPassword": "smpwd-but-that-would-be-way-too-short",
    "defaultStorageType": "POSIX"
}
EOF
echo "Set authentication token for processing facility 'localhost' manually!"
echo "facility create --file=$TEST_DATA_DIR/facility.json" >>$CLI_SCRIPT

# Ingest test data into prosEO
echo "ingest --file=$TEST_DATA_DIR/ingest_products.json localhost" >>$CLI_SCRIPT
cat >$TEST_DATA_DIR/ingest_products.json <<EOF
[
    {
        "missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T09:00:00.000000",
        "sensingStopTime": "2019-11-04T09:45:00.000000",
        "generationTime": "2019-11-04T12:00:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104090000_20191104094500_20191104120000.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "76d31dc84a24dd2b34eea488de24c2f8",
    	"checksumTime": "2019-11-04T12:00:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T09:45:00.000000",
        "sensingStopTime": "2019-11-04T10:30:00.000000",
        "generationTime": "2019-11-04T12:01:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104094500_20191104103000_20191104120100.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "be03bdf9e2ff6433af2ffc9c6e8376e2",
    	"checksumTime": "2019-11-04T12:01:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T10:30:00.000000",
        "sensingStopTime": "2019-11-04T11:15:00.000000",
        "generationTime": "2019-11-04T12:02:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104103000_20191104111500_20191104120200.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "2883a21e909d3891d8f5fd7b2fed0683",
    	"checksumTime": "2019-11-04T12:02:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T11:15:00.000000",
        "sensingStopTime": "2019-11-04T12:00:00.000000",
        "generationTime": "2019-11-04T12:03:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104111500_20191104120000_20191104120300.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "fc69dd62825413c1262920bdadf8cedd",
    	"checksumTime": "2019-11-04T12:03:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T12:00:00.000000",
        "sensingStopTime": "2019-11-04T12:45:00.000000",
        "generationTime": "2019-11-04T15:00:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104120000_20191104124500_20191104150000.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "1ad62add86fcfba32ca394db1ef059c0",
    	"checksumTime": "2019-11-04T15:00:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T12:45:00.000000",
        "sensingStopTime": "2019-11-04T13:30:00.000000",
        "generationTime": "2019-11-04T15:01:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104124500_20191104133000_20191104150100.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "46eeb6be8f41134736b68d655c3d026d",
    	"checksumTime": "2019-11-04T15:01:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T13:30:00.000000",
        "sensingStopTime": "2019-11-04T14:15:00.000000",
        "generationTime": "2019-11-04T15:02:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104133000_20191104141500_20191104150200.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "2c6d93c9f3131329672ca27f7da74010",
    	"checksumTime": "2019-11-04T15:02:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T14:15:00.000000",
        "sensingStopTime": "2019-11-04T15:00:00.000000",
        "generationTime": "2019-11-04T15:03:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104141500_20191104150000_20191104150300.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "f1f2b02c2c27665ff25c1c68de4503e0",
    	"checksumTime": "2019-11-04T15:03:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T15:00:00.000000",
        "sensingStopTime": "2019-11-04T15:45:00.000000",
        "generationTime": "2019-11-04T18:00:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104150000_20191104154500_20191104180000.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "5ac3ddf9dfad802c3e1c2a86bb8b7959",
    	"checksumTime": "2019-11-04T18:00:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T15:45:00.000000",
        "sensingStopTime": "2019-11-04T16:30:00.000000",
        "generationTime": "2019-11-04T18:01:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104154500_20191104163000_20191104180100.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "21c5b324468622de13f53d677322b2e5",
    	"checksumTime": "2019-11-04T18:01:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T16:30:00.000000",
        "sensingStopTime": "2019-11-04T17:15:00.000000",
        "generationTime": "2019-11-04T18:02:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104163000_20191104171500_20191104180200.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "e55f131285387a23ca9b5f47ac32808f",
    	"checksumTime": "2019-11-04T18:02:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T17:15:00.000000",
        "sensingStopTime": "2019-11-04T18:00:00.000000",
        "generationTime": "2019-11-04T18:03:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104171500_20191104180000_20191104180300.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "44742febb6c4f31d9680720363918e8e",
    	"checksumTime": "2019-11-04T18:03:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T18:00:00.000000",
        "sensingStopTime": "2019-11-04T18:45:00.000000",
        "generationTime": "2019-11-04T21:00:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104180000_20191104184500_20191104210000.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "2710894af465ea542cdda92a5fc48205",
    	"checksumTime": "2019-11-04T21:00:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "PTM_L0",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-11-04T18:45:00.000000",
        "sensingStopTime": "2019-11-04T19:30:00.000000",
        "generationTime": "2019-11-04T21:01:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "PTM_L0_20191104184500_20191104193000_20191104210100.RAW",
        "auxFileNames": [],
    	"fileSize": 80,
    	"checksum": "82b53742a4fe9d04ceaa8df0fb41090c",
    	"checksumTime": "2019-11-04T21:01:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "AUX_IERS_B",
        "fileClass": "OPER",
        "mode": "OPER",
        "productQuality": "TEST",
        "sensingStartTime": "2019-09-01T00:00:00.000000",
        "sensingStopTime": "2019-10-01T00:00:00.000000",
        "generationTime": "2019-10-01T12:00:00.000000",
        "parameters": [
            {
                "key": "revision",
                "parameterType": "INTEGER",
                "parameterValue": "1"
            }
        ],
        "sourceStorageType": "POSIX",
        "mountPoint": "${INGEST_MOUNT_POINT}",
        "filePath": "${FILE_PATH}",
        "productFileName": "bulletinb-380.xml",
        "auxFileNames": [],
    	"fileSize": 51090,
    	"checksum": "b754f424e3dad8f1c107ed6b8ad9d06a",
    	"checksumTime": "2019-10-01T12:01:00.000000"
    }
]
EOF

# Create a L2 processing order
echo "order create --file=$TEST_DATA_DIR/order_l2.json" >>$CLI_SCRIPT
cat >$TEST_DATA_DIR/order_l2.json <<EOF
{
    "missionCode": "PTM",
    "identifier": "L2_orbits_3000-3002",
    "orderState": "INITIAL",
    "slicingType": "ORBIT",
    "sliceOverlap": 0,
    "inputFilters": [
    	{
    		"productClass": "PTM_L0",
		    "filterConditions": [
		        {
		            "key": "revision",
		            "parameterType": "INTEGER",
		            "parameterValue": "1"
		        },
		        {
		            "key": "fileClass",
		            "parameterType": "STRING",
		            "parameterValue": "OPER"
		        }
		    ]
	    },
    	{
    		"productClass": "PTM_L1B_P1",
		    "filterConditions": [
		        {
		            "key": "revision",
		            "parameterType": "INTEGER",
		            "parameterValue": "2"
		        },
		        {
		            "key": "fileClass",
		            "parameterType": "STRING",
		            "parameterValue": "TEST"
		        }
		    ]
	    }
    ],
    "classOutputParameters": [
    	{
    		"productClass": "PTM_L1B_P1",
    		"outputParameters": [
    			{
    				"key": "revision",
    				"parameterType": "INTEGER",
    				"parameterValue": "2"
    			},
		        {
		            "key": "copernicusCollection",
		            "parameterType": "INTEGER",
		            "parameterValue": "77"
		        }
    		]
    	}
    ],
    "outputParameters": [
        {
            "key": "revision",
            "parameterType": "INTEGER",
            "parameterValue": "99"
        },
        {
            "key": "copernicusCollection",
            "parameterType": "INTEGER",
            "parameterValue": "77"
        }
    ],
    "configuredProcessors": [ "PTML2_1.1.0-SNAPSHOT_OPER_2020-03-25" ],
    "orbits": [
        { "spacecraftCode": "PTS", "orbitNumberFrom": 3000, "orbitNumberTo": 3002 }
    ],
    "requestedProductClasses": [ "PTM_L2_A", "PTM_L2_B" ],
    "inputProductClasses": [],
    "outputFileClass": "TEST",
    "processingMode": "OPER"
}
EOF

# Create a L3 processing order
echo "order create --file=$TEST_DATA_DIR/order_l3.json" >>$CLI_SCRIPT
cat >$TEST_DATA_DIR/order_l3.json <<EOF
{
    "missionCode": "PTM",
    "identifier": "L3_products_9:30-17:30",
    "orderState": "INITIAL",
    "startTime": "2019-11-04T09:30:00",
    "stopTime": "2019-11-04T17:00:00",
    "slicingType": "TIME_SLICE",
    "sliceDuration": 14400,
    "sliceOverlap": 0,
    "inputFilters": [
    	{
    		"productClass": "PTM_L2_A",
		    "filterConditions": [
		        {
		            "key": "revision",
		            "parameterType": "INTEGER",
		            "parameterValue": "99"
		        },
		        {
		            "key": "fileClass",
		            "parameterType": "STRING",
		            "parameterValue": "TEST"
		        }
		    ]
    	},
    	{
    		"productClass": "PTM_L2_B",
		    "filterConditions": [
		        {
		            "key": "revision",
		            "parameterType": "INTEGER",
		            "parameterValue": "99"
		        },
		        {
		            "key": "fileClass",
		            "parameterType": "STRING",
		            "parameterValue": "TEST"
		        }
		    ]
    	}
    ],
    "outputParameters": [
        {
            "key": "revision",
            "parameterType": "INTEGER",
            "parameterValue": "99"
        },
        {
            "key": "copernicusCollection",
            "parameterType": "INTEGER",
            "parameterValue": "77"
        }
    ],
    "configuredProcessors": [ "PTML3_1.1.0-SNAPSHOT_OPER_2020-03-25" ],
    "orbits": [],
    "requestedProductClasses": [ "PTM_L3" ],
    "inputProductClasses": [ "PTM_L2_A", "PTM_L2_B" ],
    "outputFileClass": "TEST",
    "processingMode": "OPER"
}
EOF

echo "Test data generation complete."
echo "Execute 'java -jar <path to CLI> < $CLI_SCRIPT' to load the data into prosEO."
