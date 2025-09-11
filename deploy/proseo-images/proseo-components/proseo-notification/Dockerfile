FROM localhost:5000/proseo-notification:1.20
ADD application.yml /usr/share/notification/application.yml
WORKDIR /usr/share/notification

#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=host.docker.internal:8000", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/usr/share/notification/proseo-notification.jar"]
ENTRYPOINT ["java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/usr/share/notification/proseo-notification.jar"]
