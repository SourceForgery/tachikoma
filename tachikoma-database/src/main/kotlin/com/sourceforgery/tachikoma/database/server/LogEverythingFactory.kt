package com.sourceforgery.tachikoma.database.server

import com.sourceforgery.tachikoma.logging.InvokeCounterFactory

class LogEverythingFactory : InvokeCounterFactory {
    override fun create() = LogEverything()
}
