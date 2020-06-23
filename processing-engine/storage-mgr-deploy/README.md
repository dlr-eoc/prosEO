prosEO-storage-mgr-deploy
=========================

## prerequisites
- for k8s-deployment you need a working kubectl config at your host (make sure kubectl is configured with the correct credentials)

## standalone docker-image
TODO: This section does not apply any more (no `./docker` directory), to be rewritten
- copy file `docker/application.yml.template` to `docker/application.yml`
- edit all relevant properties
- build modified docker image of storage-mgr (apllication.yml is copied next to storage-mgr.jar)
```sh
#goto prosEO-repo-root
cd ../..
#build storage mgr artifacts & docker-image
mvn clean package -pl storage-mgr -am -DskipTests
#go back to this dir
cd processing-engine/storage-mgr-deploy/
cd docker
./build.sh
#-->image name:tag is localhost:5000/proseo-storage-mgr:0.1.0-SNAPSHOT-rc1
```

## push to some registry
- in case of insecure-registry create a file `/etc/docker/daemon.json`
```js
{
"insecure-registries": ["<registry-url>"]
}
```
- restart docker-engine (sudo systemctl restart docker)
- login to the registry
```sh
docker login <registry-url>
```
- re-tag our docker-image
```sh
docker tag localhost:5000/proseo-storage-mgr:0.0.1-SNAPSHOT-rc1 <registry-url>/<repo>/proseo-storage-mgr:latest
docker push <registry-url>/<repo>/proseo-storage-mgr:latest
```

## kubernetes
See also: <https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/>

- create k8s-secret holding credentials for private registry
  ```sh
  # on some host where kubectl is configured (not a Docker Desktop host!)
  docker login <registry-url>
  # check if docker-client file is under ~/.docker/config.json, and make sure it actually contains the desired credentials
  cat ~/.docker/config.json
  # create k8s-secret
  kubectl create secret generic proseo-regcred  --from-file=.dockerconfigjson=$HOME/.docker/config.json --type=kubernetes.io/dockerconfigjson
  ```

- build and push docker image (steps above)

- deploy the storage-mgr service
    ```sh
    cd kubernetes
    sed "s/proseo-nfs-server.default.svc.cluster.local/<bastion host IP>/" <nfs-pv.yaml.template >nfs-pv.yaml
    kubectl apply -f nfs-pv.yaml
    kubectl apply -f storage-mgr.yaml
    ```