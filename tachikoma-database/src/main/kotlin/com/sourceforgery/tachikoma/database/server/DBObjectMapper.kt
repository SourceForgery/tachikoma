package com.sourceforgery.tachikoma.database.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import javax.inject.Inject

class DBObjectMapper
@Inject
private constructor(
) : ObjectMapper() {
    init {
        registerModule(JavaTimeModule())
        registerKotlinModule()
    }
}
