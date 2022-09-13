prosEO Control Instance ("Brain") Configuration
===============================================

# Ansible configuration

The Ansible configuration boils down to just running:
```
ansible-playbook -i ../proseo-hosts -u root -b -v --private-key keys/id_rsa brain.yml
```
