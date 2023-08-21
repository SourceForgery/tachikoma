package com.sourceforgery.tachikoma.account

import com.sourceforgery.tachikoma.identifiers.MailDomain
import java.net.URI

interface AccountFacade {
    fun modifyAccount(mailDomain: MailDomain, baseUrl: URI?): Account
    operator fun get(mailDomain: MailDomain): Account?
}
