package com.sourceforgery.tachikoma.socketlistener

import com.sourceforgery.tachikoma.logging.logger
import jnr.enxio.channels.NativeSelectorProvider
import jnr.unixsocket.UnixServerSocketChannel
import jnr.unixsocket.UnixSocketAddress
import jnr.unixsocket.UnixSocketChannel
import java.io.File
import java.io.IOException
import java.nio.channels.SelectionKey
import java.nio.channels.spi.AbstractSelector

public class UnixSocketListener(
        socketFile: File,
        private val actorCreator: (UnixSocketChannel) -> Actor
) {
    private val address: UnixSocketAddress
    private val channel: UnixServerSocketChannel
    private val selector: AbstractSelector

    init {
        socketFile.delete()
        socketFile.deleteOnExit()
        address = UnixSocketAddress(socketFile)
        channel = UnixServerSocketChannel.open()
        selector = NativeSelectorProvider.getInstance().openSelector()
    }

    fun start() {
        try {
            channel.configureBlocking(false)
            channel.socket().bind(address)
            channel.register(selector, SelectionKey.OP_ACCEPT, ServerActor())

            while (selector.select() > 0) {
                val iterator = selector.selectedKeys().iterator()
                var running = false
                var cancelled = false

                while (iterator.hasNext()) {
                    val k = iterator.next()
                    val a = k.attachment() as Actor
                    if (a.rxready()) {
                        running = true
                    } else {
                        k.cancel()
                        cancelled = true
                    }
                    iterator.remove()
                }
                if (!running && cancelled) {
                    LOGGER.warn("No Actors Running any more")
                    break
                }
            }
        } catch (ex: IOException) {
            LOGGER.error("Caught exception in eventLoop", ex)
        }

        LOGGER.info("UnixServer EXIT")
    }

    inner class ServerActor : Actor {
        override fun rxready(): Boolean {
            return try {
                val client = channel.accept()
                client.configureBlocking(false)
                client.register(selector, SelectionKey.OP_READ, actorCreator(client))
                true
            } catch (ex: IOException) {
                LOGGER.info("Caught exception in server actor", ex)
                false
            }
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}
