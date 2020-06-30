FROM ubuntu:20.04
LABEL maintainer="Jens Finkhaeuser <jens@finkhaeuser.de>"

# Programming languages
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get -qq update
RUN apt-get install -qqy --no-install-recommends \
      python3 \
      python3-pip \
      golang-go

# Setup Python environment
RUN pip3 install pipenv
COPY Pipfile .
RUN pipenv install

# Setup Go environment
COPY .env .
COPY gopackages.txt .
RUN source .env && go get $(cat gopackages.txt)

# Entry point
ENTRYPOINT pipenv shell

