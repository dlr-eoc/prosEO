FROM 192.168.20.155:5000/proseo-sample-processor:0.1.0-SNAPSHOT
WORKDIR /usr/share/proseo-sample-wrapper

ADD target/proseo-sample-wrapper-jar-with-dependencies.jar proseo-sample-wrapper.jar
ENV PROCESSOR_SHELL_COMMAND="java -jar /usr/share/sample-processor/proseo-sample-processor.jar"

ENTRYPOINT ["java", "-jar", "/usr/share/proseo-sample-wrapper/proseo-sample-wrapper.jar"]
#ENTRYPOINT [ "/bin/bash"]
