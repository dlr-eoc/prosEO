FROM localhost:5000/proseo-sample-processor:0.1.0-SNAPSHOT
WORKDIR /usr/share/proseo-sample-wrapper
ENTRYPOINT ["java", "-jar", "/usr/share/proseo-sample-wrapper/proseo-sample-wrapper.jar"]

ADD target/proseo-sample-wrapper-jar-with-dependencies.jar proseo-sample-wrapper.jar
