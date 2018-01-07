package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.mq.jobs.JobFactory
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class MqBinder : AbstractBinder() {
    override fun configure() {
        bindAsContract(ConsumerFactoryImpl::class.java)
                .to(MQSequenceFactory::class.java)
                .to(MQSender::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(JobFactory::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(JobWorker::class.java)
                .`in`(Singleton::class.java)
    }
}
