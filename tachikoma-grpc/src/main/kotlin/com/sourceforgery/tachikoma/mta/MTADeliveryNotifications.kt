package com.sourceforgery.tachikoma.mta

import com.sourceforgery.tachikoma.database.dao.EmailDAO
import javax.inject.Inject

internal class MTADeliveryNotifications
@Inject
private constructor(
        val emailDAO: EmailDAO
) {
    fun setDeliveryStatus(request: DeliveryNotification) {
        TODO("Implement this")
    }
}