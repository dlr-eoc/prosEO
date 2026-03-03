FROM eclipse-temurin:21

ADD target/proseo-storage-mgr.jar /usr/share/proseo-storage-mgr/proseo-storage-mgr.jar
WORKDIR /usr/share/proseo-storage-mgr

#ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n

#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=192.168.20.155:8000", "-jar", "/usr/share/proseo-storage-mgr/proseo-storage-mgr.jar"]
ENTRYPOINT ["java", "-jar", "/usr/share/proseo-storage-mgr/proseo-storage-mgr.jar"]