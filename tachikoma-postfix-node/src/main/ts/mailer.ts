import * as grpc from 'grpc'

const messages = require('../../../build/proto/com/sourceforgery/tachikoma/mta/message_queue_pb');
const services = require('../../../build/proto/com/sourceforgery/tachikoma/mta/message_queue_grpc_pb');

const mailer_socket_path = '/var/spool/postfix/private/incoming_tachikoma';

const request = new messages.MTAQueuedNotification()
const client = new services.MTAEmailQueueClient('localhost:8070', grpc.credentials.createInsecure());

const getEmails = client.getEmails();

getEmails.on('data', (data) => {
  console.log('data',data)
  const hj = new messages.EmailMessage(data)
  console.log('hj', hj.toObject(data))
})

// console.log('getEmails', getEmails)

// class Mailer {
//   private mtaEmailQueue: MTAEmailQueue;
//
//   public constructor(mtaEmailQueue: com.sourceforgery.tachikoma.mta.MTAEmailQueue) {
//     this.mtaEmailQueue = mtaEmailQueue
//   }
//
//   public start() {
//     // this.mtaEmailQueue.getEmails(request: com.sourceforgery.tachikoma.mta.IMTAQueuedNotification, (error, response) => {
//     //
//     // }): void;
//
//   }
// }