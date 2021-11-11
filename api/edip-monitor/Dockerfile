FROM openjdk:11

ADD target/proseo-edip-monitor.jar /usr/share/proseo-edip-monitor/proseo-edip-monitor.jar
WORKDIR /usr/share/proseo-edip-monitor

ENTRYPOINT ["java", "-jar", "/usr/share/proseo-edip-monitor/proseo-edip-monitor.jar"]