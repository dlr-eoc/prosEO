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

Simply `$ cd kubespray/inventory/<yourcluster>` and run commands from there.

Creating a new Cluster
----------------------

1. Make a cluster directory. This assumes you are currently in the `k8s-deploy`
   directory:
   ```bash
   $ scripts/make_cluster.sh <clustername>
   ```
   The script copies files from kubespray's `kubespray/inventory/sample`
   cluster to `kubespray/inventory/<clustername>` and adds a few
   configuration files.
1. Change to the new cluster directory.

TODO Terraform provisioning needs to be done first??

1. Create SSH keys for this cluster (within the deployment container, directory `kubespray/inventory/<clustername>`):
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

All your cluster configuration lives in this directory. You can copy
it anywhere, and adjust the relative paths to scripts in further
steps accordingly, e.g.  for managing the cluster configuration in a separate
repository.

It would also be possible to create a symbolic link from
`kubespray/inventory/<clustername>` to this repository - however, there
are some issues with using the docker environment. The
[Initial Setup](docs/SETUP.md) guide has some details here.

In either case, the guide assumes that commands are run from your cluster
directory, and that your cluster directory lives in the path that
`make_cluster.sh` creates.

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
# In cluster directory
$ ansible-playbook -i hosts --private-key ssh-keys/cluster-key ../../../scripts/ansible/bastion-preinstall.yml
```

Further to that copy the SSH key files in `ssh-keys/cluster-key*` to the `.ssh` directory of the `linux` user
on the bastion host to facilitate automated SSH logins to the cluster nodes via the bastion host.

### K8S Deployment

Once the VMs are fully provisioned, and optional bastion hosts are configured,
ansible should be able to connect to all hosts in the cluster. You can find out
whether this works by running:

```bash
# In cluster directory
$ ansible -i hosts -m ping all
```
If the connection cannot be established at the first attempt, check the `group_vars/no_floating.yml` file (e. g. correct
bastion host IP?). For an installation with a bastion host it should look something like:

```
ansible_ssh_common_args: "-o ProxyCommand='ssh -i ssh-keys/cluster-key -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -W %h:%p -q linux@<bastion IP>' -i ssh-keys/cluster-key -o StrictHostKeyChecking=no"
```

We need to run some configuration on all the nodes first. This is a little
specific to the operating system you're running on.

```bash
# In cluster directory
$ ansible-playbook -i hosts ../../../scripts/ansible/cluster-preinstall.yml
```

Now it's up to kubespray to configure all VMs for running a K8S cluster. We
need to run commands from the kubespray directory now, and specify the cluster
inventory.

```
# In cluster directory
$ ansible-playbook -i hosts --become ../../cluster.yml --flush-cache
```

*Note:* It may be that the worker nodes cannot join to the cluster with an
error message stating the kubeadm configuration file version is outdated. If
that happens, there's only one thing to do: update the configuration. From the
cluster directory, run:

```bash
# In cluster directory
$ ansible-playbook -i hosts ../../../scripts/ansible/migrate-kubeadm-config.yml
```

Then re-start the kubespray playbook.

Congratulations, you have a kubernetes cluster! You now should also configure
the bastion host to forward traffic, etc:

*Note:* This last step requires certificate files to be installed in the
cluster directory's `certs/servercert.pem` and `certs/privkey.pem` respectively.
- For creating self-signed certificates, use [a guide such as this](https://www.cyberithub.com/create-a-self-signed-certificate/)
- For using ACME/Let's Encrypt certificates, [dehydrated](https://github.com/dehydrated-io/dehydrated)
  is recommended.
  
Manual procedure to create a Let's Encrypt certificate using Certbot (on the bastion host):
```
# Prerequisite: Install snapd
sudo yum -y install snapd
sudo systemctl enable --now snapd.socket
sudo ln -s /var/lib/snapd/snap /snap
# Log out and back in again to set snapd's paths right, wait a few seconds
sudo snap install core
sudo snap refresh core

# Make sure certbot is not installed as a CentOS package
sudo yum list certbot
# IF it is installed:
sudo yum remove certbot
# END IF

# Install certbot as snap package
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/bin/certbot

# Get and install certificates
sudo certbot --nginx

# Test/check automatic renewal
sudo certbot renew --dry-run
systemctl list-timers
```

*Note 2:* You probably also want to configure users that can access the API
exposed by the bastion configuration. Since it's not a great idea to store
plaintext passwords here, we'll leave that up to you. You need to pass to
ansible the `nginx_users` dict, where keys are user names and values are
plaintext passwords.

One option would be to create a file `kubespray/inventory/<cluster>/group_vars/bastion/nginx.yml`:

```yaml
# <cluster directory>/group_vars/bastion/nginx.yml
---
nginx_users
  foo: bar  # creates user foo with password bar
```

Without that step, the API proxy will be inaccessible by anyone.

```bash
# In cluster directory
$ ansible-playbook -i hosts ../../../scripts/ansible/postinstall.yml
```

Updating a Cluster
------------------

1. Use your provisioner to create new machines. This should result in a new
   inventory for kubespray.
1. Change to your cluster directory, if you haven't already.
1. Update your SSH config - this gets regenerated when you modified your
   inventory.
   ```bash
   # In cluster directory
   $ ../../../scripts/ssh_config.sh
   ```
1. Re-run the `bastion-preinstall.yml` or `cluster-preinstall.yml` scripts as
   needed, depending on whether you added/modified bastions or other nodes or
   both (see above).
1. Go to the kubespray directory, and run the `cluster.yml` script again.
   ```bash
   # In /kubespray directory
   $ ansible-playbook -i inventory/<clustername>/hosts --become cluster.yml --flush-cache
   ```

*Note:* The `upgrade-cluster.yml` script can be used to bring kubernetes to
the latest version on already existing clusters. Do not use it to grow a
cluster.

