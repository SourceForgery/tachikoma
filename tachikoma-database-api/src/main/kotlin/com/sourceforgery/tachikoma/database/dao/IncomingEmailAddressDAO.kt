package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO

interface IncomingEmailAddressDAO {
    fun getByEmail(email: Email): IncomingEmailAddressDBO?
}