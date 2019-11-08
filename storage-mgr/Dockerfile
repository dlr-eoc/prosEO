FROM openjdk:11

MAINTAINER Hubert Asamer <hubert.asamer@dlr.de>

ADD target/proseo-storage-mgr.jar /usr/share/proseo-storage-mgr/proseo-storage-mgr.jar
WORKDIR /usr/share/proseo-storage-mgr

ENTRYPOINT ["java", "-jar", "/usr/share/proseo-storage-mgr/proseo-storage-mgr.jar"]