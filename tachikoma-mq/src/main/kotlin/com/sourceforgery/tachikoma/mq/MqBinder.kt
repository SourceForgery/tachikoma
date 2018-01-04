package com.sourceforgery.tachikoma.mq

import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class MqBinder : AbstractBinder() {
    override fun configure() {
        bindAsContract(ConsumerFactoryImpl::class.java)
                .to(MQSequenceFactory::class.java)
                .to(MQSender::class.java)
                .`in`(Singleton::class.java)
    }
}
