package com.sourceforgery.tachikoma.database.objects

data class StatusEventMetaData
constructor(
    val mtaStatusCode: String? = null,
    val ipAddress: String? = null,
    val trackingLink: String? = null
)