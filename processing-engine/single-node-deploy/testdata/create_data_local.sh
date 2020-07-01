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
sed "s|%SHARED_STORAGE_PATH%|${SHARED_STORAGE_PATH}|" <../nfs-pv.yaml.template >../nfs-pv.yaml
# Create the Persistent Volumes
kubectl apply -f ../nfs-pv.yaml

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
TEST_DATA_DIR=testfiles
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
1234567|L0________|2019-11-04T09:00:00Z|2019-11-04T09:45:00Z|2019-11-04T12:00:00Z|1
EOF

cat >PTM_L0_20191104094500_20191104103000_20191104120100.RAW <<EOF
1234567|L0________|2019-11-04T09:45:00Z|2019-11-04T10:30:00Z|2019-11-04T12:01:00Z|1
EOF

cat >PTM_L0_20191104103000_20191104111500_20191104120200.RAW <<EOF
1234567|L0________|2019-11-04T10:30:00Z|2019-11-04T11:15:00Z|2019-11-04T12:02:00Z|1
EOF

cat >PTM_L0_20191104111500_20191104120000_20191104120300.RAW <<EOF
1234567|L0________|2019-11-04T11:15:00Z|2019-11-04T12:00:00Z|2019-11-04T12:03:00Z|1
EOF

cat >PTM_L0_20191104120000_20191104124500_20191104150000.RAW <<EOF
1234567|L0________|2019-11-04T12:00:00Z|2019-11-04T12:45:00Z|2019-11-04T15:00:00Z|1
EOF

cat >PTM_L0_20191104124500_20191104133000_20191104150100.RAW <<EOF
1234567|L0________|2019-11-04T12:45:00Z|2019-11-04T13:30:00Z|2019-11-04T15:01:00Z|1
EOF

cat >PTM_L0_20191104133000_20191104141500_20191104150200.RAW <<EOF
1234567|L0________|2019-11-04T13:30:00Z|2019-11-04T14:15:00Z|2019-11-04T15:02:00Z|1
EOF

cat >PTM_L0_20191104141500_20191104150000_20191104150300.RAW <<EOF
1234567|L0________|2019-11-04T14:15:00Z|2019-11-04T15:00:00Z|2019-11-04T15:03:00Z|1
EOF

cat >PTM_L0_20191104150000_20191104154500_20191104180000.RAW <<EOF
1234567|L0________|2019-11-04T15:00:00Z|2019-11-04T15:45:00Z|2019-11-04T18:00:00Z|1
EOF

cat >PTM_L0_20191104154500_20191104163000_20191104180100.RAW <<EOF
1234567|L0________|2019-11-04T15:45:00Z|2019-11-04T16:30:00Z|2019-11-04T18:01:00Z|1
EOF

cat >PTM_L0_20191104163000_20191104171500_20191104180200.RAW <<EOF
1234567|L0________|2019-11-04T16:30:00Z|2019-11-04T17:15:00Z|2019-11-04T18:02:00Z|1
EOF

cat >PTM_L0_20191104171500_20191104180000_20191104180300.RAW <<EOF
1234567|L0________|2019-11-04T17:15:00Z|2019-11-04T18:00:00Z|2019-11-04T18:03:00Z|1
EOF

cat >PTM_L0_20191104180000_20191104184500_20191104210000.RAW <<EOF
1234567|L0________|2019-11-04T18:00:00Z|2019-11-04T18:45:00Z|2019-11-04T21:00:00Z|1
EOF

cat >PTM_L0_20191104184500_20191104193000_20191104210100.RAW <<EOF
1234567|L0________|2019-11-04T18:45:00Z|2019-11-04T19:30:00Z|2019-11-04T21:01:00Z|1
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
kubectl apply -f ../storage-mgr-local.yaml


# -------------------------
# Create Kubernetes Dashboard
# -------------------------

# Create a dashboard at http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
kubectl apply -f ../kubernetes-dashboard.yaml
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
    "processingEngineUrl": "http://host.docker.internal:8001/",
    "processingEngineUser": "kubeuser1",
    "processingEnginePassword": "very-secret-password",
    "storageManagerUrl": 
    	"http://host.docker.internal:8001/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1",
    "localStorageManagerUrl": "http://storage-mgr-service.default.svc.cluster.local:3000/proseo/storage-mgr/v0.1",
    "storageManagerUser": "smuser",
    "storageManagerPassword": "smpwd-but-that-would-be-way-too-short",
    "defaultStorageType": "POSIX"
}
EOF
echo "facility create --file=$TEST_DATA_DIR/facility.json" >>$CLI_SCRIPT

# Ingest test data into prosEO
echo "ingest --file=$TEST_DATA_DIR/ingest_products.json localhost" >>$CLI_SCRIPT
cat >$TEST_DATA_DIR/ingest_products.json <<EOF
[
    {
        "missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "50545d8ad8486dd9297014cf2e769f71",
    	"checksumTime": "2019-11-04T12:00:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "3ca720d0d2a7dee0ba95a9b50047b5a0",
    	"checksumTime": "2019-11-04T12:01:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "f65f2591c400a85ced1d7557a6644138",
    	"checksumTime": "2019-11-04T12:02:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "58a029e6b09ad1e749fc1ff523dbbf80",
    	"checksumTime": "2019-11-04T12:03:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "9ecc9bbc7abb0672efd51a39e8fc2c59",
    	"checksumTime": "2019-11-04T15:00:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "fb720888a0d9bae6b16c1f9607c4de27",
    	"checksumTime": "2019-11-04T15:01:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "4ef20d31f2e051d16cf06db2bae2c76e",
    	"checksumTime": "2019-11-04T15:02:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "8b3a2f2f386c683ce9b1c68bb52b34d2",
    	"checksumTime": "2019-11-04T15:03:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "6bea82661ad200b8dc8a912bbc9c89f6",
    	"checksumTime": "2019-11-04T18:00:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "a1953fa0c117f803c26902243bb0d3aa",
    	"checksumTime": "2019-11-04T18:01:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "a9fb7fc052b5aced6f6dc9d753bbe790",
    	"checksumTime": "2019-11-04T18:02:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "b8b5304d83100a56114cd5ca6a6bc581",
    	"checksumTime": "2019-11-04T18:03:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "9211729e47c5e2487de302f1ca48f4b9",
    	"checksumTime": "2019-11-04T21:00:10.000000"
    },
    {
		"missionCode": "PTM",
        "productClass": "L0________",
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
    	"fileSize": 84,
    	"checksum": "95eb634992099c7ebb7bf5b76b243da1",
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
    		"productClass": "L0________",
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
    		"productClass": "L1B_PART1",
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
    		"productClass": "L1B_PART1",
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
    "configuredProcessors": [ "PTML2_0.1.0_OPER_2020-03-25" ],
    "orbits": [
        { "spacecraftCode": "PTS", "orbitNumberFrom": 3000, "orbitNumberTo": 3002 }
    ],
    "requestedProductClasses": [ "PTM_L2A", "PTM_L2B" ],
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
    		"productClass": "PTM_L2A",
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
    		"productClass": "PTM_L2B",
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
    "configuredProcessors": [ "PTML3_0.1.0_OPER_2020-03-25" ],
    "orbits": [],
    "requestedProductClasses": [ "PTM_L3" ],
    "inputProductClasses": [ "PTM_L2A", "PTM_L2B" ],
    "outputFileClass": "TEST",
    "processingMode": "OPER"
}
EOF

echo "Test data generation complete."
echo "Execute 'java -jar <path to CLI> < $CLI_SCRIPT' to load the data into prosEO."
