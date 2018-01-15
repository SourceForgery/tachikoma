package com.sourceforgery.tachikoma.tracer

import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.socketlistener.Actor
import jnr.unixsocket.UnixSocketChannel
import java.io.IOException
import java.nio.ByteBuffer

class TracerParser(
        private val channel: UnixSocketChannel,
        private val callback: (Map<String, String>) -> Unit
) : Actor {

    override fun rxready(): Boolean {
        try {
            val buf = ByteBuffer.allocate(1024)
            val n = channel.read(buf)
            for (i in 0 until n) {
                addByte(buf[i])
            }
            LOGGER.debug { "Read in $n bytes from ${channel.remoteSocketAddress}" }
        } catch (ex: IOException) {
            ex.printStackTrace()
            return false
        }

        return true
    }

    private var key = StringBuilder()
    private var value = StringBuilder()
    private var map = HashMap<String, String>()
    private var readingKey = true

    private fun addByte(b: Byte) {
        if (b == 0.toByte()) {
            if (readingKey && key.isEmpty()) {
                callback(map)
                map = HashMap()
            } else {
                this.readingKey = !this.readingKey
                if (this.readingKey) {
                    map[key.toString()] = value.toString()
                    this.key = StringBuilder()
                    this.value = StringBuilder()
                }
            }
        } else {
            if (this.readingKey) {
                this.key.append(b.toChar())
            } else {
                this.value.append(b.toChar())
            }
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}
