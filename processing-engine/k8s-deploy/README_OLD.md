prosEO k8s setup (OpenStack)
===========================
deploy a k8s-cluster using terraform & kubespray

## Prerequisites
The following must be provided on your local control host (e. g. development machine)
- Docker engine (for building images and running the deployer),
- `kubectl` (to control the Kubernetes cluster, optional)
- `ssh-keygen` (part of OpenSSL),
- Unrestricted Internet access

Additionally if there is a previous deployment of the Processing Facility running, which
will be upgraded or replaced, download and store the following files for future reuse
(these are created manually during the installation process, therefore there is no way
to re-create the contents automatically):
- `/etc/nginx/.htpasswd` (the login credentials for external API and Dashboard users),
- The certificate files `cert.pem`, `chain.pem`, `fullchain.pem` and `privkey.pem` from
  `/etc/letsencrypt/live/<bastion host name>/`.

## Deploy steps
Perform this on your local development machine from the prosEO source repository
sub-directory `processing-engine/k8s-deploy`:
- Create public and private SSH keys (e.g. by using ssh-keygen) and copy the keys to:
  - ssh-keys/`cluster-key.pem` (private key)
  - ssh-keys/`cluster-key.pub` (public key)
- Create the file `terraform/.ostackrc` and edit the credential information for the Cloud Provider:
  ```sh
  cp terraform/.ostackrc.template terraform/.ostackrc
  vi terraform/.ostackrc
  ```

  The file should look something like:
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
- Build the deployer docker image by running `./build.sh`.

### Run the deployer Docker image
On your local control host, still in the directory `processing-engine/k8s-deploy`, run the script `./run.sh`
to open a shell within the deployer Docker image. The following steps are performed from this shell unless
noted otherwise.

### Run Terraform (inside container)
Terraform is responsible for the provisioning of the VMs and network-interfaces. 
The current state of the deployment is stored outside the container in `processing-engine/k8s-deploy/terraform/state`,
so container restarts are uncritical.
```sh
terraform init -var-file=cluster.tfvars ../../contrib/terraform/openstack
terraform apply -var-file=cluster.tfvars ../../contrib/terraform/openstack
```

Leave the container for the next step with `exit`.

### Prepare created bastion VM (from local control host)
When first logging in to the bastion host, copy your SSH public key first, and change the password of the
`linux` user immediately after login. Remember, you're in the wild, wild Internet ...

Perform the following steps from the local control host:
```sh
ssh-copy-id linux@<bastion host> # Assuming you already created a public/private key pair using ssh-keygen
ssh linux@<bastion host>         # Use IP address given at end of terraform apply output
passwd                           # Immediately change the password for the linux user!
sudo vi /etc/ssh/sshd_config
  # modify: AllowTcpForwarding yes
sudo systemctl restart sshd
exit                             # Leave bastion host
```

### Prepare bastion host tunneling (from inside container)
Restart the container with `./run.sh`.

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

### Check SSH connectivity between nodes (inside container)
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

### Run Kubespray (inside container)
Kubespray is responsible for the complete software configuration of all Kubernetes components.
```sh
cd /kubespray
ansible-playbook -i inventory/proseo-spray/hosts --become  cluster.yml --flush-cache
```

### Run post install recipe (inside container)
The `postinstall.yml` is responsible for creating a stable nginx reverse proxy configuration for publishing the
Kubernetes APIs (and other services provided by the bastion host or the Cloud Provider via the bastion host,
e. g. the object storage API).
```sh
cd /kubespray
ansible-playbook -i inventory/proseo-spray/hosts --become  postinstall.yml --flush-cache
```

Leave the container for the next step with `exit`.

This is probably the best point in the procedure to populate the `/etc/nginx/.htpasswd` file on the
bastion host, either by creating a new file or by copying the one saved in the first step.
- To create a new file (starting on your local control host):
  ```
  ssh linux@<bastion host>
  sudo htpasswd /etc/nginx/.htpasswd <new nginx user>
    # Enter and repeat password
  # Repeat for as many users as needed
  exit
  ```
- To copy the saved `.htpasswd` file (starting on your local control host):
  ```
  scp <path to file>/.htpasswd linux@<bastion host>:/home/linux/.htpasswd
  ssh linux@<bastion host>
  sudo mv .htpasswd /etc/nginx/
  exit
  ```

### Configure local kubectl for new Processing Facility (optional, but recommended)
The following steps are required to setup kubectl for controlling the new Processing Facility. They assume
that Nginx on the bastion host has been configured with access credentials for at least one user.
```
kubectl config set-cluster <cluster nickname> --server=<bastion host> --certificate-authority=path/to/certificate/authority
kubectl config set-credentials <user nickname> --username=<nginx username> --password=<nginx password>
kubectl config set-context <context nickname> --cluster=<cluster nickname> --user=<user nickname>
```

### Install k8s-dashboard (from bastion host)
To install the dashboard, perform the following steps on any machine, where kubectl is configured for the new
Processing Facility (local host, if the preceding step was performed, or log in to the bastion host with
`ssh linux@<bastion host>`):
```sh
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.0-beta5/aio/deploy/recommended.yaml
kubectl create serviceaccount k8s-poweruser
kubectl create clusterrolebinding k8s-poweruser-cadmin --clusterrole=cluster-admin --serviceaccount=default:k8s-poweruser
kubectl create clusterrolebinding k8s-poweruser-admin --clusterrole=admin --serviceaccount=default:k8s-poweruser
```
The dashboard URL will be 
`https://<bastion host name>/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/`

Get the dashboard login token (use last line from terminal output):
```
kubectl -n default describe secret $(kubectl -n default get secret | awk '/^k8s-poweruser-token-/{print $1}') | awk '$1=="token:"{print $2}'
```

### Create Kubernetes secret holding credentials for private registry
This secret is used when Kubernetes needs to pull images from some private registry. Perform the following on
any machine, where Docker is installed **and** kubectl is configured for the new Processing Facility (local host, if the preceding step
was performed, or one of the newly created Kubernetes nodes; the bastion host will not do, as it lacks Docker):
```sh
docker login <registry URL>
# Make sure Docker configuration file is under ~/.docker/config.json
cat ~/.docker/config.json
# Create Kubernetes secret
kubectl create secret generic regcred  --from-file=.dockerconfigjson=.docker/config.json --type=kubernetes.io/dockerconfigjson
```

