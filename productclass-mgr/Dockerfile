FROM eclipse-temurin:21
LABEL org.opencontainers.image.authors="Thomas Bassler <thomas.bassler@drbassler.de>"
ENTRYPOINT ["java", "-jar", "/usr/share/proseo-productclass-mgr/proseo-productclass-mgr.jar"]

#ADD target/lib /usr/share/proseo-productclass-mgr/lib
ADD target/proseo-productclass-mgr.jar /usr/share/proseo-productclass-mgr/proseo-productclass-mgr.jar