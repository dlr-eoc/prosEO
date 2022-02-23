prosEO Command Line Configuration
=================================

The prosEO Command Line Interface (CLI) is Java application, which runs locally on the user's personal workstation. A build
of prosEO creates the required JAR file as `<project root>/ui/cli/target/proseo-ui-cli.jar`.

The CLI is started as a Java application:
```
java -jar <path to jar>/proseo-ui-cli.jar <options>
```
Refer to the CLI documentation for the applicable options.

To establish the connection to the correct prosEO Control Instance, a file called `application.yml` must be present in the
directory, from which the CLI is started. A template file is provided in this directory.

If you created the server certificates for the Nginx reverse proxy as self-signed certificates, as it is suggested for the
setup of the bastion hosts, then you will experience security errors when trying to use the CLI. This can be avoided by
adding the self-signed certificate to Java's trust store (certificate path is relative to this directory):
```
sudo keytool -importcert -cacerts -storepass changeit \
    -file ../bastion-control/roles/configure_nginx_proxy/files/servercert.pem -alias cpros
```

In Windows (run command prompt as Administrator):
```
keytool.exe keytool -importcert -cacerts -storepass changeit  -file <path to certfile>/servercert.pem -alias cpros
```
