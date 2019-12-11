package com.sourceforgery.tachikoma.hk2

import javax.inject.Scope
import org.glassfish.hk2.api.Proxiable

@Scope
@Proxiable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class RequestScoped