package com.sourceforgery.tachikoma.hk2

import org.glassfish.hk2.api.ServiceLocator

inline fun <reified T : Any> ServiceLocator.get(): T = this.getService(T::class.java)