package syslog

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestParseGoodLine1(t *testing.T) {
	line := "Jan 18 22:55:46 1c7326acd8e5 postfix/smtp[249]: 2D61E2A03: to=<test@example.com>, relay=none, delay=30, delays=0.01/0/30/0, dsn=4.4.1, status=deferred (connect to example.com[93.184.216.34]:25: Connection timed out)"
	notification := parseLine(line)
	assert.NotNil(t, notification)
	assert.Equal(t, "test@example.com", notification.OriginalRecipient)
	assert.Equal(t, "2D61E2A03", notification.QueueId)
	assert.Equal(t, "4.4.1", notification.Status)
	assert.Equal(t, "deferred", notification.Reason)
	expectedDiagnoseText := "deferred (connect to example.com[93.184.216.34]:25: Connection timed out)"
	assert.Equal(t, expectedDiagnoseText, notification.DiagnoseText)
}

func TestParseBadLines(t *testing.T) {
	badLines := []string{
		"2021-01-07T07:59:02.970439+00:00 m postfix/anvil[18978]: statistics: max connection count 1 for (smtp:103.133.109.40) at Jan  7 07:55:41",
		"2021-01-07T07:59:02.970428+00:00 m postfix/anvil[18978]: statistics: max connection rate 1/60s for (smtp:103.133.109.40) at Jan  7 07:55:41",
		"2021-01-07T06:04:21.870406+00:00 m postfix/anvil[17260]: statistics: max cache size 1 at Jan  7 06:01:01",
		"2021-01-06T19:52:37.498401+00:00 m postfix/smtpd[8682]: warning: hostname zg-1218a-137.stretchoid.com does not resolve to address 192.241.206.190: Name or service not known",
		"2021-01-06T16:55:28.969848+00:00 m postfix/smtpd[6107]: NOQUEUE: reject: RCPT from unknown[218.1.18.154]: 454 4.7.1 <spameri@tiscali.it>: Relay access denied; from=<spameri@tiscali.it> to=<spameri@tiscali.it> proto=ESMTP helo=<AB-201803070904>",
	}

	for _, line := range badLines {
		assert.Nil(t, parseLine(line), "Expected no notification to be parsed from line: %s", line)
	}
}
