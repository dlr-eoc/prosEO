cluster ssh-keys
================

## why?
required by terraform & kubespray to provision k8s-software on VMs. The public key is used to create a key-pair within the cloud-provider's IAM.

## how?
```sh
ssh-keygen
cp ~/.ssh/id_rsa ./cluster-key.pem
cp ~/.ssh/id_rsa.pub ./cluster-key.pub
```


