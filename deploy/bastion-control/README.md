prosEO Bastion Host for Control Instance
========================================

The bastion host is installed using Ansible.


# Prerequisites

In the `roles` directory, the following subdirectories need to be populated according to the `README.md` files
found in each of the directories:
- `configure_users/files`
- `install_kubectl/files`
- `configure_nginx_proxy/files`

In the `group_vars` directory, create a file `bastion_control.yml` with the administrative user and password and an
email address for alerts from rkhunter (see file `bastion_control.yml.template` for the required contents).


# Bastion host configuration

For an initial configuration of the bastion host, run the following command from the current directory:
```
ansible-playbook -i ../proseo-hosts -u root -b -v --private-key roles/configure_users/files/id_rsa bastion-control-init.yml
```

This changes the administrative access to the `proseoadmin` user. Use `-u proseoadmin` for all subsequent executions, because after
the first execution root access will be denied.

After the deployment of Kubernetes complete the bastion host configuration:
```
ansible-playbook -i ../proseo-hosts -u proseoadmin -b -v --private-key roles/configure_users/files/id_rsa bastion-control.yml
```
