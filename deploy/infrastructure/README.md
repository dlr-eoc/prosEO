Infrastructure Deployment for prosEO
====================================


Note: This example deployment uses the Ansible deployment method for the IONOS Cloud Service.


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


# Manual configuration corrections

Currently the IONOS Cloud API does not provide fine-grained control of the IP addresses in the virtual data centre. Therefore
some manual adaptations have to be made:
- Set the public IP addresses of the bastion hosts (select those IP addresses from the public IP block `prosEO Data Center` –
  or whatever your IP block has been named –, which have not yet been assigned to the NAT Gateway),
- Set the internal LAN address of the NAT Gateway,
- Set the network address of `laninternal` in the NAT rule of the NAT Gateway.
- For all hosts and all NICs, activate DHCP by checking the respective box.

Apart from that, creating new volumes within a server does not work. Thus the NFS server disks need to be moved into the NFS
server manually.


# Gather IP addresses for further configuration

The IP addresses of the virtual machines must be extracted from the provisioned infrastructure and entered in a file
called `proseo-hosts` in the `deploy` directory (start from template file `proseo-hosts.template`).
