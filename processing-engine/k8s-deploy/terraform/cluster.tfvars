# your Kubernetes cluster name here
cluster_name = "proseo-integration"

# list of availability zones available in your OpenStack cluster
az_list=["eu-de-01", "eu-de-02"]
dns_nameservers=["100.125.4.25", "8.8.8.8"]

# SSH key to use for access to nodes
public_key_path = "~/.ssh/id_rsa.pub"

# image to use for bastion, masters, standalone etcd instances, and nodes
image = "Standard_CentOS_7_latest"

# user on the node (ex. core on Container Linux, ubuntu on Ubuntu, etc.)
ssh_user = "linux"

# 0|1 bastion nodes
number_of_bastions = 1
flavor_bastion = "s2.medium.1"

# standalone etcds
number_of_etcd = 0

wait_for_floatingip = "true"

# masters
number_of_k8s_masters = 0
number_of_k8s_masters_no_etcd = 0
number_of_k8s_masters_no_floating_ip = 1
number_of_k8s_masters_no_floating_ip_no_etcd = 0
flavor_k8s_master = "s2.xlarge.4"
master_root_volume_size_in_gb = 40

# nodes
number_of_k8s_nodes = 0
number_of_k8s_nodes_no_floating_ip = 1
node_root_volume_size_in_gb = 40
node_data_volume_size_in_gb = 400
node_docker_volume_size_in_gb = 400
flavor_k8s_node = "s2.xlarge.4"


# networking
network_name = "proseo-integration-internal-network"
external_net = ""
subnet_cidr = "192.168.0.0/24"
floatingip_pool = "admin_external_net"
bastion_allowed_remote_ips = ["0.0.0.0/0"]
k8s_allowed_remote_ips = ["0.0.0.0/0"]
