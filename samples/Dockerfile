FROM proseo/proseo-sample-processor:0.0.1-SNAPSHOT


ADD sample-wrapper/target/proseo-sample-wrapper-jar-with-dependencies.jar proseo-sample-wrapper.jar
ENV PROCESSOR_SHELL_COMMAND="java -jar proseo-sample-processor.jar"

ENTRYPOINT ["java", "-jar", "proseo-sample-wrapper.jar"]