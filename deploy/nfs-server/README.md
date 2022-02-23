prosEO NFS Server Configuration
===============================

The NFS Server provides shared storage to the prosEO Storage Manager and the Kubernetes worker nodes, whereby the Storage Manager
runs locally on this machine.

# Configure NFS server parameters

In the directory `group_vars` create a file `nfsserver.yml` from the provided template file `nfsserver.yml.template` and
fill in the following parameters:
- Actual range of virtual disks available
- Individual disk size and total size
- Subnet address for NFS clients

# Run server configuration

The server configuration boils down to just running:
```
ansible-playbook -i ../proseo-hosts -u root -b -v --private-key keys/id_rsa nfs-server.yml
```
