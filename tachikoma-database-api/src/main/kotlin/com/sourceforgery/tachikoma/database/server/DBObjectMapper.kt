package com.sourceforgery.tachikoma.database.server

import com.fasterxml.jackson.databind.ObjectMapper

interface DBObjectMapper {
    val objectMapper: ObjectMapper
}
