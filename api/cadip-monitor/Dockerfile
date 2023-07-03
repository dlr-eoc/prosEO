FROM openjdk:11

ADD target/proseo-cadip-monitor.jar /usr/share/proseo-cadip-monitor/proseo-cadip-monitor.jar
WORKDIR /usr/share/proseo-cadip-monitor

ENTRYPOINT ["java", "-jar", "/usr/share/proseo-cadip-monitor/proseo-cadip-monitor.jar"]