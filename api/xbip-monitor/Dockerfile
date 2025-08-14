FROM eclipse-temurin:21

ADD target/proseo-xbip-monitor.jar /usr/share/proseo-xbip-monitor/proseo-xbip-monitor.jar
WORKDIR /usr/share/proseo-xbip-monitor

ENTRYPOINT ["java", "-jar", "/usr/share/proseo-xbip-monitor/proseo-xbip-monitor.jar"]