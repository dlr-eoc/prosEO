FROM centos:7
LABEL maintainer="Jens Finkhaeuser <jens@finkhaeuser.de>"

# Build args
ARG WORK_DIR=/prosEO
WORKDIR ${WORK_DIR}

# Initial setup
RUN yum -y install epel-release
RUN yum -y update

ENV LC_ALL=en_US.utf8
ENV LANG=en_US.utf8


# Programming languages
RUN yum -y install \
      python3 \
      python3-pip \
      golang
      
# Helpers
RUN yum -y install openssh openssh-clients which

# Setup Python environment
RUN pip3 install pipenv
COPY Pipfile .
RUN pipenv install --three

# Setup Go environment
COPY .env .
RUN ls -la .
COPY tools/gopackages.txt ./tools/
RUN source $PWD/.env && \
      GOPATH=$GOPATH go get $(cat tools/gopackages.txt)

# Install kubespray requirements. Note that the requirements may change if
# this image is older than the kubespray path mapped at run-time.
COPY kubespray/requirements.txt ./tools/
RUN pipenv install -r ./tools/requirements.txt

# Install kubectl
COPY kubespray/roles/download/defaults/main.yml ./tools/
RUN export KUBE_VERSION=$(grep '^kube_version:' ./tools/main.yml | cut -d' ' -f2) && \
      mkdir bin && \
      cd bin && \
      curl -LO https://storage.googleapis.com/kubernetes-release/release/${KUBE_VERSION}/bin/linux/amd64/kubectl && \
      chmod +x kubectl

# Setup PATH
ENV PATH=${WORK_DIR}/bin:${WORK_DIR}/scripts:${PATH}

# Entry point
ENV PIPENV_SHELL=/bin/bash
ENV PIPENV_SHELL_FANCY=true
ENTRYPOINT pipenv shell
