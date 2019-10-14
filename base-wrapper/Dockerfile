FROM openjdk:11
WORKDIR /usr/share/base-wrapper
ENTRYPOINT ["java", "-jar", "proseo-base-wrapper.jar"]

#ADD target/lib /usr/share/base-wrapper/lib
ADD target/proseo-base-wrapper-jar-with-dependencies.jar proseo-base-wrapper.jar

# can be run with:
# docker run --mount "type=bind,src=$(pwd)/src,dst=/usr/share/base-wrapper/src" localhost:5000/proseo-base-wrapper:latest src/test/resources/base-joborder.xml