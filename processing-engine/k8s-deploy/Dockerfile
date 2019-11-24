FROM centos:7

MAINTAINER Hubert Asamer <hubert.asamer@dlr.de>

ARG KUBESPRAY_REVISION=471589f1
ARG TERRAFORM_RELEASE=0.12.13
ARG KUBECTL_RELEASE=v1.16.2
ARG INVENTORY_NAME=proseo-spray

COPY kubespray/kubespray-proseo.patch /patch/kubespray-proseo.patch

RUN yum -y install epel-release ansible wget curl git unzip \ 
  openssh-server jq ca-certificates gnupg2 python3-pip rsync \
  && wget https://releases.hashicorp.com/terraform/${TERRAFORM_RELEASE}/terraform_${TERRAFORM_RELEASE}_linux_amd64.zip \
  && unzip terraform_${TERRAFORM_RELEASE}_linux_amd64.zip \
  && mv terraform /usr/local/bin/ \
  && rm terraform_${TERRAFORM_RELEASE}_linux_amd64.zip \
  && git clone https://github.com/kubernetes-sigs/kubespray.git /kubespray \
  && cd /kubespray \
  && git checkout ${KUBESPRAY_REVISION} \
  && git apply /patch/kubespray-proseo.patch \
  && /usr/bin/python3 -m pip install pip -U \ 
  && /usr/bin/python3 -m pip install -r requirements.txt \
  && curl -LO https://storage.googleapis.com/kubernetes-release/release/${KUBECTL_RELEASE}/bin/linux/amd64/kubectl \
  && chmod a+x kubectl \ 
  && mv kubectl /usr/local/bin/kubectl \
  && cp -rfp inventory/sample inventory/${INVENTORY_NAME} \
  && cd inventory/${INVENTORY_NAME} \
  && ln -s ../../contrib/terraform/openstack/hosts \
  && ln -s ../../contrib

WORKDIR /kubespray/inventory/${INVENTORY_NAME}

COPY terraform/cluster.tfvars /kubespray/inventory/${INVENTORY_NAME}/cluster.tfvars
COPY kubespray/postinstall.yml /kubespray/postinstall.yml
COPY ssh-keys/cluster-key.pem /root/.ssh/id_rsa
COPY ssh-keys/cluster-key.pub /root/.ssh/id_rsa.pub
COPY terraform/.ostackrc /root/.bashrc
RUN terraform init -var-file=cluster.tfvars ../../contrib/terraform/openstack
RUN sed -i 's/#AllowTcpForwarding yes/AllowTcpForwarding yes/' /etc/ssh/sshd_config
RUN ln -s /terraform/state/terraform.tfstate terraform.tfstate



