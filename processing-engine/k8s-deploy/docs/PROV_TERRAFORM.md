Terraform VM Provisioning
=========================

Terraform is a CLI tool for provisioning and managing VMs, with support
for many cloud providers. For full documentation, see
[the terraform website](https://www.terraform.io/).

In this guide, we're concerned with using terraform such that the VMs
it provisions can be used by kubespray to set up our kubernetes cluster.

Since this reflects our environment, we're using an OpenStack provider.
For using any other provider, you should consult the terraform documentation.

OpenStack Configuration
-----------------------

You will need some environment variables to be defined for terraform to do
its job. You can see which environment variables are required by

1. Link the OpenStack hosts script.
   ```bash
   $ ln -s ../../contrib/terraform/openstack/hosts
   ```
1. Copy the OpenStack template to the autoenv configuration:
   ```bash
   $ cp ../../templates/terraform_ostack.template config/terraform_ostack.sh
   ```
1. Modify the configuration to include your username, password, etc.
1. Create your `cluster.tfvars` configuration file. This is outside of the scope
   of this guide.
1. Optionally upgrade kubespray's openstack configuration. You will know if
   you need to do this when the next step fails. In fact, the next step will
   output the *exact* commands to use, so treat the following as an example
   only:
   ```bash
   $ echo yes | terraform 0.13upgrade ../../kubespray/contrib/terraform/openstack
   ```
1. Using kubespray's openstack configuration combined with the above
   configuration file, run:
   ```bash
   $ terraform init -var-file=cluster.tfvars ../../kubespray/contrib/terraform/openstack
   ```
1. You now have a `.terraform` directory that holds terraform's plugins required
   for this cluster. Now apply this configuration:
   ```bash
   $ terraform apply -var-file=cluster.tfvars ../../kubespray/contrib/terraform/openstack
   ```
1. Check your configuration with:
   ```bash
   $ ./hosts --hostfile
   ```
