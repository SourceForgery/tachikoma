package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.identifiers.UserId
import javax.persistence.Column
import javax.persistence.ManyToOne

class UserDBO(
        // val password: String

        @Column(unique = true)
        var apiToken: String,

        @Column
        val backend: Boolean = false,

        @ManyToOne
        val account: AccountDBO? = null
) : GenericDBO() {

    init {
        if (backend && account != null) {
            throw RuntimeException("Should not be both backend and have accountId")
        }
        if (!backend && account == null) {
            throw RuntimeException("Must have at least one authenticator")
        }
    }
}

val UserDBO.id: UserId
    get() = UserId(realId as Long)