package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.TestAttribute
import com.sourceforgery.tachikoma.TestBinder
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.hk2.get
import io.ebean.EbeanServer
import io.ebean.config.dbplatform.postgres.PostgresPlatform
import io.ebeaninternal.server.core.DefaultServer
import kotlin.test.assertTrue
import org.apache.commons.lang3.RandomStringUtils
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class StartupSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(RandomStringUtils.randomAlphanumeric(10), TestBinder(TestAttribute.POSTGRESQL), DatabaseBinder())
    }
    afterEachTest {
        serviceLocator.shutdown()
    }
    describe("Test") {
        it("Test upgrade scripts") {
            val ebeanServer: EbeanServer = serviceLocator.get()
            val platform = (ebeanServer as DefaultServer).databasePlatform
            assertTrue(platform is PostgresPlatform, "platform is $platform")
            ebeanServer.find(EmailDBO::class.java).findList()
        }
    }
})