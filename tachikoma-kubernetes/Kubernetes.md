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

Use either letsencrypt or other signed cert 

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

The files need to be called _exactly_ what they are called here

```
$ kubectl create secret generic tachikoma-webserver-cert \
 --from-file=./server.pem \
 --from-file=./ca_chain.pem
```




### Upgrading

``
$ kubectl replace -f deployment-webserver.yaml
```