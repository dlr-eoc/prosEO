prosEO Logging and Monitoring Server Configuration
==================================================

This server provides the performance monitor and Grafana server required for the logging and monitoring of a prosEO production
environment.

# Prepare configuration

In the directory `prepare_monitoring/files` copy `docker-compose.yml.template` into `docker-compose.yml`. Then replace the following placeholder:
- `<password from bastion nginx config>`: Copy the password of the bastion host user for Grafana from `bastion-control/group-vars/bastion_control.yml`

Under prepare_monitoring/files, adapt alerting.sh to the project's needs. Most importantly, the e-mail address and the exclusions need to be adapted.
Adapt the postfix domain and host under group:vars/loghost.yaml

Note that Grafana is pre-configured so that the native alerting system can be used as well.

# Deploy configuration

The server configuration boils down to just running:
```
ansible-playbook -i ../proseo-hosts -u root -b -v --private-key keys/id_rsa loghost.yml
```
