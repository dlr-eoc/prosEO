docker run --rm \
-e JOBORDER_FS_TYPE="S3" \
-e JOBORDER_FILE="" \
-e S3_ENDPOINT="http://localhost:9000" \
-e S3_ACCESS_KEY="short_access_key" \
-e S3_SECRET_ACCESS_KEY="short_secret_key" \
-e S3_STORAGE_ID_OUTPUTS="s3test" \
-e ALLUXIO_STORAGE_ID_OUTPUTS="alluxio1" \
-e INGESTOR_ENDPOINT="http://localhost:8082" \
-e STATE_CALLBACK_ENDPOINT="http://localhost:8080/proseo/planner/v0.1/processingfacilities/Lerchenhof/finish/proseojob968" \
-e PROCESSING_FACILITY_NAME="Lerchenhof" \
-v /tmp:/mnt/ramdisk \
--net=host \
-h localhost \
localhost:5000/proseo-sample-integration-processor:0.0.1-SNAPSHOT