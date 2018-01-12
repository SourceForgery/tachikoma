import * as net from "net";
import { com } from '../../../typings/mtagrpc'
import MTADeliveryNotifications = com.sourceforgery.tachikoma.mta.MTADeliveryNotifications
import DeliveryNotification = com.sourceforgery.tachikoma.mta.DeliveryNotification

const tracer_socket_path = '/var/spool/postfix/private/tracer_tachikoma';

class ParseStateMachine {
  private callback: (map: { [p: string]: string }) => void;

  constructor(callback: (map: { [key: string]: string }) => void) {
    this.callback = callback;
  }

  private key = "";
  private value = "";
  private map: { [key: string]: string } = {};
  private readingKey = true;

  public addByte(byte: number) {
    if (byte === 0) {
      if (this.readingKey && this.key.length === 0) {
        this.callback(this.map);
        this.map = {};
      } else {
        this.readingKey = !this.readingKey;
        if (this.readingKey) {
          this.map[this.key] = this.value;
          this.key = "";
          this.value = "";
        }
      }
    } else {
      if (this.readingKey) {
        this.key += String.fromCharCode(byte);
      } else {
        this.value += String.fromCharCode(byte);
      }
    }
  }

}


class Tracer {

  public constructor(mtaDeliveryRecord: MTADeliveryNotifications) {
  }


  public start() {
    const tracerSocket = net.createServer(function (client) {
      const stateMachine = new ParseStateMachine((map) => {
        this.sendEvent(map)
      });
      client.addListener("data", (data: Buffer) => {
        data.forEach(byte => {
          stateMachine.addByte(byte)
        })
      });
      client.addListener("close", () => {
        client.end();
      });
    });

    tracerSocket.listen(tracer_socket_path);
  }


  private sendEvent(map: { [key: string]: string }) {
    // {"dsn_orig_rcpt": "rfc822;foo@example.net", "flags": "1024", "notify_flags": "0", "nrequest": "0","offset": "258", "original_recipient": "foo@example.net", "queue_id": "458182054", "recipient": "foo@example.net", "status": "4.4.1"}
    // {"diag_type": "diag_text"}
    // {"mta_type": "mta_mname"}
    // {"action": "delayed", "reason": "connect to example.net[93.184.216.34]:25: Connection timed out"}
    // {"action": "relayed", "diag_text": "250 2.0.0 OK 1515166737 d71si1945550lfg.282 - gsmtp", "diag_type": "smtp", "dsn_orig_rcpt": "rfc822;foo@toface.com", "flags": "1024", "mta_mname": "aspmx.l.google.com", "mta_type": "dns", "notify_flags": "0", "nrequest": "0", "offset": "256", "original_recipient": "foo@toface.com", "queue_id": "2061D205A", "reason": "delivery via aspmx.l.google.com[173.194.222.27]:25: 250 2.0.0 OK 1515166737 d71si1945550lfg.282 - gsmtp", "recipient": "foo@toface.com", "status": "2.0.0"}

    const status = map["status"];
    const queueId = map["queue_id"];
    const originalRecipient = map["original_recipient"];
    const reason = map["reason"];
    const diagnoseText = map["diag_text"];
    if (status && queueId && originalRecipient) {
      const props = {
        status,
        queueId,
        originalRecipient,
        reason,
        diagnoseText,
      };
      const notification = new DeliveryNotification(props);
      // mtaDeliveryRecord.setDeliveryStatus(notification);
      // Send over gRPC
    }
  }
}