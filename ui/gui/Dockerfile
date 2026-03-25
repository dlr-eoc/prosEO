FROM eclipse-temurin:21
LABEL org.opencontainers.image.authors="Ernst Melchinger <melchinger@prophos.de>"
ENTRYPOINT ["java", "-jar", "/usr/share/proseo-ui-gui/proseo-ui-gui.jar"]

#ADD target/lib /usr/share/proseo-ui-gui/lib
ADD target/proseo-ui-gui.jar /usr/share/proseo-ui-gui/proseo-ui-gui.jar