CProS Kubernetes Cluster Deployment
===================================


# Prerequisites

## Keys for SSH login

For access to the cluster nodes the public and private keys for SSH login have to be provided in the
`keys` directory as `id_rsa` and `id_rsa.pub` (if different filenames are used, the commands listed
below need to be changed accordingly).

## Ansible

Ansible for Kubespray is run from a preconfigured container:
```
docker pull quay.io/kubespray/kubespray:v2.16.0
docker run --rm -it --mount type=bind,source="$(pwd)"/inventory,dst=/inventory \
  --mount type=bind,source="$(pwd)"/keys/id_rsa,dst=/root/.ssh/id_rsa \
  quay.io/kubespray/kubespray:v2.16.0 bash
```


# Deploy Kubernetes cluster

The files in `inventory/proseo` have been copied from the `inventory/sample` directory
in the above mentioned container. The following files have been changed:
- `group_vars/k8s_cluster/addons.yml`
- `group_vars/k8s_cluster/k8s_cluster.yml`

The `inventory.ini` file was replaced by a `hosts.yml` file. This file must be updated according to the physical (VM)
infrastructure, and whenever the infrastructure has been changed. Also in the file `group_vars/k8s_cluster/k8s_cluster.yml`
the IP address of the bastion host for the control instance needs to be updated in the variable `ansible_ssh_common_args`.

A file `predeploy.yml` has been added to setup the networking in newly provisioned hosts. In this file the IP address
of the network gateway must be updated to the actual IP address of the provisioned gateway.


# Deploy the configuration

The initial deployment is performed from within the Kubespray container. The first step is to prepare
the cluster nodes:
```
ansible-playbook -i /inventory/proseo/hosts.yml -u root -b -v --private-key /root/.ssh/id_rsa /inventory/proseo/predeploy.yml
```

Then the Kubernetes cluster can be generated with
```
ansible-playbook -i /inventory/proseo/hosts.yml -u root -b -v --private-key /root/.ssh/id_rsa cluster.yml
```

After generating the cluster, a file called `kubeadm_certificate_key.creds` will be present in a new folder
`inventory/proseo/credentials`. This file is implementation-specific, therefore the whole `credentials` folder
has been excluded from management by Git.

To update deployments use the `scale.yml` and `remove-node.yml`. For further instructions see
(https://kubespray.io/#/docs/nodes)


# Configure Kubernetes

As an optional preparation for the following steps, configure your local `kubectl` (on the deployment controller) to access
the Kubernetes instance using the file `/root/.kube/config` found on the Kubernetes master node.

## Configure access to the Kubernetes Dashboard

The Kubernetes Dashboard has already been deployed with Kubernetes. To get access to the Dashboard, first an administrator
account must be created in Kubernetes. The file `kube-admin.yaml` contains the required configuration.
From it, create the administrator account:
```
kubectl apply -f kube-admin.yaml
```

__CAUTION:__ This will grant full super-user access to your cluster! For information on how to create a role binding
with more restricted privileges, please see
[the Kubernetes RBAC documentation](https://kubernetes.io/docs/reference/access-authn-authz/rbac/#user-facing-roles).
   
Find the account's secret and copy the token to a safe place (note that there is no line break between the end of the
token and the prompt for the next shell command):
```
kubectl get secret -n kube-system \
  $(kubectl get serviceaccount admin-user -n kube-system -o jsonpath="{.secrets[0].name}") \
  -o jsonpath="{.data.token}" | base64 --decode 
```

You can now access the Kubernetes dashboard from any browser (e. g. on your local workstation) using the URL
https://your.bastion.host/kubectl/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/
clicking on "Token" at the login screen and providing the saved token string as input.


## Configure access to the NFS server in Kubernetes

For the Kubernetes worker nodes to get access to the NFS server, a persistent volume with an NFS driver must be
set up. Create an `kubernetes/nfs-pv.yaml` file from the template file given in the `kubernetes` folder, replacing
the NFS server IP address with the actual address in your environment. Then create the NFS persistent volume:
```
kubectl apply -f kubernetes/nfs-pv.yaml
```


## Configure an account for the Production Planner

For the Production Planner, an account with access to the Kubernetes API is required. The account must be able to read general
information about the Kubernetes cluster (health state, node list) and to fully manage jobs and pods (create, update, list, delete).

Create the account, role and role binding, and retrieve the authentication token for the new account:
```bash
kubectl apply -f kubernetes/planner-account.yaml
kubectl describe secret/$(kubectl get secrets | grep proseo-planner | cut -d ' ' -f 1)
```


## Create a Kubernetes secret for private Docker registry access

Create a Kubernetes secret holding the credentials for the private registry on some host, which has both `kubectl` and Docker
(not Docker Desktop!) configured (e. g. the Kubernetes master node):
```sh
docker login <registry-url>
# Check if docker client file is under ~/.docker/config.json, and make sure it actually contains the desired credentials
cat ~/.docker/config.json
# Create Kubernetes secret
kubectl create secret generic proseo-regcred  --from-file=.dockerconfigjson=$HOME/.docker/config.json --type=kubernetes.io/dockerconfigjson
```
See also: <https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/>


# Alternative deployment of Storage Manager in Kubernetes

Instead of running the Storage Manager as a Docker container on the NFS server, as the present deployment description
assumes, it may also be started as a service in Kubernetes. A sample service definition can be found in
`kubernetes/storage-mgr.yaml.template`.
