Infrastructure Deployment for prosEO
====================================


Note: This example deployment uses the Ansible deployment method for the IONOS Cloud Service. An example for an OpenStack
Terraform deployment will be added later.


# Prerequisites

For a deployment in the IONOS Cloud using Ansible, a Python module called `ionoscloud` and the IONOS Ansible module are needed
(see https://docs.ionos.com/ansible/). In a suitable location on your deployment host (not here!), perform the following steps:
```
pip install ionoscloud
git clone https://github.com/ionos-cloud/module-ansible
```


# Configure Ansible files

The following files need to be customized (a template is provided for each with a filename ending in `.template`):
- `group_vars/all.yml`: Set the desired infrastructure names, sizes etc.

Note that the customized files are included in `.gitignore` by default and will not be included in the Git directory.


# Deploy the infrastructure

The infrastructure can be deployed using the following commands:

```
export ANSIBLE_LIBRARY=</path/to/ionos/module-ansible>
export IONOS_USERNAME=<IONOS DCD user>
export IONOS_PASSWORD=<IONOS DCD password>
ansible-playbook proseo-infrastructure.yml
```

Note: We use the environment variables for username and password to avoid having to specify the `username` and `password`
parameters with every single deploymente task.