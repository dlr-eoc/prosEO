prosEO Kubernetes Cluster Deployment
====================================

This guide explains how to deploy and manage a Kubernetes cluster for prosEO using Kubespray, and configure it for prosEO applications.

### 1. Prerequisites
##### 1.1 SSH Keys
Place the SSH keys for access to cluster nodes keys/ directory (see README there):
- keys/id_rsa
- keys/id_rsa.pub

If you use different filenames, update the commands in this guide accordingly.

##### 1.2 Prepare Kubespray Container
Prepare the Kubespray container for Ansible deployment (run from this directory):
```
docker pull quay.io/kubespray/kubespray:v2.25.1

docker run --rm -it \
--mount type=bind,source="$(pwd)"/inventory,dst=/inventory \
--mount type=bind,source="$(pwd)"/keys/id_rsa,dst=/root/.ssh/id_rsa \
quay.io/kubespray/kubespray:v2.25.1 bash
```

### 2. Deploying the Cluster
##### 2.1 Prepare Inventory
Update the following files in inventory/cdse (originally copied from inventory/sample in the kubespray container):
- group_vars/k8s_cluster/addons.yml
- group_vars/k8s_cluster/k8s_cluster.yml

Update the IP addresses:
- Adjust hosts.yml to match your VM infrastructure.
- In k8s_cluster.yml, set the bastion host IP in ansible_ssh_common_args.
- Update the router IP in predeploy.yml.

##### 2.2 Prepare Nodes
```
ansible-playbook -i /inventory/cdse/hosts.yml -u eouser -b -v \
--private-key /root/.ssh/id_rsa /inventory/cdse/predeploy.yml
```
  
##### 2.3 Deploy the Cluster
```
ansible-playbook -i /inventory/cdse/hosts.yml -u eouser -b -v \
--private-key /root/.ssh/id_rsa cluster.yml
```
  
After deployment, inventory/proseo/credentials/kubeadm_certificate_key.creds will be created (excluded from version control).

### 3. Configuring Kubernetes
##### 3.1 kubectl Setup
Copy `/root/.kube/config` (possibly `/etc/kubernetes/admin.conf`) from the master to bastion-control/roles/install_kubectl/files. Update the IP.

Optionally, configure your local `kubectl` (on the deployment controller) to access the Kubernetes instance using this file.

##### 3.2 Kubernetes Dashboard
Create an admin account and retrieve the token and save the token:
```
kubectl apply -f kube-admin.yaml
kubectl describe secret/$(kubectl get secrets --namespace kube-system | grep admin-user | cut -d ' ' -f 1) --namespace kube-system
``` 

Open the dashboard and login with the token:
https://your.bastion.host/kubectl/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/

Warning: this account has full cluster admin rights. For restricted access, see Kubernetes RBAC.

### 4. NFS Setup
##### 4.1 NFS Persistent Volumes
Create `kubernetes/nfs-pv.yaml` file from the template and set your NFS server IP. Then:
```
kubectl apply -f kubernetes/nfs-pv.yaml
kubectl apply -f kubernetes/nfs-pvc.yaml
```
  
### 5. prosEO Planner Configuration
##### 5.1 Create Planner Account
```
kubectl apply -f kubernetes/planner-account.yaml
kubectl describe secret/$(kubectl get secrets | grep proseo-planner | cut -d ' ' -f 1)
```

Save the token - prosEO uses it for Kubernetes API access. Must be copied to facility.

##### 5.2 Private Docker Registry
Log into the docker registry with `docker login <registry-url>` .
Then, verify credentials with `cat ~/.docker/config.json`.

Create the Kubernetes secret:
```
kubectl create secret generic proseo-regcred \
--from-file=.dockerconfigjson=$HOME/.docker/config.json \
--type=kubernetes.io/dockerconfigjson
```

See also: <https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/>

### 6. Application Deployment
##### 6.1 Background Monitors
Some applications running as background tasks can be deployed directly to Kubernetes: `kubectl apply -f kubernetes/<deployment-file>`  

Available deployments:
- s1-mpl-monitor.yaml (requires `MPL/log` and `MPL/processed` directories on NFS)
- s1a-obs-orbit-monitor.yaml (requires `OBS/log` on NFS)

##### 6.2 Storage Manager (Optional)
You can run Storage Manager inside Kubernetes instead of directly on the NFS server.
Use the template: kubernetes/storage-mgr.yaml.template

### 7. Managing Nodes
##### 7.1 Adding Nodes
Use scale.yml (same as cluster.yml): `ansible-playbook -i /inventory/cdse/hosts.yml scale.yml`

##### 7.2 Removing Nodes
 `remove-node.yml` can be executed with the additional specification of the node(s) to remove on the command line using the option `-e "node=<nodename>,<nodename2>"` and optionally (if the node is unreachable and/or will be removed completely from the infrastructure) `-e reset_nodes=false -e allow_ungraceful_removal=true`.

To generate a bulk list of workers, adapt from the following example:
```
rm -f workers.txt
for i in 05 06 07 08 09 {10..48}; do echo -n "worker${i}," >> workers.txt; done
cat workers.txt
```

##### 7.3 Changing node CPU
If a worker node was changed in terms of CPU and memory resources available, `kubectl` must be restarted. 

First, drain the worker node with `kubectl drain <worker node> --ignore-daemonsets --delete-emptydir-data`.
Then, log in to the worker node and restart `kubectl` with `systemctl restart kubelet`

After a CPU change, reboot and reactivate the worker node in Kubernetes with `kubectl uncordon <worker node>`.

##### 7.4 Changing node disk size
While CPU and RAM changes can be done on the fly for Linux worker nodes, extending the disk size requires more manual work (not
to mention shrinking the disk size, which is out of scope of this document). 

First drain the node as above. Then, log into the worker node and perform the disk extension (a very good guide is at
`https://devops.ionos.com/tutorials/increase-the-size-of-a-linux-root-partition-without-rebooting/`:

```
# Check whether /dev/vda is indeed the current root partition
df -h | grep vda

# Update the partition table as described in the guide
fdisk /dev/vda
partprobe

# Resize/recreate the file systems
resize2fs /dev/vda1
mkswap /dev/vda2

# At this point DO NOT activate the swap (conflicts with kubelet!) and DO NOT update /etc/fstab as recommended in the guide

# Confirm that the change was effective
df -h | grep vda

# Restart and check kubelet
systemctl restart kubelet
systemctl status kubelet
```

Finally confirm that the changes are visible to Kubernetes, and reactivate the node:
```
kubectl describe <worker node>
kubectl uncordon <worker node>
```