package com.sourceforgery.tachikoma.rest.unsubscribe

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kodein.di.DI

class AbuseReportingServiceTest {
    @Test
    fun `render abuse page`() {
        val service =
            AbuseReportingService(
                DI {
                },
            )

        assertEquals(
            """
            <head title="Abuse reporting"></head>
            <body>
              <form method="post"><span style="color: red">yes error</span>
                <table>
                  <tr>
                    <td style="width: 20%">Message id on the form of 'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX@some-host.com' (without quotation). Looking through the offending email for 'Message-Id: ', remove the &lt;&gt; and put the remainder into this field</td>
                    <td><input type="text" name="abuseEmailId" value=""></td>
                  </tr>
                  <tr>
                    <td>Your name. (Optional)</td>
                    <td><input type="text" name="reporterName" value="foo"></td>
                  </tr>
                  <tr>
                    <td>Your email. (Optional)</td>
                    <td><input type="text" name="reporterEmail" value="foo@bar.com"></td>
                  </tr>
                  <tr>
                    <td>ALL of the headers in the email and any extra information that may be useful.</td>
                    <td><textarea rows="100" cols="200" name="info">info</textarea></td>
                  </tr>
                </table>
            <input type="submit" name="submit" value="Submit abuse complaint"></form>
            </body>
            """.trimIndent(),
            service.renderPage(
                mailId = AutoMailId(""),
                info = "info",
                reporterName = "foo",
                reporterEmail = Email("foo@bar.com"),
                error = "yes error",
            ).trim(),
        )
    }
}
