import jnr.enxio.channels.NativeSelectorProvider
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.logging.Level
import java.util.logging.Logger
import jnr.unixsocket.UnixServerSocket
import jnr.unixsocket.UnixServerSocketChannel
import jnr.unixsocket.UnixSocketAddress
import jnr.unixsocket.UnixSocketChannel

fun main(args: Array<String>) {
    val path = java.io.File("/var/spool/postfix/private/tracer_tachikoma")
    path.deleteOnExit()
    val address = UnixSocketAddress(path)
    val channel = UnixServerSocketChannel.open()

    try {
        val sel = NativeSelectorProvider.getInstance().openSelector()
        channel.configureBlocking(false)
        channel.socket().bind(address)
        channel.register(sel, SelectionKey.OP_ACCEPT, ServerActor(channel, sel))

        while (sel.select() > 0) {
            val keys = sel.selectedKeys()
            val iterator = keys.iterator()
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
                println("No Actors Running any more")
                break
            }
        }
    } catch (ex: IOException) {
        Logger.getLogger(UnixServerSocket::class.java.name).log(Level.SEVERE, null, ex)
    }

    println("UnixServer EXIT")
}

internal interface Actor {
    fun rxready(): Boolean
}

internal class ServerActor(private val channel: UnixServerSocketChannel, private val selector: Selector) : Actor {
    override fun rxready(): Boolean {
        try {
            val client = channel.accept()
            client.configureBlocking(false)
            client.register(selector, SelectionKey.OP_READ, ClientActor(client))
            return true
        } catch (ex: IOException) {
            return false
        }
    }
}

class ParseStateMachine {
    private var key = StringBuilder()
    private var value = StringBuilder()
    private var map = HashMap<String, String>()
    private var readingKey = true

    public fun addByte(b: Byte) {
        if (b == 0.toByte()) {
            if (readingKey && key.length == 0) {
                System.err.println(map)
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
}

internal class ClientActor(private val channel: UnixSocketChannel) : Actor {
    private val parseStateMachine = ParseStateMachine()

    override fun rxready(): Boolean {
        try {
            val buf = ByteBuffer.allocate(1024)
            val n = channel.read(buf)
            val remote = channel.remoteSocketAddress!!
            for (i in 0 until n) {
                parseStateMachine.addByte(buf[i])
            }

            println("Read in $n bytes from $remote")
        } catch (ex: IOException) {
            ex.printStackTrace()
            return false
        }

        return true
    }
}