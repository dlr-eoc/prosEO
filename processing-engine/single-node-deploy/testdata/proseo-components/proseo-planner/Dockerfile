FROM localhost:5000/proseo-planner:0.8.0
ADD application.yml /usr/share/planner/application.yml

# Uncomment the next line, if a fixed login to a Kubernetes instance is required, and provide kube_config from the template file
ADD kube_config /usr/share/planner/kube_config

WORKDIR /usr/share/planner/

#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=host.docker.internal:8000", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/usr/share/planner/proseo-planner.jar"]
ENTRYPOINT ["java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/usr/share/planner/proseo-planner.jar"]
