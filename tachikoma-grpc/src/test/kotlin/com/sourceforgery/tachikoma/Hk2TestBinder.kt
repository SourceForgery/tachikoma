package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.tracking.TrackingConfig
import com.sourceforgery.tachikoma.tracking.TrackingDecoder
import com.sourceforgery.tachikoma.tracking.TrackingDecoderImpl
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoder
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoderImpl
import org.glassfish.hk2.utilities.binding.AbstractBinder
import java.net.URI
import javax.inject.Singleton

class Hk2TestBinder : AbstractBinder() {
    override fun configure() {
        bind(object : TrackingConfig {
            override val encryptionKey = "lk,;sxjdfljkdskljhnfgdskjlhfrjhkl;fdsflijkfgdsjlkfdslkjfjklsd"
            override val baseUrl: URI = URI.create("http://localhost/")
        })
                .to(TrackingConfig::class.java)
        bindAsContract(TrackingDecoderImpl::class.java)
                .to(TrackingDecoder::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(UnsubscribeDecoderImpl::class.java)
                .to(UnsubscribeDecoder::class.java)
                .`in`(Singleton::class.java)
    }
}