package com.sourceforgery.tachikoma.rest

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.MediaTypeNames
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Produces
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.stream.createHTML
import kotlinx.html.title
import kotlinx.html.ul
import org.kodein.di.DI
import org.kodein.di.DIAware

class RootPage(override val di: DI) : RestService, DIAware {
    @Get("/")
    @Produces(MediaTypeNames.HTML_UTF_8)
    fun rootPage(httpRequest: HttpRequest): String {
        val htmlDoc = createHTML()
        htmlDoc.head {
            title = "Abuse reporting"
        }
        htmlDoc.body {
            h1 { text("Tachikoma ESP") }
            br()
            text("This is the Tachikoma ESP (Email Service Provider) running at ${httpRequest.headers().authority()}.")
            br()
            text("Current features include:")
            br()
            ul {
                li { text("Handle relatively large amount of emails (sub 100k/month)") }

                li { text("Accurately track bounce, delivers, opens & clicks") }
                li {
                    text("Handle unsubscribe properly via replacing links in the email and")
                    link("https://tools.ietf.org/html/rfc8058") { text("RFC8058") }
                }
                li { text("Block lists for unsubscribed emails (per sender email)") }
                li { text("Zero Downtime upgrades for web server") }
                li { text("No messages lost, not even during upgrade") }
                li { text("Queue outgoing emails until a specific time") }
            }
            text("To get involved with the project, please go to")
            a("https://github.com/SourceForgery/tachikoma") { text("https://github.com/SourceForgery/tachikoma") }
        }
        return htmlDoc.finalize()
    }
}
