FROM openjdk:11

MAINTAINER Hubert Asamer <hubert.asamer@dlr.de>

ADD target/proseo-storage-mgr.jar /usr/share/proseo-storage-mgr/proseo-storage-mgr.jar
WORKDIR /usr/share/proseo-storage-mgr

#ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n

ENTRYPOINT ["java", "-jar", "/usr/share/proseo-storage-mgr/proseo-storage-mgr.jar"]