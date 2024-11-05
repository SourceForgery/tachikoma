package com.sourceforgery.tachikoma

import com.google.protobuf.GeneratedMessage

inline operator fun <T : GeneratedMessage.Builder<V>, V> T.invoke(block: T.() -> Unit) =
    apply {
        block()
    }
