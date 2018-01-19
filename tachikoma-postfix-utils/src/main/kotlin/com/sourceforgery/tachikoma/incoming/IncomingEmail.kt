package com.sourceforgery.tachikoma.incoming

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mta.IncomingEmailMessage
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import com.sourceforgery.tachikoma.mta.MailAcceptanceResult
import io.grpc.Channel
import jnr.unixsocket.UnixServerSocketChannel
import jnr.unixsocket.UnixSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class IncomingEmail(
        grpcChannel: Channel
) {
    private val stub = MTAEmailQueueGrpc.newBlockingStub(grpcChannel)

    private val executor = Executors.newCachedThreadPool()!!

    fun start() {
        executor.execute { startBlocking() }
    }

    private fun startBlocking() {
        SOCKET_PATH.delete()
        SOCKET_PATH.deleteOnExit()
        val address = UnixSocketAddress(SOCKET_PATH)
        val serverSocketChannel = UnixServerSocketChannel.open()
        val serverSocket = serverSocketChannel.socket()
        serverSocket.bind(address)
        LOGGER.info { "Successfully started listening for incoming emails on socket" }
        while (true) {
            val clientSocket = serverSocketChannel.accept()
            val socket = clientSocket.socket()
            val lmtpServer = LMTPServer(socket, this::acceptIncomingEmail)
            executor.execute(lmtpServer)
        }
    }

    private fun acceptIncomingEmail(fromEmailAddress: String, emailBody: String, toEmailAddress: String): MailAcceptanceResult {
        val incomingEmailMessage = IncomingEmailMessage.newBuilder()
                .setBody(ByteString.copyFrom(emailBody, StandardCharsets.US_ASCII))
                .setFrom(fromEmailAddress)
                .setEmailAddress(toEmailAddress)
                .build()
        return stub.incomingEmail(incomingEmailMessage)
    }

    companion object {
        val SOCKET_PATH = java.io.File("/var/spool/postfix/private/incoming_tachikoma")
        private val LOGGER = logger()
    }
}