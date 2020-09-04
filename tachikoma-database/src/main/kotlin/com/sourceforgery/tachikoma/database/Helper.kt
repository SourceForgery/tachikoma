package com.sourceforgery.tachikoma.database

import io.ebean.Database

inline fun <reified T> Database.find() = find(T::class.java)

inline fun <reified T> Database.find(id: Any) = find(T::class.java, id)
