FROM openjdk:11
WORKDIR /usr/share/sample-wrapper
ENTRYPOINT ["/usr/bin/java", "-jar", "proseo-sample-wrapper.jar"]

#ADD target/lib /usr/share/sample-wrapper/lib
ADD target/proseo-sample-wrapper-jar-with-dependencies.jar proseo-sample-wrapper.jar

# can be run with:
# docker run --mount "type=bind,src=$(pwd)/src,dst=/usr/share/sample-wrapper/src" localhost:5000/proseo-sample-wrapper:latest src/test/resources/sample-joborder.xml