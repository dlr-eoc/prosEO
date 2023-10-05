FROM openjdk:11
MAINTAINER Thomas Bassler <thomas.bassler@drbassler.de>
ENTRYPOINT ["java", "-jar", "/usr/share/proseo-archive-mgr/proseo-archive-mgr.jar"]
#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=host.docker.internal:8124", "-jar", "/usr/share/proseo-archive-mgr/proseo-archive-mgr.jar"]

ADD target/proseo-archive-mgr.jar /usr/share/proseo-archive-mgr/proseo-archive-mgr.jar