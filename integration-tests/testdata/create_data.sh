#!/bin/bash
#
# create_data.sh
# --------------
#
# Create dynamic test data for the prosEO test mission:
# - L0 input data
# - IERSB AUX input data
# - a processing facility on a Telekom OTC cluster
# - a processing order for L2 products
# - a processing order for L3 products
#

# Subdirectory for test data
TEST_DATA_DIR=testfiles

# Create L0 input data
# Products consist of the fields id, type, start time, stop time, generation time and revision,
# separated by vertical bars
# L0 products are in 45 min slices, starting with orbit 3000
mkdir -p $TEST_DATA_DIR
cd $TEST_DATA_DIR

cat >PTM_L0_20191104090000_20191104094500_20191104120000.RAW <<EOF
1234567|L0|2019-11-04T09:00:00Z|2019-11-04T09:45:00Z|2019-11-04T12:00:00Z|1
EOF

cat >PTM_L0_20191104094500_20191104103000_20191104120100.RAW <<EOF
1234567|L0|2019-11-04T09:45:00Z|2019-11-04T10:30:00Z|2019-11-04T12:01:00Z|1
EOF

cat >PTM_L0_20191104103000_20191104111500_20191104120200.RAW <<EOF
1234567|L0|2019-11-04T10:30:00Z|2019-11-04T11:15:00Z|2019-11-04T12:02:00Z|1
EOF

cat >PTM_L0_20191104111500_20191104120000_20191104120300.RAW <<EOF
1234567|L0|2019-11-04T11:15:00Z|2019-11-04T12:00:00Z|2019-11-04T12:03:00Z|1
EOF

cat >PTM_L0_20191104120000_20191104124500_20191104150000.RAW <<EOF
1234567|L0|2019-11-04T12:00:00Z|2019-11-04T12:45:00Z|2019-11-04T15:00:00Z|1
EOF

cat >PTM_L0_20191104124500_20191104133000_20191104150100.RAW <<EOF
1234567|L0|2019-11-04T12:45:00Z|2019-11-04T13:30:00Z|2019-11-04T15:01:00Z|1
EOF

cat >PTM_L0_20191104133000_20191104141500_20191104150200.RAW <<EOF
1234567|L0|2019-11-04T13:30:00Z|2019-11-04T14:15:00Z|2019-11-04T15:02:00Z|1
EOF

cat >PTM_L0_20191104141500_20191104150000_20191104150300.RAW <<EOF
1234567|L0|2019-11-04T14:15:00Z|2019-11-04T15:00:00Z|2019-11-04T15:03:00Z|1
EOF

cat >PTM_L0_20191104150000_20191104154500_20191104180000.RAW <<EOF
1234567|L0|2019-11-04T15:00:00Z|2019-11-04T15:45:00Z|2019-11-04T18:00:00Z|1
EOF

cat >PTM_L0_20191104154500_20191104163000_20191104180100.RAW <<EOF
1234567|L0|2019-11-04T15:45:00Z|2019-11-04T16:30:00Z|2019-11-04T18:01:00Z|1
EOF

cat >PTM_L0_20191104163000_20191104171500_20191104180200.RAW <<EOF
1234567|L0|2019-11-04T16:30:00Z|2019-11-04T17:15:00Z|2019-11-04T18:02:00Z|1
EOF

cat >PTM_L0_20191104171500_20191104180000_20191104180300.RAW <<EOF
1234567|L0|2019-11-04T17:15:00Z|2019-11-04T18:00:00Z|2019-11-04T18:03:00Z|1
EOF

cat >PTM_L0_20191104180000_20191104184500_20191104210000.RAW <<EOF
1234567|L0|2019-11-04T18:00:00Z|2019-11-04T18:45:00Z|2019-11-04T21:00:00Z|1
EOF

cat >PTM_L0_20191104184500_20191104193000_20191104210100.RAW <<EOF
1234567|L0|2019-11-04T18:45:00Z|2019-11-04T19:30:00Z|2019-11-04T21:01:00Z|1
EOF

cd -

# IERSB AUX products are real-world data:
# Using bulletinb-380.xml
cp -p bulletinb-380.xml $TEST_DATA_DIR

# Upload test data to S3 storage on test cluster
# Requires "aws" (aws-cli) as a local S3 client, correctly configured with access key id and secret
aws s3 sync $TEST_DATA_DIR s3://proseo-s5p-main/integration-test/testdata/ --endpoint-url https://obs.eu-de.otc.t-systems.com

# Create a processing facility
psql proseo -U postgres <<EOF
INSERT INTO processing_facility VALUES ( (SELECT nextval('hibernate_sequence')), 1, 'Telekom OTC', 'telekom-otc', 'https://proseo-k8s-gate.de/', 'https://proseo-k8s-gate.de/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1/' );
\q
EOF

# Ingest test data into prosEO
curl --insecure --data @- --header "Content-Type: application/json" --user s5p-proseo:sieb37.Schlaefer http://localhost:8081/proseo/ingestor/v0.1/ingest/telekom-otc <<EOF
[
    {
        "missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104090000_20191104094500_20191104120000.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104094500_20191104103000_20191104120100.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104103000_20191104111500_20191104120200.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104111500_20191104120000_20191104120300.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104120000_20191104124500_20191104150000.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104124500_20191104133000_20191104150100.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104133000_20191104141500_20191104150200.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104141500_20191104150000_20191104150300.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104150000_20191104154500_20191104180000.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104154500_20191104163000_20191104180100.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104163000_20191104171500_20191104180200.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104171500_20191104180000_20191104180300.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104180000_20191104184500_20191104210000.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "L0",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104184500_20191104193000_20191104210100.RAW",
        "auxFileNames": []
    },
    {
		"missionCode": "PTM",
        "productClass": "AUX_IERS_B",
        "fileClass": "OPER",
        "mode": "OPER",
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "bulletinb-380.xml",
        "auxFileNames": []
    }
]
EOF

# Create a L2 processing order
curl --insecure --data @- --header "Content-Type: application/json" --user s5p-proseo:sieb37.Schlaefer http://localhost:8082/proseo/order-mgr/v0.1/orders <<EOF
{
    "missionCode": "PTM",
    "identifier": "L2 orbits 3000-3002",
    "orderState": "INITIAL",
    "slicingType": "ORBIT",
    "sliceOverlap": 0,
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
    "configuredProcessors": [ "PTML2 0.0.1 OPER 2019-11-04" ],
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
curl --insecure --data @- --header "Content-Type: application/json" --user s5p-proseo:sieb37.Schlaefer http://localhost:8082/proseo/order-mgr/v0.1/orders <<EOF
{
    "missionCode": "PTM",
    "identifier": "L3 products 9:30-17:30",
    "orderState": "INITIAL",
    "startTime": "2019-11-04T09:30:00",
    "stopTime": "2019-11-04T17:00:00",
    "slicingType": "TIME_SLICE",
    "sliceOverlap": 0,
    "sliceDuration": 14400,
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
    "configuredProcessors": [ "PTML3 0.0.1 OPER 2019-11-04" ],
    "orbits": [],
    "requestedProductClasses": [ "PTM_L3" ],
    "inputProductClasses": [],
    "outputFileClass": "TEST",
    "processingMode": "OPER"
}
EOF
