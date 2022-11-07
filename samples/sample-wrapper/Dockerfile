FROM localhost:5000/proseo-sample-processor:0.9.3-ODIP
WORKDIR /usr/share/proseo-sample-wrapper

ADD target/proseo-sample-wrapper-jar-with-dependencies.jar proseo-sample-wrapper.jar

#Debug
#ENV PROCESSOR_SHELL_COMMAND="java -Xdebug -Xrunjdwp:transport=dt_socket,address=192.168.20.155:8001 -jar /usr/share/sample-processor/proseo-sample-processor.jar"
ENV PROCESSOR_SHELL_COMMAND="java -jar /usr/share/sample-processor/proseo-sample-processor.jar"


#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=192.168.20.155:8002", "-jar", "/usr/share/proseo-sample-wrapper/proseo-sample-wrapper.jar", "de.dlr.proseo.samplewrap.SampleWrapper"]
ENTRYPOINT ["java", "-jar", "/usr/share/proseo-sample-wrapper/proseo-sample-wrapper.jar", "de.dlr.proseo.samplewrap.SampleWrapper"]

#ENTRYPOINT [ "/bin/bash"]
