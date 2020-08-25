package com.sourceforgery.tachikoma.database

import io.ebean.Database

inline fun <reified T> Database.find() = find(T::class.java)
