CProS Logging and Monitoring Server Configuration
=================================================

This server provides the InfluxDB and Grafana servers required for the logging and monitoring of the Sentinel-1B Production Service.

# Prepare configuration

In the directory `prepare_monitoring/files` copy `docker-compose.yml.template` into `docker-compose.yml` and `telegraf.conf.template`
into `telegraf.conf`. Then replace the following placeholders:
- `<my_influxdb_token>`: The authentication token for the InfluxDB as defined during prosEO image creation (both files)
- `<password from bastion nginx config>`: Copy the password of the bastion host user `proseoadmin` for the InfluxDB and Grafana

Furthermore, ensure that in `telegraf.conf` the `endpoint` variable points to the correct Docker instance.


# Deploy configuration

The server configuration boils down to just running:
```
ansible-playbook -i ../proseo-hosts -u root -b -v --private-key keys/id_rsa loghost.yml
```
