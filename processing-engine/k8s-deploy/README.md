prosEO-k8s-gateway setup
========================
ansible playbook to configure a public k8s-gateway

### prerequisites
- running, managed k8s-cluster (OTC-CCE)
- a valid kubectl config file (download from OTC-web console)
- a running bare centos-VM in the same VPC & subnet like the k8s cluster
- a valid domainname with A-record pointing to the public IP of the gateway-VM (`proseo-k8s-gate.de`)
- a working ssh-key for VM-access
- an ansible control-host

### configure ansible
- edit file ansible-conf/hosts and set public IP and path to ssh-key
- place kubectl-config file to `vars/kube.config`

### run ansible playbook

```sh
./run-playbook.sh k8s-gateway.yml
```

### get letsencrypt ssl-cert
- ssh into VM: ssh -i ~/ssh-key.pem linux@proseo-k8s-gate.de
- sudo certbot certonly --nginx
- restart nginx

### k8s-access from your localhost
- install kubectl
- copy kubectl-config file to .kube/config
- change property `server` to https://proseo-k8s-gate.de/otc-proseo01
- run "kubectl config use-context internal"
- test it with: kubectl cluster-info

### install k8s-dashboard

```sh
# install it...
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.0-beta4/aio/deploy/recommended.yaml
# dashboard-url will be: 
# https://proseo-k8s-gate.de/otc-proseo01/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
# get dashboard login-token (use last line from stdout):
kubectl -n kube-system describe secrets \
   `kubectl -n kube-system get secrets | awk '/clusterrole-aggregation-controller/ {print $1}'` \
       | awk '/token:/ {print $2}'
```

