FROM localhost:5000/proseo-aip-client:0.9.5-ODIP
ADD application.yml /usr/share/api/aip-client/application.yml
WORKDIR /usr/share/api/aip-client

#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=host.docker.internal:8000", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/usr/share/api/aip-client/proseo-aip-client.jar"]
ENTRYPOINT ["java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/usr/share/api/aip-client/proseo-aip-client.jar"]
