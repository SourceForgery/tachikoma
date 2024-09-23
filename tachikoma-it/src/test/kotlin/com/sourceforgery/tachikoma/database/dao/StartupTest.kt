package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.TestAttribute
import com.sourceforgery.tachikoma.database.find
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.testModule
import io.ebean.Database
import io.ebean.platform.postgres.PostgresPlatform
import io.ebeaninternal.server.core.DefaultServer
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import kotlin.test.assertTrue

class StartupTest : DIAware {
    override val di =
        DI {
            importOnce(testModule(TestAttribute.POSTGRESQL), allowOverride = true)
        }

    val database: Database by instance()

    @Test
    fun `Test upgrade scripts`() {
        val platform = (database as DefaultServer).config().databasePlatform
        assertTrue(platform is PostgresPlatform, "platform is $platform")
        database.find<EmailDBO>().findList()
    }
}
