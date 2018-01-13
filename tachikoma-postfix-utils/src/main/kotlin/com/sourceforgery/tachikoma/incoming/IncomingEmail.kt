package com.sourceforgery.tachikoma.incoming

import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mta.IncomingEmailMessage
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import io.grpc.Channel
import io.grpc.stub.StreamObserver
import jnr.unixsocket.UnixServerSocketChannel
import jnr.unixsocket.UnixSocketAddress
import java.util.concurrent.Executors

class IncomingEmail(
        grpcChannel: Channel
) {
    private val stub = MTAEmailQueueGrpc.newStub(grpcChannel)!!

    val executor = Executors.newCachedThreadPool()

    fun start() {
        executor.run { startBlocking() }
    }

    fun startBlocking() {
        SOCKET_PATH.delete()
        SOCKET_PATH.deleteOnExit()
        val address = UnixSocketAddress(SOCKET_PATH)
        val serverSocketChannel = UnixServerSocketChannel.open()
        while (true) {
            val clientSocket = serverSocketChannel.accept()
            val socket = clientSocket.socket()
            val lmtpServer = LMTPServer(socket, this::acceptIncomingEmail)
            executor.execute(lmtpServer)
        }
    }

    fun acceptIncomingEmail(fromEmailAddress: String, emailBody: ByteArray, toEmailAddress: String) {
        val incomingEmailMessage = IncomingEmailMessage.newBuilder()
                .setBody(ByteString.copyFrom(emailBody))
                .setFrom(fromEmailAddress)
                .setEmailAddress(toEmailAddress)
                .build()
        stub.incomingEmail(incomingEmailMessage, nullObserver)
    }

    private val nullObserver = object : StreamObserver<Empty> {
        override fun onNext(value: Empty?) {
            // Don't care
        }

        override fun onCompleted() {
            // Don't care
        }

        override fun onError(t: Throwable?) {
            LOGGER.warn("Exception sending incoming email", t)
        }
    }

    companion object {
        val SOCKET_PATH = java.io.File("/var/spool/postfix/private/incoming_tachikoma")
        val LOGGER = logger()
    }
}