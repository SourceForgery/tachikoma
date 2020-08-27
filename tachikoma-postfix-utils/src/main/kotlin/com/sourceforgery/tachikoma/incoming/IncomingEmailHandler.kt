package com.sourceforgery.tachikoma.incoming

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.mta.IncomingEmailMessage
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import java.nio.charset.StandardCharsets
import jnr.unixsocket.UnixServerSocketChannel
import jnr.unixsocket.UnixSocketAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.logging.log4j.kotlin.logger

class IncomingEmailHandler(
    private val stub: MTAEmailQueueGrpc.MTAEmailQueueBlockingStub
) {

    fun start(): Job {
        return GlobalScope.launch(Dispatchers.IO) {
            SOCKET_PATH.delete()
            SOCKET_PATH.deleteOnExit()
            SOCKET_PATH.parentFile.mkdirs()
            val address = UnixSocketAddress(SOCKET_PATH)
            try {
                UnixServerSocketChannel.open()!!
                    .use { serverSocketChannel ->
                        val serverSocket = serverSocketChannel.socket()!!
                        serverSocket.bind(address)
                        LOGGER.info { "Successfully started listening for incoming emails on socket" }
                        while (true) {
                            val clientSocket = serverSocketChannel.accept()
                            launch {
                                val socket = clientSocket.socket()
                                val lmtpServer = LMTPServer(socket) { fromEmailAddress, emailBody, toEmailAddress ->
                                    val incomingEmailMessage = IncomingEmailMessage.newBuilder()
                                        .setBody(ByteString.copyFrom(emailBody, StandardCharsets.US_ASCII))
                                        .setFrom(fromEmailAddress)
                                        .setEmailAddress(toEmailAddress)
                                        .build()
                                    stub.incomingEmail(incomingEmailMessage)
                                }
                                lmtpServer.receiveMail()
                            }
                        }
                    }
            } catch (e: Exception) {
                LOGGER.error(e) { "Failure with socket $SOCKET_PATH" }
            }
        }
    }

    companion object {
        val SOCKET_PATH = java.io.File("/var/spool/postfix/tachikoma/incoming_tachikoma")
        private val LOGGER = logger()
    }
}
