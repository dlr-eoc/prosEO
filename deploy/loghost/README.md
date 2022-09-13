prosEO Logging and Monitoring Server Configuration
==================================================

This server provides the performance monitor and Grafana server required for the logging and monitoring of a prosEO production
environment.

# Prepare configuration

In the directory `prepare_monitoring/files` copy `docker-compose.yml.template` into `docker-compose.yml`.


# Deploy configuration

The server configuration boils down to just running:
```
ansible-playbook -i ../proseo-hosts -u root -b -v --private-key keys/id_rsa loghost.yml
```
