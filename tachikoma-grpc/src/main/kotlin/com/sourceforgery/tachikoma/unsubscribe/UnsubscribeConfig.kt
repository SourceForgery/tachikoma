package com.sourceforgery.tachikoma.unsubscribe

import com.sourceforgery.tachikoma.identifiers.MailDomain

interface UnsubscribeConfig {
    /**
     * Used to override the domain in the from address with another domain
     * so that unsubscribe emails and bounce emails will get bounced
     * to a tachikoma-connected mail server.
     *
     * E.g. sending emails from password-reset@company.com but only receiving
     * emails to tachikoma on the domain *@tachikoma.company.com.
     * Setting this to *@tachikoma.company.com would make email unsubscribes work.
     */
    val unsubscribeDomainOverride: MailDomain?
}
