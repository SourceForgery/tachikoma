package com.sourceforgery.tachikoma.tracer

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mta.DeliveryNotification
import com.sourceforgery.tachikoma.mta.MTADeliveryNotificationsGrpc
import com.sourceforgery.tachikoma.socketlistener.UnixSocketListener
import io.grpc.Channel
import io.grpc.stub.StreamObserver

class TraceMessageListener(
        channel: Channel
) {
    private val stub = MTADeliveryNotificationsGrpc.newStub(channel)

    fun startBlocking() {
        UnixSocketListener(SOCKET_PATH, { TracerParser(it, this::setDeliveryStatus) }).startBlocking()
    }

    private fun setDeliveryStatus(map: Map<String, String>) {
        // Decipher what kind of message it is, and send it to mta_notifier
        // {"dsn_orig_rcpt": "rfc822;foo@example.net", "flags": "1024", "notify_flags": "0", "nrequest": "0","offset": "258", "original_recipient": "foo@example.net", "queue_id": "458182054", "recipient": "foo@example.net", "status": "4.4.1"}
        // {"diag_type": "diag_text"}
        // {"mta_type": "mta_mname"}
        // {"action": "delayed", "reason": "connect to example.net[93.184.216.34]:25: Connection timed out"}
        // {"action": "relayed", "diag_text": "250 2.0.0 OK 1515166737 d71si1945550lfg.282 - gsmtp", "diag_type": "smtp", "dsn_orig_rcpt": "rfc822;foo@toface.com", "flags": "1024", "mta_mname": "aspmx.l.google.com", "mta_type": "dns", "notify_flags": "0", "nrequest": "0", "offset": "256", "original_recipient": "foo@toface.com", "queue_id": "2061D205A", "reason": "delivery via aspmx.l.google.com[173.194.222.27]:25: 250 2.0.0 OK 1515166737 d71si1945550lfg.282 - gsmtp", "recipient": "foo@toface.com", "status": "2.0.0"}

        map["status"]?.let { status ->
            map["queue_id"]?.let { queueId ->
                map["original_recipient"]?.let { originalRecipient ->
                    val notification = DeliveryNotification.newBuilder()
                            .setDiagnoseText(map["diag_text"].orEmpty())
                            .setReason(map["reason"].orEmpty())
                            .setQueueId(queueId)
                            .setStatus(status)
                            .setOriginalRecipient(originalRecipient)
                            .build()
                    stub.setDeliveryStatus(notification, nullObserver)
                }
            }
        }
    }

    private val nullObserver = object : StreamObserver<Empty> {
        override fun onNext(value: Empty?) {
            // Don't care
        }

        override fun onCompleted() {
            // Don't care
        }

        override fun onError(t: Throwable?) {
            LOGGER.warn("Exception sending setDeliveryStatus", t)
        }
    }

    companion object {
        val SOCKET_PATH = java.io.File("/var/spool/postfix/private/tracer_tachikoma")
        private val LOGGER = logger()
    }
}