FROM localhost:5000/proseo-api-odip:2.0.0
ADD application.yml /usr/share/api/prip/application.yml
WORKDIR /usr/share/api/odip

#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=host.docker.internal:8000", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/usr/share/api/prip/proseo-api-odip.jar"]
ENTRYPOINT ["java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/usr/share/api/prip/proseo-api-odip.jar"]
