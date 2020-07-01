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

1. Copy the OpenStack template to the autoenv configuration:
   `$ cp ../../templates/terraform_ostack.template config/terraform_ostack.sh`
1. Modify the configuration to include your username, password, etc.


