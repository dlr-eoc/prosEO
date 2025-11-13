FROM eclipse-temurin:21

ADD target/proseo-auxip-monitor.jar /usr/share/proseo-auxip-monitor/proseo-auxip-monitor.jar
WORKDIR /usr/share/proseo-auxip-monitor

ENTRYPOINT ["java", "-jar", "/usr/share/proseo-auxip-monitor/proseo-auxip-monitor.jar"]