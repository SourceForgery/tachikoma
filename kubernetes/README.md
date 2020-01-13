## 1. Setting the settings

Edit the config. I recommend using `pwgen` or something similar to generate 
the sign keys, preferably long passwords (40-50 chars)
```
$ editor tachikoma.properties
```

Create the config secret
```
$ kubectl create secret generic tachikoma-webserver-config \
 --from-file=./tachikoma.properties
```

## 2. Set up encryption

Use either letsencrypt or other signed cert.

### 2a. Use letsencrypt to provide certs
Set the hostname to use for connecting to the certificate server. 
```
$ sed -i 's/example.com/«yourdomain»/g' certificate.yaml
```

Set 
```
$ kubectl create secret generic tachikoma-webserver-config \
 --from-file=./tachikoma.properties
```



### 2b. Use other certificate

The key MUST be in pkcs8 format. If it isn't use the following command to convert it:
```
openssl pkcs8 -topk8 -in <server key in other format> -out server.key -nocrypt
```

The files need to be called _exactly_ what they are called here

```
$ kubectl create secret generic tachikoma-webserver-cert \
 --from-file=./server.crt \
 --from-file=./server.key
```

### Upgrading the certificate

When upgrading the cert, the old secret must be deleted first, e.g.
```
kubectl delete secret tachikoma-webserver-cert
```


## Upgrading

```
$ kubectl replace -f deployment-webserver.yaml
```
