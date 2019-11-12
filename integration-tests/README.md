prosEO test integration
=======================

## prerequisites
- successful & recent `mvn clean install -DskipTests` in prosEO repo root
- valid & context-specific application.yml files under `proseo-components/<component>/application.yml`
  - each component has an `application.yml.template` file included
- for component `proseo-db` a valid `init.sql` file has to be in place
  - a template file in `proseo-components/proseo-db/init.sql.template is present
- access to a valid & working docker-registry (docker login <registry-url> shall be successful)

## build all deployment docker images
```sh
./build_integration_images.sh <registry-url>
```

## push all images
```sh
./push_integration_images.sh <registry-url> <proseo-revison>
```

## start a local docker-compose stack
```sh
./run_local_control_stack.sh <registry-url> <proseo-revison>
```