FROM openjdk:11
WORKDIR /usr/share/sample-processor
ENTRYPOINT ["java", "-jar", "proseo-sample-processor.jar"]

#ADD target/lib /usr/share/sample-processor/lib
ADD target/proseo-sample-processor-jar-with-dependencies.jar proseo-sample-processor.jar

# can be run with:
# docker run --mount "type=bind,src=$(pwd)/src,dst=/usr/share/sample-processor/src" localhost:5000/proseo-sample-processor:latest src/test/resources/sample-joborder.xml