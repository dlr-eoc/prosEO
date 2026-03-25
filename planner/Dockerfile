FROM eclipse-temurin:21
LABEL org.opencontainers.image.authors="Ernst Melchinger <melchinger@prophos.de>"
ENTRYPOINT ["java", "-jar", "/usr/share/planner/proseo-planner.jar"]

#ADD target/lib /usr/share/planner/lib
ADD target/proseo-planner.jar /usr/share/planner/proseo-planner.jar