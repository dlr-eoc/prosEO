FROM centos:7.6.1810
MAINTAINER Thomas Bassler <thomas.bassler@drbassler.de>

RUN yum --assumeyes install java-11-openjdk.x86_64

#ADD target/lib /usr/share/proseo-ui-cli/lib
ADD target/proseo-ui-cli.jar /usr/share/proseo-ui-cli/proseo-ui-cli.jar
ADD src/main/resources/proseo /usr/bin/proseo