package com.sourceforgery.tachikoma.database.objects

import io.ebean.common.BeanList

class SentMailMessageBodyDBO(
        val body: String
) : GenericDBO() {
    val emails: List<EmailDBO> = BeanList()
}