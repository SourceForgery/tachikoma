import * as protobuf from 'protobufjs';

import * as grpc from 'grpc';


const client = new grpc.Client('localhost:8070', grpc.credentials.createInsecure())


// new mtaGrpc.com.sourceforgery.tachikoma.mta.MTADeliveryNotifications(client, null, null)
console.log('Protobuf', protobuf);
