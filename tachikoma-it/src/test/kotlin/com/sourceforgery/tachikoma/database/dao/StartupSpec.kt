package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.TestAttribute
import com.sourceforgery.tachikoma.TestBinder
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.hk2.get
import io.ebean.EbeanServer
import io.ebean.config.dbplatform.postgres.PostgresPlatform
import io.ebeaninternal.server.core.DefaultServer
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
class StartupSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(TestBinder(TestAttribute.POSTGRESQL), DatabaseBinder())
    }
    afterEachTest {
        serviceLocator.shutdown()
    }
    describe("Test", {
        it("Test upgrade scripts", {
            val ebeanServer: EbeanServer = serviceLocator.get()
            assertTrue((ebeanServer as DefaultServer).databasePlatform is PostgresPlatform)
            ebeanServer.find(EmailDBO::class.java).findList()
        })
    })
})