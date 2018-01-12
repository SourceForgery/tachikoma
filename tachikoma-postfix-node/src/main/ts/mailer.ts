import { com } from '../../../typings/mtagrpc'
import MTAEmailQueue = com.sourceforgery.tachikoma.mta.MTAEmailQueue

const mailer_socket_path = '/var/spool/postfix/private/incoming_tachikoma';

class Mailer {
  private mtaEmailQueue: MTAEmailQueue;

  public constructor(mtaEmailQueue: com.sourceforgery.tachikoma.mta.MTAEmailQueue) {
    this.mtaEmailQueue = mtaEmailQueue
  }

  public start() {
    // this.mtaEmailQueue.getEmails(request: com.sourceforgery.tachikoma.mta.IMTAQueuedNotification, (error, response) => {
    //
    // }): void;

  }
}