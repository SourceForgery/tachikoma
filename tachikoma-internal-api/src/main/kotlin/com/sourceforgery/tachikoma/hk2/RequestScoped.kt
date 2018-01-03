package com.sourceforgery.tachikoma.hk2

import org.glassfish.hk2.api.Proxiable
import javax.inject.Scope


@Scope
@Proxiable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class RequestScoped

