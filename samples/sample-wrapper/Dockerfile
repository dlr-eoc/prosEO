FROM localhost:5000/proseo-sample-processor:1.1.0
WORKDIR /usr/share/proseo-sample-wrapper

ADD target/proseo-sample-wrapper-jar-with-dependencies.jar proseo-sample-wrapper.jar

# Timeout if other prosEO services do not respond within 10 min
ENV HTTP_TIMEOUT=600

ENV PROCESSOR_SHELL_COMMAND="java -jar /usr/share/sample-processor/proseo-sample-processor.jar"

ENTRYPOINT ["java", "-jar", "/usr/share/proseo-sample-wrapper/proseo-sample-wrapper.jar", "de.dlr.proseo.samplewrap.SampleWrapper"]

# Variable setting for Debugging (adapt IP address as needed)
#ENV PROCESSOR_SHELL_COMMAND="java -Xdebug -Xrunjdwp:transport=dt_socket,address=192.168.20.155:8001 -jar /usr/share/sample-processor/proseo-sample-processor.jar"

#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=192.168.20.155:8002", "-jar", "/usr/share/proseo-sample-wrapper/proseo-sample-wrapper.jar", "de.dlr.proseo.samplewrap.SampleWrapper"]
