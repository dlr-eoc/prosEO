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

# Create a new CLI command script
CLI_SCRIPT=cli_data_script.txt
echo "" >$CLI_SCRIPT
 
# Create empty subdirectory for test data
TEST_DATA_DIR=testfiles
mkdir -p $TEST_DATA_DIR
cd $TEST_DATA_DIR
rm *

# Create L0 input data
# Products consist of the fields id, type, start time, stop time, generation time and revision,
# separated by vertical bars
# L0 products are in 45 min slices, starting with orbit 3000
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
cp -p bulletinb-380.xml $TEST_DATA_DIR

# Upload test data to S3 storage on test cluster
# Requires "aws" (aws-cli) as a local S3 client, correctly configured with access key id and secret
# aws s3 sync $TEST_DATA_DIR s3://<bucket> --endpoint-url https://<provider-url>

# Create a processing facility
# echo "facility create <facility-name> description=<facility-description> defaultStorageType=S3 processingEngineUrl=https://<facility-url>/ storageManagerUrl=https://<facility-url>/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1/" >>$CLI_SCRIPT
echo "facility create localhost description=Local_Facility defaultStorageType=POSIX processingEngineUrl=https://localhost/ storageManagerUrl=https://localhost/api/v1/namespaces/default/services/storage-mgr-service:service/proxy/proseo/storage-mgr/v1/" >>$CLI_SCRIPT

# Ingest test data into prosEO
echo "ingest --file=$TEST_DATA_DIR/ingest_products.json telekom-otc" >>$CLI_SCRIPT
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104090000_20191104094500_20191104120000.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104094500_20191104103000_20191104120100.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104103000_20191104111500_20191104120200.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104111500_20191104120000_20191104120300.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104120000_20191104124500_20191104150000.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104124500_20191104133000_20191104150100.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104133000_20191104141500_20191104150200.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104141500_20191104150000_20191104150300.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104150000_20191104154500_20191104180000.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104154500_20191104163000_20191104180100.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104163000_20191104171500_20191104180200.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104171500_20191104180000_20191104180300.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "PTM_L0_20191104180000_20191104184500_20191104210000.RAW",
        "auxFileNames": []
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
        "sourceStorageType": "S3",
        "mountPoint": "s3://proseo-s5p-main",
        "filePath": "integration-test/testdata",
        "productFileName": "bulletinb-380.xml",
        "auxFileNames": []
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
    "propagateSlicing": false,
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