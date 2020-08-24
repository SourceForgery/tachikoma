package com.sourceforgery.tachikoma.grpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun CoroutineScope.grpcLaunch(block: suspend  () -> Unit) {
    launch {
        block()
    }
}
