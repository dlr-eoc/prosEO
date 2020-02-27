prosEO test integration
=======================

## prerequisites
- successful & recent `mvn clean install -DskipTests` in prosEO repo root
- valid & context-specific application.yml files under `proseo-components/<component>/application.yml`
  - each component has an `application.yml.template` file included
- for component `proseo-db` a valid `init.sql` file has to be in place
  - a template file in `proseo-components/proseo-db/init.sql.template` is present
- access to a valid & working docker-registry (`docker login <registry-url>` shall be successful)

## build all deployment docker images
```sh
./build_integration_images.sh <registry-url>
```

## push all images
```sh
./push_integration_images.sh <registry-url>
```

## start a local docker-compose stack
First a certificate for running pgadmin needs to be created in `proseo-components/proseo-pgadmin/certs`:
```sh
[sudo] openssl req -x509 -newkey rsa:4096 -keyout proseo-components/proseo-pgadmin/certs/proseo-selfsigned.key -out proseo-components/proseo-pgadmin/certs/proseo-selfsigned.crt -days 9999 -nodes
```
For the pgadmin-container run:
```sh
export PGADMIN_EMAIL=some.custom@email.address
export PGADMIN_PASSWORD=some-pw
```
Then the containers can be started:
```sh
./run_local_control_stack.sh <registry-url>
```

## Deploy compose-stack to a real webserver
the docker-compose config could be deployed to a public ssl-enabled webserver. The following steps for a typical nginx-integration are required:
- the current stack has the following components and:
  - db
  - ingestor
  - planner
  - order-mgr
  - processor-mgr
  - productclass-mgr
  - user-mgr
  - ui-gui
  - pgadmin
- ssh to your public webserver (note: a valid domain & a valid ssl-cert are recommended)
- clone the prosEO-repository e.g. to /opt/integration-tests/prosEO
- edit your current nginx ssl-config file. e.g. under /etc/nginx/conf.d/ssl.conf
  - inside the server-tag insert the following line:
```sh
include /opt/integration-tests/prosEO/integration-tests/controlCmp.conf;
```
- stop running compose stack using `stop_local_control_stack.sh`
- from /opt/integration-tests/prosEO/integration-tests run the script `run_local_control_stack.sh`
- run systemctl restart nginx


