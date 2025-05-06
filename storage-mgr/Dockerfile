FROM openjdk:11

ADD target/proseo-storage-mgr.jar /usr/share/proseo-storage-mgr/proseo-storage-mgr.jar
WORKDIR /usr/share/proseo-storage-mgr

#ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n

#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=192.168.20.155:8000", "-jar", "/usr/share/proseo-storage-mgr/proseo-storage-mgr.jar"]
#ENTRYPOINT ["java", \
#"-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=192.168.20.155:8000", \
#"-Dcom.sun.management.jmxremote", \
#"-Dcom.sun.management.jmxremote.port=8009", \
#"-Dcom.sun.management.jmxremote.rmi.port=8009", \
#"-Dcom.sun.management.jmxremote.ssl=false", \
#"-Dcom.sun.management.jmxremote.authenticate=false", \
#"-Djava.rmi.server.hostname=192.168.20.155", \
#"-XX:+UseCodeCacheFlushing", \
#"-Xmx1600m", \
#"-jar", "/usr/share/proseo-storage-mgr/proseo-storage-mgr.jar"]

ENTRYPOINT ["java", \
"-XX:+UseCodeCacheFlushing", \
#"-Xmx1400m", \
"-jar", "/usr/share/proseo-storage-mgr/proseo-storage-mgr.jar"]