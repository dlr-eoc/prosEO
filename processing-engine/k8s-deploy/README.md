Kubernetes Deployment of proSEO
===============================

There are two ways you can set up for your k8s deployment: directly on a
UNIX-like operating system, or in a Docker container.

The design rationale either way is to have the most up-to-date tooling
available, without keeping it in the repository. At the same time,
configuration data must be kept in the repository.

The exception to this up-to-date tooling is `kubectl` - that we want in the
version we're deploying kubernetes in.

See [Initial Setup](docs/SETUP.md) for initial setup instructions. You may need
to re-run these if you upgrade the tooling, but for merely deploying a cluster,
it is not necessary.

Entering a Deployment Environment
---------------------------------

As outlined in the setup document, you can enter your deployment environment
either by running `$ pipenv shell`, or by running `$ docker/run.sh`.

Either deployment environment is not bound to a particular cluster
configuration; you can administer multiple different clusters with them. They
are, however, bound to a particular version of the tooling.

Cluster Management
==================

We use kubespray to provision machines with a kubernetes installation.
Kubespray is fine with managing multiple clusters. Each cluster configuration
lives in its own subdirectory in `kubespray/inventory`, e.g.
`kubespray/inventory/foo`.

All detail guides assume that this is your working directory.

Kubespray knows how to integrate with tools that set up VMs, such as
terraform. It also is capable of provisioning a cluster through a bastion
host. In fact, this is the setup we're going for here. You can safely
skip sections that do not apply to you.

Choosing a Cluster
------------------

Simply `$ cd kybespray/inventory/<yourcluster>` and run commands from there.

Creating a new Cluster
----------------------

1. Make a cluster directory:
   ```bash
   $ make_cluster.sh <clustername>
   ```
1. Change to the new cluster directory.
1. Create SSH keys for this cluster:
   ```bash
   $ ../../../scripts/ssh_config.sh
   ```
1. TODO: the previous step could be done as part of the `make_cluster.sh` script.

**Notes:**
- The `.autoenv` file gets executed whenever you enter the directory it
  lives in. The file reads bash configurations from `config/*.sh`, and
  sets up a bash prompt.
- We make use of this automatic configuration loading; when switching
  between clusters, this helps re-set the environment of the cluster
  we're managing.
- The `clustername.sh` file is entirely optional, but it sets the bash
  prompt, indicating which cluster's environment is currently loaded.

### VM Provisioning

How you would set up machines is specific to the hosting provider you're
using. We can collect guides here, though for now there is only one for
terraform.

- [Terraform VM Provisioning](docs/PROV_TERRAFORM.md)

It's best to re-run the SSH configuration script after this step is done.
It will configure the deploy environment with regards to SSH settings.

```bash
$ ../../../scripts/ssh_config.sh
```

Amongst other things, this will create a configuration for the `ssh` command
and an alias that uses this configuration that should allow you to SSH into
any of the provisioned VMs, so that the below should work:

```bash
$ ./hosts --hostfile
# Pick any hostname/IP
$ ssh <hostname|ip>
```

If you have bastion hosts, you need to run the following section first.

#### Bastion Hosts

If your configuration includes bastion hosts, then after the VMs are
provisioned, you need to enable SSH forwarding on those hosts. SSH into
the bastion:

```bash
(bastion) $ sudo sed -i 's/^#* *AllowTcpForwarding.*/AllowTcpForwarding yes/g' /etc/ssh/sshd_config
```

The file `/etc/ssh/sshd_config` should contain the line `AllowTcpForwarding yes`.
If not, edit it manually. Then:

```bash
(bastion) $ sudo systemctl restart sshd
```

### K8S Deployment

Once the VMs are fully provisioned, and optional bastion hosts are configured,
ansible should be able to connect to all hosts in the cluster. You can find out
whether this works by running:

```bash
$ ansible -i hosts -m ping all
```

It's best to download some K8S components from the prosEO registry:

1. Don't validate certs:
   ```bash
   $ sed -i 's/^#* *download_validate_certs:.*/download_validate_certs: False/g' group_vars/all/all.yml
   ```
1. Add download URLs for packages:
   ```bash
   $ cat >group_vars/all/proseo.yml <<EOF
# Use prosEO registry for downloads
etcd_download_url: "https://proseo-registry.eoc.dlr.de/artifactory/proseo/etcd-v3.3.10-linux-amd64.tar.gz"
cni_download_url: "https://proseo-registry.eoc.dlr.de/artifactory/proseo/cni-plugins-linux-amd64-v0.8.1.tgz"
calicoctl_download_url: "https://proseo-registry.eoc.dlr.de/artifactory/proseo/calicoctl-linux-amd64"
crictl_download_url: "https://proseo-registry.eoc.dlr.de/artifactory/proseo/critest-v1.16.1-linux-amd64.tar.gz"
EOF
   ```
1. TODO: these could be done as part of the `make_cluster.sh` script.

Now it's up to kubespray to configure all VMs for running a K8S cluster. We
need to run commands from the kubespray directory now, and specify the cluster
inventory.

```
$ cd ../..  # /kubespray directory
$ ansible-playbook -i inventory/<clustername>/hosts --become cluster.yml --flush-cache
```
