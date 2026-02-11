FROM eclipse-temurin:21
LABEL org.opencontainers.image.authors="Ernst Melchinger <melchinger@prophos.de>"
ENTRYPOINT ["java", "-jar", "/usr/share/proseo-order-gen/proseo-order-gen.jar"]

#ADD target/lib /usr/share/planner/lib
ADD target/proseo-order-gen.jar /usr/share/proseo-order-gen/proseo-order-gen.jar