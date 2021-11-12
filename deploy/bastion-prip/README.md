CProS Bastion Host for Control Instance
=======================================

The bastion host is installed using Ansible.


# Prerequisites

In the `roles` directory, the following subdirectories need to be populated according to the `README.md` files
found in each of the directories:
- `configure_users/files`
- `configure_nginx_proxy/files`

In the `group_vars` directory, create a file `bastion.yml` with the administrative user and password (see file
`bastion.yml.template` for the required contents).


# Bastion host configuration

To configure the bastion host, run the following command from the current directory:
```
ansible-playbook -i ../proseo-hosts -u root -b -v --private-key roles/configure_users/files/id_rsa bastion-prip.yml
```
(Use `-u proseoadmin` for subsequent executions, because after the first execution root access will be denied.)
