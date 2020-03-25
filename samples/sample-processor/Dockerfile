FROM openjdk:11
WORKDIR /usr/share/sample-processor

#ADD target/lib /usr/share/sample-processor/lib
ADD target/proseo-sample-processor-jar-with-dependencies.jar proseo-sample-processor.jar

# Static configuration files
ADD src/test/resources/ptm_l1b_config.xml conf/ptm_l1b_config.xml
ADD src/test/resources/ptm_l2_config.xml conf/ptm_l2_config.xml
ADD src/test/resources/ptm_l3_config.xml conf/ptm_l3_config.xml

# can be run with:
# docker run --mount "type=bind,src=$(pwd)/src,dst=/usr/share/sample-processor/src" localhost:5000/proseo-sample-processor:latest src/test/resources/sample-joborder.xml