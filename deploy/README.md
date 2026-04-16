prosEO Deployment Example
=========================


This document describes how to deploy a prosEO processing facility on a new set of
(virtual) machines, e. g. as provisioned by some cloud service provider.

# Prerequisites

The (virtual) machines need to at least fulfil the following requirements:
- Linux installed
- Ability to run Docker and Kubernetes (i. e. standard CentOS kernel, no provider-specific builds)

Suggested sizing for a small to medium installation:
- CPUs: Bastion host and Kubernetes master 4 CPUs each, worker nodes 8-16 CPUs each (depending on the processors to be run)
- RAM: Bastion host and Kubernetes master 8-16 GB each, worker nodes 32 GB or more (again depending on the processors)
- Disk: Bastion host 40 GB system; NFS server 10 TB data (or more, we are talking earth observation satellite data here!);
  Kubernetes master 40 GB system; worker nodes 40 GB system, 80 GB Docker library and scratch working area (depening on processor needs)
- Optional: Access to some object storage conforming to the AWS S3 protocol (if not, more disk space will be needed for the NFS server)

The procedure below assumes that a cloud infrastructure has been deployed according to the details given in the
`infrastructure` subdirectory. The IP addresses of the virtual machines must be recorded in a file called `proseo-hosts`
(derived from template file `proseo-hosts.template`).


# General Procedure

The deployment of a full prosEO environment requires the following steps:
1. Create images for the prosEO microservices (see `proseo-images/REAMDE.md`).
2. Configure the bastion host for the prosEO Control Instance (see `bastion-control/README.md`).
3. Configure the Kubernetes cluster (the "hands", see `hands/README.md`).
4. Re-run the configuration for the bastion host after adding the file `/root/.kube/config` found on the Kubernetes master node
   as `kubectl.conf` in `bastion-control/roles/install_kubectl/files`, replacing the IP address `127.0.0.1` by the IP address 
   of the master node.
5. Configure the database server (the "brain", see `db-server/README.md`).
6. Configure the prosEO Control Instance (the "brain", see `brain/README.md`).
7. Configure the NFS server (see `nfs-server/README.md`).
8. Configure the logging and monitoring host (see `loghost/README.md`).
9. Configure the bastion host for the prosEO PRIP (see `bastion-prip/REAMDE.md`).
10. Start the database server (see below).
11. Start the brain (see below).
12. Start the Storage Manager (see below).
13. Start the monitoring services and the Grafana server on the loghost (TBC, maybe already done with deployment).

Note that the Kubernetes cluster is started as part of the deployment (step 3).

The dependencies are:
(3), (5), (7), (8) depend on (2)
(4) depends on (3)
(6) depends on (5)
(10) depends on (1) and (5)
(11) depends on (1) and (6)
(12) depends on (1) and (7)
(13) depends on (1) and (8)

Administrative access usually is through the bastion host for the control instance, since all inner nodes are reachable from there.


# Starting the prosEO Database Server

Log in to the db-server host via the control instance bastion host, then create the container for the prosEO metadata database:
```
cd /opt/prosEO
./run_db.sh <private Docker registry> <version>
```


# Starting the prosEO Control Instance ("brain")

Log in to the brain host via the control instance bastion host, then create the containers for the prosEO microservices:
```
cd /opt/prosEO
export PGADMIN_EMAIL=some.custom@email.address
export PGADMIN_PASSWORD=some-pw
./run_control_instance.sh <private Docker registry> <version>
```

__First start:__ After the first start of the brain, the views for products with product files available on any Processing Facility
and for monitoring need to be generated. For this, return to the database server and open a shell in the database container:
```
docker exec -it proseo_proseo-db_1 /bin/bash -c 'su - postgres'
```
Within this shell, run the provided SQL scripts:
```
psql proseo -U postgres -h localhost </proseo/create_view_product_processing_facilities.sql
psql proseo -U postgres -h localhost </proseo/populate_mon_service_state.sql
```
You may be asked to provide the password for the `postgres` user. Enter the password configured in the `docker-compose.yml` file
in `brain/prepare_proseo/files`.

The control instance can be stopped using the script `stop_control_instance.sh`.


# Starting the prosEO Storage Manager

Log in to the NFS server host via the control instance bastion host, then create the containers for the prosEO microservices:
```
cd /opt/prosEO
./run_storage_mgr.sh <private Docker registry> <version>
```


# Starting the logging and monitoring

Start the Docker applications. Convenience scripts (run_containers.sh, stop_containers.sh) are available on the loghost under /opt/prosEO.


## Configure Grafana

Connect to the Postgres container and switch to the prosEO database:
```
docker exec -it proseo_proseo-db_1 su - postgres
psql
\c proseo
```

Create a dedicated read-only user named "grafana", replacing "password" with a secure password for the new user 
(to be stored for example in deploy/loghost/prepare-monitoring/files/grafana.cred):
```
CREATE USER grafana WITH PASSWORD 'password';
GRANT CONNECT ON DATABASE proseo TO grafana;
GRANT USAGE ON SCHEMA public TO grafana;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO grafana;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO grafana;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO grafana;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO grafana;
ALTER ROLE grafana SET search_path TO public;
```
**Note:** It may be more secure to limit access to specific tables.

Go to https://<your.bastion.host>/proseo/grafana. The Grafana datasources and dashboards are configured
automatically. However, to properly initialize the prosEO database, it must be selected in the datasource
view and saved and tested with the available "save & test" button. Only then will the dashboards be displayed correctly.

An overview over recent errors is available at https://<your.bastion.host>/proseo/grafana/explore?orgId=1&left={"datasource":"Loki","queries":[{"expr":"{job=\"proseo\"} |= \"ERROR\""}]}.
