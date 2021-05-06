Alluxio k8s-deploy
==================

## Disclaimer
A Storage Manager implementation based on Alluxio is envisioned, but has not been
fully developed yet, and it has never been tested so far.


## prerequisites (->../k8s-deploy)
- running k8s cluster 
- valid & reachable k8s-gateway
- kubectl configured at your localhost

## generate config file templates
run the following commands at your localhost (some place where kubectl is configured)

```sh
id=$(docker create alluxio/alluxio:2.0.1)
docker cp $id:/opt/alluxio/integration/kubernetes/ - > kubernetes.tar
docker rm -v $id 1>/dev/null
tar -xvf kubernetes.tar
cd kubernetes
```

## provision master persistent volume

```sh
cp alluxio-journal-volume.yaml.template alluxio-journal-volume.yaml
kubectl create -f alluxio-journal-volume.yaml
```

## Configure Alluxio properties

```sh
cp alluxio-configMap.yaml.template alluxio-configMap.yaml
```

- now edit the yaml file:

```python
apiVersion: v1
kind: ConfigMap
metadata:
  name: alluxio-config
data:
  ALLUXIO_JAVA_OPTS: |-
    -Dalluxio.master.hostname=alluxio-master -Dalluxio.master.journal.type=UFS -Dalluxio.master.journal.folder=/journal -Dalluxio.worker.data.server.domain.socket.address=/opt/domain -Dalluxio.worker.data.server.domain.socket.as.uuid=true -Dalluxio.worker.memory.size=2G -Dalluxio.worker.rpc.port=29999 -Dalluxio.worker.web.port=29996 -Dalluxio.job.worker.rpc.port=30001 -Dalluxio.job.worker.data.port=30002 -Dalluxio.job.worker.web.port=30003 -Daws.accessKeyId=XXXXXXXXXX -Daws.secretKey=XXXXXXXXXXX -Dalluxio.underfs.s3.endpoint=https://obs.eu-de.otc.t-systems.com -Dalluxio.underfs.s3.disable.dns.buckets=true -Dalluxio.underfs.s3.inherit.acl=false -Dalluxio.master.mount.table.root.ufs=s3://XXXXXXX -Dalluxio.user.file.metadata.sync.interval=0 -Dalluxio.underfs.s3.socket.timeout=500sec -Dalluxio.underfs.s3.request.timeout=5min -Dalluxio.underfs.s3.admin.threads.max=80 -Dalluxio.underfs.s3.threads.max=160 -Dalluxio.underfs.s3.upload.threads.max=80 -Dalluxio.underfs.object.store.service.threads=80 -Dalluxio.underfs.cleanup.enabled=true -Dalluxio.underfs.s3.streaming.upload.enabled=true -Dalluxio.underfs.s3.signer.algorithm=S3SignerType
```

- deploy config-map

```sh
kubectl create -f alluxio-configMap.yaml
```

## Deploy Alluxio Master & Worker

```sh
cp alluxio-master.yaml.template alluxio-master.yaml
cp alluxio-worker.yaml.template alluxio-worker.yaml
# optionally edit worker spec & deploy...
kubectl create -f alluxio-master.yaml
kubectl create -f alluxio-worker.yaml
```

## Verify Alluxio

```sh
kubectl get pods
kubectl exec -ti alluxio-master-0 /bin/bash
cd /opt/alluxio
./bin/alluxio runTests
```

## Rollback & Delete

```sh
kubectl delete -f alluxio-worker.yaml
kubectl delete -f alluxio-master.yaml
kubectl delete -f alluxio-journal-volume.yaml
kubectl delete -f alluxio-configMap.yaml
```

