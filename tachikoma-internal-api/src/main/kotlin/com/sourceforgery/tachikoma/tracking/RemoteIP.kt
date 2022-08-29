package com.sourceforgery.tachikoma.tracking

import io.netty.util.AttributeKey

interface RemoteIP {
    val remoteAddress: String
}
val REMOTE_IP_ATTRIB = AttributeKey.newInstance<String>("remote.ip")
