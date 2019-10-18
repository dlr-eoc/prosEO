FROM openjdk:11
WORKDIR /usr/share/sample-wrapper
ENTRYPOINT ["java", "-jar", "proseo-sample-wrapper.jar"]

#ADD target/lib /usr/share/sample-wrapper/lib
ADD target/proseo-sample-wrapper-jar-with-dependencies.jar proseo-sample-wrapper.jar

