FROM localhost:5000/proseo-ingestor:1.1.0
ADD application.yml /usr/share/ingestor/application.yml
WORKDIR /usr/share/ingestor

#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=host.docker.internal:8000", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/usr/share/ingestor/proseo-ingestor.jar"]
