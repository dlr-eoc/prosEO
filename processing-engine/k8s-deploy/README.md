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

In order to manage multiple clusters, cluster-specific configuration is added
to the `clusters` directory, in a subdirectory per cluster. All commands assume
that your working directory is one of those cluster directories.

We use kubespray to provision machines with a kubernetes installation.
Kubespray knows how to integrate with tools that set up VMs, such as
terraform. It also is capable of provisioning a cluster through a bastion
host. In fact, this is the setup we're going for here. You can safely
skip sections that do not apply to you.

Choosing a Cluster
------------------

Simply `$ cd clusters/<yourcluster>` and run commands from there.

Creating a new Cluster
----------------------

1. Make a cluster directory: `$ mkdir clusters/<yourcluster>`
1. Copy the autoenv template into this directory:
  `$ cp templates/autoenv clusters/<yourcluster>/.autoenv`
1. Make a config directory: `$ mkdir clusters/<yourcluster>/config`
1. Create a file: `clusters/<yourcluster>/config/clustername.sh`:
   ```bash
   export PROSEO_CLUSTER_NAME=foo
   ```
1. Change to the new cluster directory.

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
