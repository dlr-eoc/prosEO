prosEO sample wrapper & processor integration
=============================================

## prerequisites
- docker-compose stack from `prosEO/docker-compose.yml` is up

## create storages for ALLUXIO & S3
- POST http://localhost:8083/proseo/storage-mgr/v1/devel/storages
```js
{
    "id": "alluxio1",
    "storageType" : "ALLUXIO",
    "description": "prosEO-FS for L1B products"
}
```
- POST http://localhost:8083/proseo/storage-mgr/v1/devel/storages
```js
{
    "id": "s3test",
    "storageType" : "S3",
    "description": "prosEO-FS for L1B products"
}
```

## upload test-data
- install awscli (if not installed)
  - pip install awscli
  - aws configure
  - ...enter access-keys/secrets/...
- Upload all alluxio-test-files
  - cd ../base-wrapper/src/test/resources
  - aws s3 sync alluxioTestData/ s3://internal/underfs/alluxio1/3244232/ --endpoint-url http://localhost:9000
- Upload all s3-testdata
  - cd ../base-wrapper/src/test/resources
  - aws s3 sync s3TestData/ s3://s3test/3244233/ --endpoint-url http://localhost:9000

## build wrapper-integration docker-image
- cd prosEO
- mvn clean package –DskipTests (or mvn package -pl samples/sample-wrapper -am -DskipTests)
- cd samples
- ./build-integration-proc.sh

## base64 encode Sample-JOF
- cd prosEO/samples/sample-wrapper/src/test/resources
- sample JOF is: samples/sample-wrapper/src/test/resources/TEST_JOF.xml
- create base64-string of this file, or use sample-POST of next step

## Upload JOF using storage-mgr
- POST http://localhost:8083/proseo/storage-mgr/v1/devel/joborders
```js
{
    "jobOrderStringBase64": "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/Pgo8SXBmX0pvYl9PcmRlcj4KICA8SXBmX0NvbmY+CiAgICA8UHJvY2Vzc29yX05hbWU+Q09fX19fPC9Qcm9jZXNzb3JfTmFtZT4KICAgIDxWZXJzaW9uPjAxLjAzLjAxPC9WZXJzaW9uPgogICAgPFN0ZG91dF9Mb2dfTGV2ZWw+SU5GTzwvU3Rkb3V0X0xvZ19MZXZlbD4KICAgIDxTdGRlcnJfTG9nX0xldmVsPldBUk5JTkc8L1N0ZGVycl9Mb2dfTGV2ZWw+CiAgICA8VGVzdD5mYWxzZTwvVGVzdD4KICAgIDxCcmVha3BvaW50X0VuYWJsZT50cnVlPC9CcmVha3BvaW50X0VuYWJsZT4KICAgIDxQcm9jZXNzaW5nX1N0YXRpb24+UERHUy1PUDwvUHJvY2Vzc2luZ19TdGF0aW9uPgogICAgPEFjcXVpc2l0aW9uX1N0YXRpb24+UERHUy1HU048L0FjcXVpc2l0aW9uX1N0YXRpb24+CiAgICA8U2Vuc2luZ19UaW1lPgogICAgICA8U3RhcnQ+MjAxODAxMDVfMDc1MzA3MDAwMDAwPC9TdGFydD4KICAgICAgPFN0b3A+MjAxODAxMDVfMDkzNDM3MDAwMDAwPC9TdG9wPgogICAgPC9TZW5zaW5nX1RpbWU+CiAgICA8Q29uZmlnX0ZpbGVzLz4KICAgIDxEeW5hbWljX1Byb2Nlc3NpbmdfUGFyYW1ldGVycz4KICAgICAgPFByb2Nlc3NpbmdfUGFyYW1ldGVyPgogICAgICAgIDxOYW1lPmxvZ2dpbmcucm9vdDwvTmFtZT4KICAgICAgICA8VmFsdWU+bm90aWNlPC9WYWx1ZT4KICAgICAgPC9Qcm9jZXNzaW5nX1BhcmFtZXRlcj4KICAgICAgPFByb2Nlc3NpbmdfUGFyYW1ldGVyPgogICAgICAgIDxOYW1lPmxvZ2dpbmcuZHVtcGxvZzwvTmFtZT4KICAgICAgICA8VmFsdWU+bnVsbDwvVmFsdWU+CiAgICAgIDwvUHJvY2Vzc2luZ19QYXJhbWV0ZXI+CiAgICAgIDxQcm9jZXNzaW5nX1BhcmFtZXRlcj4KICAgICAgICA8TmFtZT5UaHJlYWRzPC9OYW1lPgogICAgICAgIDxWYWx1ZT45PC9WYWx1ZT4KICAgICAgPC9Qcm9jZXNzaW5nX1BhcmFtZXRlcj4KICAgICAgPFByb2Nlc3NpbmdfUGFyYW1ldGVyPgogICAgICAgIDxOYW1lPm9yYml0IG51bWJlcjwvTmFtZT4KICAgICAgICA8VmFsdWU+MDExOTE8L1ZhbHVlPgogICAgICA8L1Byb2Nlc3NpbmdfUGFyYW1ldGVyPgogICAgICA8UHJvY2Vzc2luZ19QYXJhbWV0ZXI+CiAgICAgICAgPE5hbWU+UHJvY2Vzc2luZ19Nb2RlPC9OYW1lPgogICAgICAgIDxWYWx1ZT5PRkZMPC9WYWx1ZT4KICAgICAgPC9Qcm9jZXNzaW5nX1BhcmFtZXRlcj4KICAgICAgPFByb2Nlc3NpbmdfUGFyYW1ldGVyPgogICAgICAgIDxOYW1lPkRlYWRsaW5lX1RpbWU8L05hbWU+CiAgICAgICAgPFZhbHVlPjIwMTkwNDI1XzE2MzUzMDAwMDAwMDwvVmFsdWU+CiAgICAgIDwvUHJvY2Vzc2luZ19QYXJhbWV0ZXI+CiAgICA8L0R5bmFtaWNfUHJvY2Vzc2luZ19QYXJhbWV0ZXJzPgogIDwvSXBmX0NvbmY+CiAgPExpc3Rfb2ZfSXBmX1Byb2NzIGNvdW50PSIxIj4KICAgIDxJcGZfUHJvYz4KICAgICAgPFRhc2tfTmFtZT5UUk9QTkxMMkRQPC9UYXNrX05hbWU+CiAgICAgIDxUYXNrX1ZlcnNpb24+MDEuMDMuMDE8L1Rhc2tfVmVyc2lvbj4KICAgICAgPExpc3Rfb2ZfSW5wdXRzIGNvdW50PSIxOCI+CiAgICAgICAgPElucHV0PgogICAgICAgICAgPEZpbGVfVHlwZT5DRkdfQ09fX19fPC9GaWxlX1R5cGU+CiAgICAgICAgICA8RmlsZV9OYW1lX1R5cGU+UEhZU0lDQUw8L0ZpbGVfTmFtZV9UeXBlPgogICAgICAgICAgPExpc3Rfb2ZfRmlsZV9OYW1lcyBjb3VudD0iMSI+CiAgICAgICAgICAgIDxGaWxlX05hbWUgRlNfVFlQRT0iQUxMVVhJTyI+L2FsbHV4aW8xLzMyNDQyMzIvZmlsZTAxLnR4dDwvRmlsZV9OYW1lPgogICAgICAgICAgPC9MaXN0X29mX0ZpbGVfTmFtZXM+CiAgICAgICAgPC9JbnB1dD4KICAgICAgICA8SW5wdXQ+CiAgICAgICAgICA8RmlsZV9UeXBlPkNGR19DT19fX0Y8L0ZpbGVfVHlwZT4KICAgICAgICAgIDxGaWxlX05hbWVfVHlwZT5QSFlTSUNBTDwvRmlsZV9OYW1lX1R5cGU+CiAgICAgICAgICA8TGlzdF9vZl9GaWxlX05hbWVzIGNvdW50PSIxIj4KICAgICAgICAgICAgPEZpbGVfTmFtZSBGU19UWVBFPSJBTExVWElPIj4vYWxsdXhpbzEvMzI0NDIzMi9maWxlMDIudHh0PC9GaWxlX05hbWU+CiAgICAgICAgICA8L0xpc3Rfb2ZfRmlsZV9OYW1lcz4KICAgICAgICA8L0lucHV0PgogICAgICAgIDxJbnB1dD4KICAgICAgICAgIDxGaWxlX1R5cGU+UkVGX1NPTEFSXzwvRmlsZV9UeXBlPgogICAgICAgICAgPEZpbGVfTmFtZV9UeXBlPlBIWVNJQ0FMPC9GaWxlX05hbWVfVHlwZT4KICAgICAgICAgIDxMaXN0X29mX0ZpbGVfTmFtZXMgY291bnQ9IjEiPgogICAgICAgICAgICA8RmlsZV9OYW1lIEZTX1RZUEU9IlMzIj5zMzovL3MzdGVzdC8zMjQ0MjMzL2ZpbGUwMy50eHQ8L0ZpbGVfTmFtZT4KICAgICAgICAgIDwvTGlzdF9vZl9GaWxlX05hbWVzPgogICAgICAgIDwvSW5wdXQ+CiAgICAgICAgPElucHV0PgogICAgICAgICAgPEZpbGVfVHlwZT5SRUZfWFNfX0NPPC9GaWxlX1R5cGU+CiAgICAgICAgICA8RmlsZV9OYW1lX1R5cGU+UEhZU0lDQUw8L0ZpbGVfTmFtZV9UeXBlPgogICAgICAgICAgPExpc3Rfb2ZfRmlsZV9OYW1lcyBjb3VudD0iMSI+CiAgICAgICAgICAgIDxGaWxlX05hbWUgRlNfVFlQRT0iUzMiPnMzOi8vczN0ZXN0LzMyNDQyMzMvZmlsZTA0LnR4dDwvRmlsZV9OYW1lPgogICAgICAgICAgPC9MaXN0X29mX0ZpbGVfTmFtZXM+CiAgICAgICAgPC9JbnB1dD4KICAgICAgICA8SW5wdXQ+CiAgICAgICAgICA8RmlsZV9UeXBlPkFVWF9JU1JGX188L0ZpbGVfVHlwZT4KICAgICAgICAgIDxGaWxlX05hbWVfVHlwZT5QSFlTSUNBTDwvRmlsZV9OYW1lX1R5cGU+CiAgICAgICAgICA8TGlzdF9vZl9GaWxlX05hbWVzIGNvdW50PSIxIj4KICAgICAgICAgICAgPEZpbGVfTmFtZSBGU19UWVBFPSJTMyI+czM6Ly9zM3Rlc3QvMzI0NDIzMy9maWxlMDUudHh0PC9GaWxlX05hbWU+CiAgICAgICAgICA8L0xpc3Rfb2ZfRmlsZV9OYW1lcz4KICAgICAgICA8L0lucHV0PgogICAgICAgIDxJbnB1dD4KICAgICAgICAgIDxGaWxlX1R5cGU+QVVYX0NUTUNINDwvRmlsZV9UeXBlPgogICAgICAgICAgPEZpbGVfTmFtZV9UeXBlPlBIWVNJQ0FMPC9GaWxlX05hbWVfVHlwZT4KICAgICAgICAgIDxMaXN0X29mX0ZpbGVfTmFtZXMgY291bnQ9IjMiPgogICAgICAgICAgICA8RmlsZV9OYW1lIEZTX1RZUEU9IlMzIj5zMzovL3MzdGVzdC8zMjQ0MjMzL2ZpbGUwNi50eHQ8L0ZpbGVfTmFtZT4KICAgICAgICAgICAgPEZpbGVfTmFtZSBGU19UWVBFPSJBTExVWElPIj4vYWxsdXhpbzEvMzI0NDIzMi9maWxlMDcudHh0PC9GaWxlX05hbWU+CiAgICAgICAgICAgIDxGaWxlX05hbWUgRlNfVFlQRT0iQUxMVVhJTyI+L2FsbHV4aW8xLzMyNDQyMzIvZmlsZTA4LnR4dDwvRmlsZV9OYW1lPgogICAgICAgICAgPC9MaXN0X29mX0ZpbGVfTmFtZXM+CiAgICAgICAgPC9JbnB1dD4KICAgICAgICA8SW5wdXQ+CiAgICAgICAgICA8RmlsZV9UeXBlPkwxQl9JUl9TSVI8L0ZpbGVfVHlwZT4KICAgICAgICAgIDxGaWxlX05hbWVfVHlwZT5QSFlTSUNBTDwvRmlsZV9OYW1lX1R5cGU+CiAgICAgICAgICA8TGlzdF9vZl9GaWxlX05hbWVzIGNvdW50PSIxIj4KICAgICAgICAgICAgPEZpbGVfTmFtZSBGU19UWVBFPSJTMyI+czM6Ly9zM3Rlc3QvMzI0NDIzMy9maWxlMDkudHh0PC9GaWxlX05hbWU+CiAgICAgICAgICA8L0xpc3Rfb2ZfRmlsZV9OYW1lcz4KICAgICAgICA8L0lucHV0PgogICAgICAgIDxJbnB1dD4KICAgICAgICAgIDxGaWxlX1R5cGU+TDFCX1JBX0JEMTwvRmlsZV9UeXBlPgogICAgICAgICAgPEZpbGVfTmFtZV9UeXBlPlBIWVNJQ0FMPC9GaWxlX05hbWVfVHlwZT4KICAgICAgICAgIDxMaXN0X29mX0ZpbGVfTmFtZXMgY291bnQ9IjEiPgogICAgICAgICAgICA8RmlsZV9OYW1lIEZTX1RZUEU9IlMzIj5zMzovL3MzdGVzdC8zMjQ0MjMzL2ZpbGUxMC50eHQ8L0ZpbGVfTmFtZT4KICAgICAgICAgIDwvTGlzdF9vZl9GaWxlX05hbWVzPgogICAgICAgIDwvSW5wdXQ+CiAgICAgICAgPElucHV0PgogICAgICAgICAgPEZpbGVfVHlwZT5MMUJfUkFfQkQyPC9GaWxlX1R5cGU+CiAgICAgICAgICA8RmlsZV9OYW1lX1R5cGU+UEhZU0lDQUw8L0ZpbGVfTmFtZV9UeXBlPgogICAgICAgICAgPExpc3Rfb2ZfRmlsZV9OYW1lcyBjb3VudD0iMSI+CiAgICAgICAgICAgIDxGaWxlX05hbWUgRlNfVFlQRT0iUzMiPnMzOi8vczN0ZXN0LzMyNDQyMzMvZmlsZTExLnR4dDwvRmlsZV9OYW1lPgogICAgICAgICAgPC9MaXN0X29mX0ZpbGVfTmFtZXM+CiAgICAgICAgPC9JbnB1dD4KICAgICAgICA8SW5wdXQ+CiAgICAgICAgICA8RmlsZV9UeXBlPkwxQl9SQV9CRDM8L0ZpbGVfVHlwZT4KICAgICAgICAgIDxGaWxlX05hbWVfVHlwZT5QSFlTSUNBTDwvRmlsZV9OYW1lX1R5cGU+CiAgICAgICAgICA8TGlzdF9vZl9GaWxlX05hbWVzIGNvdW50PSIxIj4KICAgICAgICAgICAgPEZpbGVfTmFtZSBGU19UWVBFPSJTMyI+czM6Ly9zM3Rlc3QvMzI0NDIzMy9maWxlMTIudHh0PC9GaWxlX05hbWU+CiAgICAgICAgICA8L0xpc3Rfb2ZfRmlsZV9OYW1lcz4KICAgICAgICA8L0lucHV0PgogICAgICAgIDxJbnB1dD4KICAgICAgICAgIDxGaWxlX1R5cGU+TDFCX1JBX0JENDwvRmlsZV9UeXBlPgogICAgICAgICAgPEZpbGVfTmFtZV9UeXBlPlBIWVNJQ0FMPC9GaWxlX05hbWVfVHlwZT4KICAgICAgICAgIDxMaXN0X29mX0ZpbGVfTmFtZXMgY291bnQ9IjEiPgogICAgICAgICAgICA8RmlsZV9OYW1lIEZTX1RZUEU9IlMzIj5zMzovL3MzdGVzdC8zMjQ0MjMzL2ZpbGUxMy50eHQ8L0ZpbGVfTmFtZT4KICAgICAgICAgIDwvTGlzdF9vZl9GaWxlX05hbWVzPgogICAgICAgIDwvSW5wdXQ+CiAgICAgICAgPElucHV0PgogICAgICAgICAgPEZpbGVfVHlwZT5MMUJfUkFfQkQ1PC9GaWxlX1R5cGU+CiAgICAgICAgICA8RmlsZV9OYW1lX1R5cGU+UEhZU0lDQUw8L0ZpbGVfTmFtZV9UeXBlPgogICAgICAgICAgPExpc3Rfb2ZfRmlsZV9OYW1lcyBjb3VudD0iMSI+CiAgICAgICAgICAgIDxGaWxlX05hbWUgRlNfVFlQRT0iUzMiPnMzOi8vczN0ZXN0LzMyNDQyMzMvZmlsZTE0LnR4dDwvRmlsZV9OYW1lPgogICAgICAgICAgPC9MaXN0X29mX0ZpbGVfTmFtZXM+CiAgICAgICAgPC9JbnB1dD4KICAgICAgICA8SW5wdXQ+CiAgICAgICAgICA8RmlsZV9UeXBlPkwxQl9SQV9CRDY8L0ZpbGVfVHlwZT4KICAgICAgICAgIDxGaWxlX05hbWVfVHlwZT5QSFlTSUNBTDwvRmlsZV9OYW1lX1R5cGU+CiAgICAgICAgICA8TGlzdF9vZl9GaWxlX05hbWVzIGNvdW50PSIxIj4KICAgICAgICAgICAgPEZpbGVfTmFtZSBGU19UWVBFPSJTMyI+czM6Ly9zM3Rlc3QvMzI0NDIzMy9maWxlMTUudHh0PC9GaWxlX05hbWU+CiAgICAgICAgICA8L0xpc3Rfb2ZfRmlsZV9OYW1lcz4KICAgICAgICA8L0lucHV0PgogICAgICAgIDxJbnB1dD4KICAgICAgICAgIDxGaWxlX1R5cGU+TDFCX1JBX0JENzwvRmlsZV9UeXBlPgogICAgICAgICAgPEZpbGVfTmFtZV9UeXBlPlBIWVNJQ0FMPC9GaWxlX05hbWVfVHlwZT4KICAgICAgICAgIDxMaXN0X29mX0ZpbGVfTmFtZXMgY291bnQ9IjEiPgogICAgICAgICAgICA8RmlsZV9OYW1lIEZTX1RZUEU9IlMzIj5zMzovL3MzdGVzdC8zMjQ0MjMzL2ZpbGUxNi50eHQ8L0ZpbGVfTmFtZT4KICAgICAgICAgIDwvTGlzdF9vZl9GaWxlX05hbWVzPgogICAgICAgIDwvSW5wdXQ+CiAgICAgICAgPElucHV0PgogICAgICAgICAgPEZpbGVfVHlwZT5MMUJfUkFfQkQ4PC9GaWxlX1R5cGU+CiAgICAgICAgICA8RmlsZV9OYW1lX1R5cGU+UEhZU0lDQUw8L0ZpbGVfTmFtZV9UeXBlPgogICAgICAgICAgPExpc3Rfb2ZfRmlsZV9OYW1lcyBjb3VudD0iMSI+CiAgICAgICAgICAgIDxGaWxlX05hbWUgRlNfVFlQRT0iUzMiPnMzOi8vczN0ZXN0LzMyNDQyMzMvZmlsZTE2LnR4dDwvRmlsZV9OYW1lPgogICAgICAgICAgPC9MaXN0X29mX0ZpbGVfTmFtZXM+CiAgICAgICAgPC9JbnB1dD4KICAgICAgICA8SW5wdXQ+CiAgICAgICAgICA8RmlsZV9UeXBlPkFVWF9NRVRfVFA8L0ZpbGVfVHlwZT4KICAgICAgICAgIDxGaWxlX05hbWVfVHlwZT5QSFlTSUNBTDwvRmlsZV9OYW1lX1R5cGU+CiAgICAgICAgICA8TGlzdF9vZl9GaWxlX05hbWVzIGNvdW50PSIxIj4KICAgICAgICAgICAgPEZpbGVfTmFtZSBGU19UWVBFPSJTMyI+czM6Ly9zM3Rlc3QvMzI0NDIzMy9maWxlMTYudHh0PC9GaWxlX05hbWU+CiAgICAgICAgICA8L0xpc3Rfb2ZfRmlsZV9OYW1lcz4KICAgICAgICA8L0lucHV0PgogICAgICAgIDxJbnB1dD4KICAgICAgICAgIDxGaWxlX1R5cGU+QVVYX01FVF8yRDwvRmlsZV9UeXBlPgogICAgICAgICAgPEZpbGVfTmFtZV9UeXBlPlBIWVNJQ0FMPC9GaWxlX05hbWVfVHlwZT4KICAgICAgICAgIDxMaXN0X29mX0ZpbGVfTmFtZXMgY291bnQ9IjEiPgogICAgICAgICAgICA8RmlsZV9OYW1lIEZTX1RZUEU9IlMzIj5zMzovL3MzdGVzdC8zMjQ0MjMzL2ZpbGUxNy50eHQ8L0ZpbGVfTmFtZT4KICAgICAgICAgIDwvTGlzdF9vZl9GaWxlX05hbWVzPgogICAgICAgIDwvSW5wdXQ+CiAgICAgICAgPElucHV0PgogICAgICAgICAgPEZpbGVfVHlwZT5BVVhfTUVUX1FQPC9GaWxlX1R5cGU+CiAgICAgICAgICA8RmlsZV9OYW1lX1R5cGU+UEhZU0lDQUw8L0ZpbGVfTmFtZV9UeXBlPgogICAgICAgICAgPExpc3Rfb2ZfRmlsZV9OYW1lcyBjb3VudD0iMSI+CiAgICAgICAgICAgIDxGaWxlX05hbWUgRlNfVFlQRT0iUzMiPnMzOi8vczN0ZXN0LzMyNDQyMzMvZmlsZTE4LnR4dDwvRmlsZV9OYW1lPgogICAgICAgICAgPC9MaXN0X29mX0ZpbGVfTmFtZXM+CiAgICAgICAgPC9JbnB1dD4KICAgICAgPC9MaXN0X29mX0lucHV0cz4KICAgICAgPExpc3Rfb2ZfT3V0cHV0cyBjb3VudD0iMyI+CiAgICAgICAgPE91dHB1dCBQcm9kdWN0X0lEPSI3Mzk3MTI5ODMxIj4KICAgICAgICAgIDxGaWxlX1R5cGU+TDJfX0NPX19fXzwvRmlsZV9UeXBlPgogICAgICAgICAgPEZpbGVfTmFtZV9UeXBlPlBIWVNJQ0FMPC9GaWxlX05hbWVfVHlwZT4KICAgICAgICAgIDxMaXN0X29mX0ZpbGVfTmFtZXMgY291bnQ9IjEiPgogICAgICAgICAgICA8RmlsZV9OYW1lIEZTX1RZUEU9IkFMTFVYSU8iPnJlc3VsdHNfQS9yZXN1bHQwMS50eHQ8L0ZpbGVfTmFtZT4KICAgICAgICAgIDwvTGlzdF9vZl9GaWxlX05hbWVzPgogICAgICAgIDwvT3V0cHV0PgogICAgICAgIDxPdXRwdXQgUHJvZHVjdF9JRD0iNzM5NzEyOTgzMiI+CiAgICAgICAgICA8RmlsZV9UeXBlPkwyX19DT19fX188L0ZpbGVfVHlwZT4KICAgICAgICAgIDxGaWxlX05hbWVfVHlwZT5QSFlTSUNBTDwvRmlsZV9OYW1lX1R5cGU+CiAgICAgICAgICA8TGlzdF9vZl9GaWxlX05hbWVzIGNvdW50PSIxIj4KICAgICAgICAgICAgPEZpbGVfTmFtZSBGU19UWVBFPSJBTExVWElPIj5yZXN1bHRzX0IvcmVzdWx0MDIudHh0PC9GaWxlX05hbWU+CiAgICAgICAgICA8L0xpc3Rfb2ZfRmlsZV9OYW1lcz4KICAgICAgICA8L091dHB1dD4KICAgICAgICA8T3V0cHV0IFByb2R1Y3RfSUQ9IjczOTcxMjk4MzMiPgogICAgICAgICAgPEZpbGVfVHlwZT5MMl9fQ09fX19fPC9GaWxlX1R5cGU+CiAgICAgICAgICA8RmlsZV9OYW1lX1R5cGU+UEhZU0lDQUw8L0ZpbGVfTmFtZV9UeXBlPgogICAgICAgICAgPExpc3Rfb2ZfRmlsZV9OYW1lcyBjb3VudD0iMSI+CiAgICAgICAgICAgIDxGaWxlX05hbWUgRlNfVFlQRT0iUzMiPnJlc3VsdHNfQy9yZXN1bHQwMy50eHQ8L0ZpbGVfTmFtZT4KICAgICAgICAgIDwvTGlzdF9vZl9GaWxlX05hbWVzPgogICAgICAgIDwvT3V0cHV0PgogICAgICA8L0xpc3Rfb2ZfT3V0cHV0cz4KICAgIDwvSXBmX1Byb2M+CiAgPC9MaXN0X29mX0lwZl9Qcm9jcz4KPC9JcGZfSm9iX09yZGVyPg=="
}
```

## Run Proc-Image
- cd prosEO/samples
- edit run.sh
  - change env-var JOBORDER_FILE to response URL of previous step!!
- ./run.sh

## sample-output
```sh
[dev1@gate0 samples]$ ./run.sh
2019-10-21T13:15:48.390 sample-wrapper 00.01: [I]

{"prosEO" : "A Processing System for Earth Observation Data"}

2019-10-21T13:15:48.392 sample-wrapper 00.01: [I] Starting base-wrapper V00.00.01 with JobOrder file s3://internal/joborders/2b114127-7ead-4a52-8cf8-bf9268193d42.xml
2019-10-21T13:15:48.394 sample-wrapper 00.01: [I] Checking 11 ENV_VARS...
2019-10-21T13:15:48.431 sample-wrapper 00.01: [I] ... JOBORDER_FS_TYPE=S3
2019-10-21T13:15:48.432 sample-wrapper 00.01: [I] ... JOBORDER_FILE=s3://internal/joborders/2b114127-7ead-4a52-8cf8-bf9268193d42.xml
2019-10-21T13:15:48.432 sample-wrapper 00.01: [I] ... S3_ENDPOINT=http://localhost:9000
2019-10-21T13:15:48.432 sample-wrapper 00.01: [I] ... S3_ACCESS_KEY=short_access_key
2019-10-21T13:15:48.432 sample-wrapper 00.01: [I] ... S3_SECRET_ACCESS_KEY=short_secret_key
2019-10-21T13:15:48.432 sample-wrapper 00.01: [I] ... S3_STORAGE_ID_OUTPUTS=s3test
2019-10-21T13:15:48.432 sample-wrapper 00.01: [I] ... ALLUXIO_STORAGE_ID_OUTPUTS=alluxio1
2019-10-21T13:15:48.432 sample-wrapper 00.01: [I] ... INGESTOR_ENDPOINT=http://localhost:8082
2019-10-21T13:15:48.432 sample-wrapper 00.01: [I] ... STATE_CALLBACK_ENDPOINT=http://localhost:8080/proseo/planner/v0.1/processingfacilities/Lerchenhof/finish/proseojob968
2019-10-21T13:15:48.432 sample-wrapper 00.01: [I] ... PROCESSOR_SHELL_COMMAND=java -jar proseo-sample-processor.jar
2019-10-21T13:15:48.432 sample-wrapper 00.01: [I] ... PROCESSING_FACILITY_NAME=Lerchenhof
2019-10-21T13:15:48.433 sample-wrapper 00.01: [I] ENV_VARS looking good...
2019-10-21T13:15:48.433 sample-wrapper 00.01: [I] PREFIX timestamp used for JobOrderFile-Naming & Results is 1571663747
2019-10-21T13:15:49.558 sample-wrapper 00.01: [I] Copied s3://internal/joborders/2b114127-7ead-4a52-8cf8-bf9268193d42.xml to file://joborders/2b114127-7ead-4a52-8cf8-bf9268193d42.xml
2019-10-21T13:15:49.598 sample-wrapper 00.01: [I] Fetch Inputs & provide a valid JobOrderFile for the container-context...
2019-10-21T13:15:49.946 sample-wrapper 00.01: [I] EPOLL_MODE is available
2019-10-21T13:15:50.009 sample-wrapper 00.01: [I] Initialized tiered identity TieredIdentity(node=10.0.2.15, rack=null)
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.google.protobuf.UnsafeUtil (file:/usr/share/sample-processor/proseo-sample-wrapper.jar) to field java.nio.Buffer.address
WARNING: Please consider reporting this to the maintainers of com.google.protobuf.UnsafeUtil
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
2019-10-21T13:15:50.483 sample-wrapper 00.01: [I] Alluxio client has loaded configuration from meta master 10.0.2.15/10.0.2.15:19998
2019-10-21T13:15:50.491 sample-wrapper 00.01: [I] Alluxio client (version 2.0.0) is trying to load cluster level configurations
2019-10-21T13:15:50.515 sample-wrapper 00.01: [I] Alluxio client has loaded cluster level configurations
2019-10-21T13:15:50.515 sample-wrapper 00.01: [I] Alluxio client (version 2.0.0) is trying to load path level configurations
2019-10-21T13:15:50.609 sample-wrapper 00.01: [I] Alluxio client has loaded path level configurations
2019-10-21T13:15:51.158 sample-wrapper 00.01: [I] Copied alluxio:///alluxio1/3244232/file01.txt to file://inputs/alluxio1/3244232/file01.txt
2019-10-21T13:15:51.279 sample-wrapper 00.01: [I] Copied alluxio:///alluxio1/3244232/file02.txt to file://inputs/alluxio1/3244232/file02.txt
2019-10-21T13:15:51.309 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file03.txt to file://inputs/s3test/3244233/file03.txt
2019-10-21T13:15:51.324 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file04.txt to file://inputs/s3test/3244233/file04.txt
2019-10-21T13:15:51.337 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file05.txt to file://inputs/s3test/3244233/file05.txt
2019-10-21T13:15:51.349 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file06.txt to file://inputs/s3test/3244233/file06.txt
2019-10-21T13:15:51.425 sample-wrapper 00.01: [I] Copied alluxio:///alluxio1/3244232/file07.txt to file://inputs/alluxio1/3244232/file07.txt
2019-10-21T13:15:51.524 sample-wrapper 00.01: [I] Copied alluxio:///alluxio1/3244232/file08.txt to file://inputs/alluxio1/3244232/file08.txt
2019-10-21T13:15:51.542 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file09.txt to file://inputs/s3test/3244233/file09.txt
2019-10-21T13:15:51.556 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file10.txt to file://inputs/s3test/3244233/file10.txt
2019-10-21T13:15:51.568 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file11.txt to file://inputs/s3test/3244233/file11.txt
2019-10-21T13:15:51.582 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file12.txt to file://inputs/s3test/3244233/file12.txt
2019-10-21T13:15:51.600 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file13.txt to file://inputs/s3test/3244233/file13.txt
2019-10-21T13:15:51.616 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file14.txt to file://inputs/s3test/3244233/file14.txt
2019-10-21T13:15:51.631 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file15.txt to file://inputs/s3test/3244233/file15.txt
2019-10-21T13:15:51.641 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file16.txt to file://inputs/s3test/3244233/file16.txt
2019-10-21T13:15:51.653 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file16.txt to file://inputs/s3test/3244233/file16.txt
2019-10-21T13:15:51.666 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file16.txt to file://inputs/s3test/3244233/file16.txt
2019-10-21T13:15:51.678 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file17.txt to file://inputs/s3test/3244233/file17.txt
2019-10-21T13:15:51.692 sample-wrapper 00.01: [I] Copied s3://s3test/3244233/file18.txt to file://inputs/s3test/3244233/file18.txt
2019-10-21T13:15:51.695 sample-wrapper 00.01: [I] Fetched 20 Input-Files and prepared dirs for 3 Outputs -- Ready for processing using Container-JOF /usr/share/sample-processor/1571663747.xml
2019-10-21T13:15:51.799 sample-wrapper 00.01: [I] Starting Processing using command java -jar proseo-sample-processor.jar and local JobOrderFile: /usr/share/sample-processor/1571663747.xml
2019-10-21T13:15:52.502 sample-wrapper 00.01: [I] ... 2019-10-21T13:15:52.497000 localhost sample-processor 00.01 [0000012345]: [I] Starting sample-processor V00.00.01 with JobOrder file /usr/share/sample-processor/1571663747.xml
2019-10-21T13:15:52.541 sample-wrapper 00.01: [I] ... 2019-10-21T13:15:52.539000 localhost sample-processor 00.01 [0000012345]: [D] ... input product test-read with file inputs/alluxio1/3244232/file01.txt having 1 fields
2019-10-21T13:15:52.576 sample-wrapper 00.01: [I] ... 2019-10-21T13:15:52.573000 localhost sample-processor 00.01 [0000012345]: [D] ... creating output product file 1571663747/results_A/result01.txt
2019-10-21T13:15:52.585 sample-wrapper 00.01: [I] ... 2019-10-21T13:15:52.585000 localhost sample-processor 00.01 [0000012345]: [D] ... creating output product file 1571663747/results_B/result02.txt
2019-10-21T13:15:52.588 sample-wrapper 00.01: [I] ... 2019-10-21T13:15:52.586000 localhost sample-processor 00.01 [0000012345]: [D] ... creating output product file 1571663747/results_C/result03.txt
2019-10-21T13:15:52.589 sample-wrapper 00.01: [I] ... 2019-10-21T13:15:52.587000 localhost sample-processor 00.01 [0000012345]: [I] Leaving sample-processor with exit code 0 (OK)
2019-10-21T13:15:52.599 sample-wrapper 00.01: [I] Processing finished with return code 0
2019-10-21T13:15:52.600 sample-wrapper 00.01: [I] Uploading results to prosEO storage...
2019-10-21T13:15:52.600 sample-wrapper 00.01: [I] Upload File-Pattern based on timestamp-prefix is: FS_TYPE://<product_id>/1571663747/<filename>
2019-10-21T13:15:53.632 sample-wrapper 00.01: [I] Copied file://1571663747/results_A/result01.txt to alluxio://alluxio1/7397129831/1571663747/results_A/result01.txt
2019-10-21T13:15:53.995 sample-wrapper 00.01: [I] Copied file://1571663747/results_B/result02.txt to alluxio://alluxio1/7397129832/1571663747/results_B/result02.txt
2019-10-21T13:15:54.282 sample-wrapper 00.01: [I] Copied file://1571663747/results_C/result03.txt to s3://s3test/7397129833/1571663747/results_C/result03.txt
2019-10-21T13:15:54.283 sample-wrapper 00.01: [I] Uploaded 3 results to prosEO storage...
2019-10-21T13:15:54.283 sample-wrapper 00.01: [I] Upload summary: listing 3 Outputs of type `PushedProcessingOutput`
2019-10-21T13:15:54.283 sample-wrapper 00.01: [I] PRODUCT_ID=7397129831, FS_TYPE=ALLUXIO, PATH=/alluxio1/7397129831/1571663747/results_A/result01.txt, REVISION=1571663747
2019-10-21T13:15:54.283 sample-wrapper 00.01: [I] PRODUCT_ID=7397129832, FS_TYPE=ALLUXIO, PATH=/alluxio1/7397129832/1571663747/results_B/result02.txt, REVISION=1571663747
2019-10-21T13:15:54.284 sample-wrapper 00.01: [I] PRODUCT_ID=7397129833, FS_TYPE=S3, PATH=s3test/7397129833/1571663747/results_C/result03.txt, REVISION=1571663747
2019-10-21T13:15:54.284 sample-wrapper 00.01: [I] Trying to register 3 products with prosEO-Ingestor@http://localhost:8082
2019-10-21T13:15:54.377 sample-wrapper 00.01: [I] POST http://localhost:8082/ingest/Lerchenhof/7397129831
2019-10-21T13:15:54.781 sample-wrapper 00.01: [E] java.net.ConnectException: Connection refused (Connection refused)
2019-10-21T13:15:54.784 sample-wrapper 00.01: [I] POST http://localhost:8082/ingest/Lerchenhof/7397129832
2019-10-21T13:15:54.829 sample-wrapper 00.01: [E] java.net.ConnectException: Connection refused (Connection refused)
2019-10-21T13:15:54.831 sample-wrapper 00.01: [I] POST http://localhost:8082/ingest/Lerchenhof/7397129833
2019-10-21T13:15:54.891 sample-wrapper 00.01: [E] java.net.ConnectException: Connection refused (Connection refused)
2019-10-21T13:15:54.891 sample-wrapper 00.01: [E] 0 out of 3 products have been ingested...
2019-10-21T13:15:54.892 sample-wrapper 00.01: [I] PATCH http://localhost:8080/proseo/planner/v0.1/processingfacilities/Lerchenhof/finish/proseojob968?status=FAILURE
2019-10-21T13:15:54.959 sample-wrapper 00.01: [E] java.net.ConnectException: Connection refused (Connection refused)
2019-10-21T13:15:54.959 sample-wrapper 00.01: [I] Leaving base-wrapper with exit code 255 (FAILURE)
```


