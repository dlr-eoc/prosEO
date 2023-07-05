# prosEO – the Processing System for Earth Observation Data


## Introduction

The “prosEO” software system is an open-source processing control system is designed to perform all activities required to process
Earth Observation satellite data (e. g. Sentinel data), generating user-level data, engineering data and/or housekeeping telemetry
data as desired by a configured mission. The technical infrastructure used to deliver the Production Service is a cloud-native
multi-mission infrastructure by design, with strict separation of competences and concerns.

A full description of the design approach can be found on the
[prosEO Wiki](https://github.com/dlr-eoc/prosEO/wiki/Building-a-Production-Service-Based-on-prosEO).


## License

prosEO is licensed under the GNU Public License (GPL) version 3.


## Build from source code

To build prosEO from source code, the following prerequisites must be met:
- OpenJDK 11 installed locally
- Maven installed locally
- Docker installed locally (for Windows or Mac: Docker Desktop)
- Run a local registry on port 5000:
  ```
  docker run -d -p 5000:5000 --restart always \
       -e STORAGE_DELETE_ENABLED=true \
       -v <path/to/local/registry/dir>:/var/lib/registry \
       --name registry \
       registry:2
  ```
  The `-e` and `-v` options are recommended for better maintenance of the local registry. For the management of the registry
  we recommend using a GUI tool, e. g. Joxit (<https://joxit.dev/docker-registry-ui/>). Sample (!) files for running the docker
  registry and the Joxit GUI are included in the `src/docker` directory.
  
- Add the following to your Maven settings file (usually at `$HOME/.m2/settings.xml`):
  ```
  <settings>
    <profiles>
      <profile>
        <id>dev-local</id><!-- or any other id you prefer -->
        <activation>
          <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
          <docker.registry>localhost:5000</docker.registry>
        </properties>
      </profile>
    </profiles>
  </settings>
  ```
  If you already have a `properties` element in your settings file, it is of course
  sufficient to just add the `docker.registry` property there.
- Add the following to your Docker Engine configuration (e. g. via the Docker Dashboard):
  ```
  "insecure-registries": [
    "localhost:5000"
  ],
  ```
- Push the OpenJDK 11 image to your local repository:
  ```
  docker pull openjdk:11
  docker tag openjdk:11 localhost:5000/openjdk:11
  docker push localhost:5000/openjdk:11
  ```
- Install Node.js including `npm` (installation packages can be found on (https://nodejs.org)).
- Install the `raml2html` helper ((https://github.com/raml2html/raml2html)):
  ```
  npm i -g raml2html
  ```
  
To test your development environment, change into the prosEO project directory (the directory, where this README file
resides) and run
```
mvn clean install -Dmaven.test.skip=true
```

A project setup for Eclipse is beyond the scope of this documentation.


## Installation

prosEO is a very complex system, therefore no single installer can be provided. A deployment guide on the Wiki will be added
in due course.


## Documentation

All documentation can be found on the [prosEO Wiki](https://github.com/dlr-eoc/prosEO/wiki).


## Contributions

prosEO is currently developed by the German Aerospace Center (DLR) together with Dr. Bassler & Co. Managementberatung (BCM) and
Prophos Informatik. For contributions to the project please contact [tangobravo62](mailto:thomas.bassler@drbassler.de). For support
requests, please contact [prosEO-support@drbassler.de](prosEO-support@drbassler.de).
