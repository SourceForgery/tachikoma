package com.sourceforgery.tachikoma.maildelivery

import com.sourceforgery.tachikoma.common.Email
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class ExtractBodyKtTest {

    @Test
    fun `extract body 1`() {
        val email =
            """
            |Has done the following!
            |
            |FOOO:     0 (1)
            |BAR: false (true)
            |FOO:       false (true)
            |FOOBAR: false (true)
            |
            |Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut
            |labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris
            |nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
            |cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui
            |officia deserunt mollit anim id est laborum.
            |
            |PING!
            |
            |--
            |Phone: +1800555555
            |GTalk: foobar@example.com
            |
            |On Fri, Feb 3, 2017 at 9:12 PM, Foo Bar <barfoo@example.net> wrote:
            |
            |> Lorem ispum
            |> Lorem ipsum
            |>
            """.trimMargin()
        assertEquals(
            """
                |Has done the following!
                |
                |FOOO:     0 (1)
                |BAR: false (true)
                |FOO:       false (true)
                |FOOBAR: false (true)
                |
                |Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut
                |labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris
                |nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
                |cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui
                |officia deserunt mollit anim id est laborum.
                |
                |PING!
                |
                |--
                |Phone: +1800555555
                |GTalk: foobar@example.com
            """.trimMargin(),
            extractBodyFromPlaintextEmail(email, Email("barfoo@example.net"))
        )
    }

    @Test
    fun `extract body 2`() {
        val email =
            """
                |Hey Foo,
                |
                |
                |
                |So sad.
                |
                |
                |
                |So happy.
                |
                |
                |
                |Best Regards
                |Foobar
                |
                |Foo Customer Service
                |Some 123, 123 45 Foobar
                |Foobar 74, 123 45 Foobar, 7th floor
                |Tel. +15558800000
                |Open all days..
                |www.example.com (www.example.com
                |
                |( http://www.example.com )
                |
                |( https://www.example.com/foobar )
                |
                |Follow us
                |( http://www.facebook.com/example )( https://www.instagram.com/example/ )
                |
                |
                |
                |Foooooo Baaaaar <foobar@example.net>
                |*Errand:* 9999999-1
                |*Sent:* 2020-01-01, 07:03
                |*To:* * Foo <customer.support@foo.com>
            """.trimMargin()
        assertEquals(
            """
                |Hey Foo,
                |
                |
                |So sad.
                |
                |
                |So happy.
                |
                |
                |Best Regards
                |Foobar
                |
                |Foo Customer Service
                |Some 123, 123 45 Foobar
                |Foobar 74, 123 45 Foobar, 7th floor
                |Tel. +15558800000
                |Open all days..
                |www.example.com (www.example.com
                |
                |( http://www.example.com )
                |
                |( https://www.example.com/foobar )
                |
                |Follow us
                |( http://www.facebook.com/example )( https://www.instagram.com/example/ )
            """.trimMargin(),
            extractBodyFromPlaintextEmail(email, Email("foobar@example.net"))
        )
    }
}
