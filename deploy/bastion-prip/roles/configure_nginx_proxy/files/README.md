Populate this directory with valid server certificates named `servercert.pem` and `serverkey.pem`:
```
openssl req -x509 -newkey rsa:4096 -keyout serverkey.pem -out servercert.pem -days 3650 -nodes
```


(Note that for a deployment with LetsEncrypt these are only dummies, which will be replaced by the Certbot.
For other deployments this should be valid server certificates.)