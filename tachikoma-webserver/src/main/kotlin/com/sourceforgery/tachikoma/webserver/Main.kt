package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.server.ServerBuilder

fun main() {
    val sb = ServerBuilder()

    val server = sb.build()!!
    server.start()
}