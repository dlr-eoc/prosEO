FROM eclipse-temurin:21
LABEL org.opencontainers.image.authors="Thomas Bassler <thomas.bassler@drbassler.de>"
ENTRYPOINT ["java", "-jar", "/usr/share/proseo-user-mgr/proseo-user-mgr.jar"]

#ADD target/lib /usr/share/proseo-processor-mgr/lib
ADD target/proseo-user-mgr.jar /usr/share/proseo-user-mgr/proseo-user-mgr.jar