prosEO k8s setup (OpenStack)
===========================
deploy a k8s-cluster using terraform & kubespray

### prerequisites
- docker engine running at your control host
- ssh-keygen
- unrestricted internet access

### deploy steps
- create public and private ssh-key (e.g. by using ssh-keygen) and copy the keys to:
  - ssh-keys/`cluster-key.pem` (private key)
  - ssh-keys/`cluster-key.pub` (public key)
- create file `terraform/.ostackrc`

```sh
cp terraform/.ostackrc.template terraform/.ostackrc
vi terraform/.ostackrc
```

the file shall look like...
```sh
# .bashrc
# User specific aliases and functions
alias rm='rm -i'
alias cp='cp -i'
alias mv='mv -i'
# Source global definitions
if [ -f /etc/bashrc ]; then
        . /etc/bashrc
fi

export OS_PASSWORD=XXXX
export OS_USERNAME=XXXX
export OS_TENANT_NAME=eu-de
export OS_PROJECT_NAME=eu-de
export OS_USER_DOMAIN_NAME=OTC-EU-DE-XXXX
export OS_AUTH_URL=https://iam.eu-de.otc.t-systems.com:443/v3
export OS_PROJECT_DOMAIN_NAME=XXXX
export OS_IDENTITY_API_VERSION=3
export OS_REGION_NAME=eu-de
export OS_VOLUME_API_VERSION=2
export OS_IMAGE_API_VERSION=2
export OS_ENDPOINT_TYPE=publicURL
export NOVA_ENDPOINT_TYPE=publicURL
export CINDER_ENDPOINT_TYPE=publicURL
```
- build deployer docker image by running `build.sh`

### run deployer docker image
- cd /prosEO/processing-engine/k8s-deploy
- `run.sh` -> this opens a shell inside the container

### run terraform (inside container)
terraform is responsible for the provisioning of the VM's and network-interfaces. 
The current state of the deployment is stored outside the container. (prosEO/processing-engine/k8s-deploy/terraform/state), so container restarts are uncritical...
```sh
terraform init -var-file=cluster.tfvars ../../contrib/terraform/openstack
terraform apply -var-file=cluster.tfvars ../../contrib/terraform/openstack
```

### prepare created bastion-VM (from inside container)
```sh
ssh linux@<bastion-host>
sudo vi /etc/ssh/sshd_config
AllowTcpForw.. yes
sudo systemctl restart sshd
```

### prepare bastion host tunneling (from inside container)

Kubespray can generate a SSH configuration that lets you tunnel through the
bastion host. Before you can run the complete kubespray setup, you need to run
this.
```sh
cd /kubespray
ansible-playbook -i inventory/proseo-spray/hosts --become  cluster.yml --flush-cache -l bastion
```

This generates a `ssh-bastion.conf`, which is most easily used by copying it to
the user configuration file.
```sh
cd /kubespray
cp ssh-bastion.conf ~/.ssh/config
```

SSH tunnels through the bastion host will be set up when any of the cluster-
internal IP addresses are accessed.

### check ssh-connectivity between nodes (inside container)
```sh
cd /kubespray/inventory/proseo-spray/
ANSIBLE_HOST_KEY_CHECKING=False ansible -i hosts -m ping all
```

*Note 1:* Ansible is not good at letting you accept host SSH keys. Setting
this environment variable to `False` bypasses checks, which is not usually
safe. However, when using terraform, the keys were just generated - there is
not much you can do to verify the keys by any means that will apply
everywhere.

*Note 2:* As a result of the ping command, SSH tunnels have been established
from the deploy container through the bastion host to internal nodes. You
can verify this by running `ps` and seeing a lot of SSH processes. If ansible
fails because a tunnel is not working, re-running this ping command should
work to create them.

### run kubespray (inside container)
kubespray is responsible for the complete sw-config of all k8s-components
```sh
cd /kubespray
ansible-playbook -i inventory/proseo-spray/hosts --become  cluster.yml --flush-cache
```

### run post install recipe (inside container)
the postinstall.yml is responsible for creating a stable nginx reverse proxy cfg for publishing the k8s-apis...
```sh
cd /kubespray
ansible-playbook -i inventory/proseo-spray/hosts --become  postinstall.yml --flush-cache
```

### install k8s-dashboard (from bastion host)

```sh
# install it...
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.0-beta5/aio/deploy/recommended.yaml
kubectl create serviceaccount k8s-poweruser
kubectl create clusterrolebinding k8s-poweruser-cadmin --clusterrole=cluster-admin --serviceaccount=default:k8s-poweruser
kubectl create clusterrolebinding k8s-poweruser-admin --clusterrole=admin --serviceaccount=default:k8s-poweruser
kubectl -n default describe secret $(kubectl -n default get secret | awk '/^k8s-poweruser-token-/{print $1}') | awk '$1=="token:"{print $2}'

# dashboard-url will be: 
# https://proseo-k8s-gate.de/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
# get dashboard login-token (use last line from stdout):
kubectl -n default describe secret $(kubectl -n default get secret | awk '/^k8s-poweruser-token-/{print $1}') | awk '$1=="token:"{print $2}'
```

### create k8s-secret holding credentials for private registry

this secret is used when k8s needs to pull images from some private registry

```sh
# on some host where kubectl is configured
docker login <registry-url>
#check if docker-client file is under ~/.docker/config.json
cat ~/.docker/config.json
#create k8s-secret
kubectl create secret generic regcred  --from-file=.dockerconfigjson=.docker/config.json --type=kubernetes.io/dockerconfigjson
```

