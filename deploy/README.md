prosEO Deployment Example
=========================


# Prerequisites

The procedure below assumes that a cloud infrastructure has been deployed according to the details given in the
`infrastructure` subdirectory.


# General Procedure

The deployment of a full prosEO environment requires the following steps:
1. Create images for the prosEO microservices (see `proseo-images/REAMDE.md`).
2. Configure the bastion host for the prosEO Control Instance ("brain", see `bastion-control/README.md`).
3. Configure the Kubernetes cluster (the "hands", see `hands/README.md`).
4. Re-run the configuration for the bastion host after adding the file `kubectl.conf` in `bastion-control/roles/install_kubectl/files`.
5. Configure the prosEO Control Instance (the "brain", see `brain/README.md`).
6. Configure the bastion host for the prosEO PRIP (see `bastion-prip.REAMDE.md`).
7. Configure the NFS server (see `nfs-server/README.md`).
8. Configure the logging and monitoring host (see `loghost/README.md`).
9. Start the brain (see below).
10. Start the Storage Manager (see below).
11. Start the InfluxDB and the Grafana server (TBC, maybe already done with deployment).

Note that the Kubernetes cluster is started as part of the deployment (step 3).

The dependencies are:
(3), (4), (6), (7) depend on (2)
(8) depends on (1) and (4)
(9) depends on (1) and (6)

Administrative access usually is through the bastion host for the control instance, since all inner nodes are reachable from there.


# Starting the prosEO Control Instance ("brain")

Log in to the brain host via the control instance bastion host, then create the containers for the prosEO microservices:
```
cd /opt/proseo
export PGADMIN_EMAIL=some.custom@email.address
export PGADMIN_PASSWORD=some-pw
./run_control_instance.sh
```

__First start:__ After the first start, the view for products with product files available on any Processing Facility
needs to be generated. For this, open a shell in the database container:
```
docker exec -it proseo_proseo-db_1 /bin/bash
```
Within this shell, run the provided SQL script:
```
psql proseo -U postgres -h localhost <create_view_product_processing_facilities.sql
```
You will be asked to provide the password for the `postgres` user. Enter the password configured in the `docker-compose.yml` file
in `brain/prepare_proseo/files`.

The control instance can be stopped using the script `stop_control_instance.sh`. To stop only the prosEO microservices, but not
the database (e. g. for maintenance work on the database) use the script `stop_brain.sh`.


# Starting the logging and monitoring

Starting the logging and monitoring consists of two tasks:
- Start the applications (in Docker)
- Create the required buckets in the InfluxDB


## Start the logging and monitoring applications

Log in to the loghost via the control instance bastion host, then create the containers for InfluxDB, Grafana and Telegraf:
```
cd /opt/proseo
docker-compose -p cpros up -d
```

## Configure the monitoring database (only once!)

```
  docker exec cpros_influxdb_1 influx bucket create -n operation -o proseo
  docker exec cpros_influxdb_1 influx bucket create -n order -o proseo
  docker exec cpros_influxdb_1 influx bucket create -n production -o proseo

  docker exec cpros_influxdb_1 influx user create --name example-username --password ExAmPl3PA55W0rD
```
