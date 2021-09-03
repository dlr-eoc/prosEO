CProS NFS Server Configuration
==============================

The NFS Server provides shared storage to the prosEO Storage Manager and the Kubernetes worker nodes, whereby the Storage Manager
runs locally on this machine.

The server configuration boils down to just running:
```
ansible-playbook -i proseo-hosts -u root -b -v --private-key keys/id_rsa nfs-server.yml
```
