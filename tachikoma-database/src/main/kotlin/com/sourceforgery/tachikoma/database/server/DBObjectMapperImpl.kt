package com.sourceforgery.tachikoma.database.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object DBObjectMapperImpl : DBObjectMapper {
    override val objectMapper: ObjectMapper by lazy {
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerKotlinModule()
    }
}
