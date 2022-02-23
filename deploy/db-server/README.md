prosEO Database Server ("db-server") Configuration
==================================================

The configuration of the prosEO Database Server consists of two parts:
- Prepare the VM (Ansible)
- Deploy PostgreSQL on the VM (Docker Compose)

# Deployment preparation

Before deploying the database server, the database password defined during the creation of the prosEO images must be configured.
To this end, copy the file `prepare_proseo/docker-compose.yml.template` into `ansible/prepare_proseo/docker-compose.yml`
and enter the password in the line beginning with `- POSTGRES_PASSWORD=`.


# Ansible configuration

The Ansible configuration boils down to just running:
```
ansible-playbook -i ../proseo-hosts -u root -b -v --private-key keys/id_rsa db-server.yml
```
