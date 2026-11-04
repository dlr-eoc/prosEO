FROM eclipse-temurin:21
LABEL org.opencontainers.image.authors="Thomas Bassler <thomas.bassler@drbassler.de>"

ADD target/proseo-datatake-mgr.jar /usr/share/proseo-datatake-mgr/proseo-datatake-mgr.jar

ENTRYPOINT ["java", "-jar", "/usr/share/proseo-datatake-mgr/proseo-datatake-mgr.jar"]
