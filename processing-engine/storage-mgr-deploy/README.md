prosEO-storage-mgr-deploy
=========================

## prerequisites
- for k8s-deployment you need a working kubectl config at your host

## standalone docker-image
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
#-->image name:tag is localhost:5000/proseo-storage-mgr:0.0.1-SNAPSHOT-rc1
```

## push to some registry
- in case of insecure-registry create a file `/etc/docker/daemon.json`
```js
{
"insecure-registries": ["<registry-url"]
}
```
- restart docker-engine (sudo systemctl restart docker)
- login to the registry
```sh
docker login <registry-url>
```
- re-tag our docker-image
```sh
docker tag localhost:5000/proseo-storage-mgr:0.0.1-SNAPSHOT-rc1 <registry-url>/<repo>/sample-integration-processor:0.0.1-SNAPSHOT-rc1
docker push <registry-url>/<repo>/sample-integration-processor:0.0.1-SNAPSHOT-rc1
```

## kubernetes
- first build and push docker image (steps above)
- deploy a storage-mgr daemonset (--> storage-mgr at every k8s-node)
```sh
cd kubernetes
kubectl create -f storage-mgr.yaml
```