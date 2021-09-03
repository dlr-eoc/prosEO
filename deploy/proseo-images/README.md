prosEO Image Build Example
==========================

The steps below are intended to be run on a development machine, not in the target environment (which will probably not yet be
available at this point).


# Prerequisites

The following prerequisites are required before starting the build process:
- Successful and recent `mvn clean install -DskipTests` in the prosEO repository root,
- Access to a valid and working Docker registry (`docker login <registry-url>` shall be successful).


# Required configuration

Before building the images, the following secrets need to be determined:
- The database password
- The shared secret of the prosEO Ingestor and the prosEO Storage Manager
- The authentication token for the InfluxDB monitoring database (an alphanumeric token of e. g. 32 characters length)

Then for all prosEO components (all subdirectories in `proseo-components` except `proseo-db` and `proseo-pgadmin`)
the file `application.yml.template` needs to be copied into a new file `application.yml`. The following updates are then needed
for the new `application.yml` files:
- All files except in `proseo-ui-gui` and `proseo-storage-mgr`: Set the database password in the property `spring.datasource.password`.
- In `proseo-ingestor` and `proseo-storage-mgr`: Set the shared secret in the property `proseo.storageManager.secret`.
- In `proseo-ingestor`, `proseo-planner` and `proseo-order-mgr`: Set the InfluxDB authentication token in the property `proseo.log.token`.

Changes to the logging configuration may be applied to the `logback.xml` files as needed, however the standard configuration
should be a reasonable starting point.


# Build all deployment docker images

The build process creates new images with the specific `application.yml` and `logback.xml` files. The build process is started
for a specific registry (e. g. `proseo-registry.eoc.dlr.de`), which is passed as parameter to the build script:
```sh
./build_images.sh <registry-url>
```

# Push all images

After the build the new images need to be pushed to the target registry (e. g. `proseo-registry.eoc.dlr.de`),
which again is passed as parameter to the push script:
```sh
./push_images.sh <registry-url>
```
