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
COPY tools/kubectl_release.txt ./tools/
RUN export KUBECTL_RELEASE=$(cat ./tools/kubectl_release.txt) && \
      mkdir bin && \
      cd bin && \
      curl -LO https://storage.googleapis.com/kubernetes-release/release/${KUBECTL_RELEASE}/bin/linux/amd64/kubectl && \
      chmod +x kubectl

# Entry point
ENV PIPENV_SHELL=/bin/bash
ENV PIPENV_SHELL_FANCY=true
ENTRYPOINT pipenv shell
