package incoming

import (
	"errors"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/src/common"
	"github.com/stretchr/testify/assert"
	"strings"
	"testing"
)

type testcase struct {
	name                   string
	shouldAccept           bool
	exchange               string
	onIncomingMessageError error
}

func TestHandleConnectionSunnyDay(t *testing.T) {
	testCases := []testcase{
		{
			name: "Simple email exchange",
			exchange: `
> LHLO localhost
< 250-localhost
< 250 SIZE 10240000
< 250 8BITMIME
> MAIL FROM:<sender@example.com>
< 250 OK
> RCPT TO:<receiver@example.com>
< 250 OK
> DATA
< 354 End data with <CR><LF>.<CR><LF>
> Hello, this is a test email.
> .
< 250 email queued
> QUIT
< 221 Bye
`,
			shouldAccept: true,
		},
		{
			name: "Recipient not accepted",
			exchange: `
> LHLO localhost
< 250-localhost
< 250 SIZE 10240000
< 250 8BITMIME
> MAIL FROM:<sender@example.com>
< 250 OK
> RCPT TO:<nonexistent@example.com>
< 250 OK
> DATA
< 354 End data with <CR><LF>.<CR><LF>
> Hello, this is a test email.
> .
< 550 nobody here with that email
`,
			shouldAccept: false,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			rw := common.NewChannelReadWriteCloser()
			//goland:noinspection GoUnhandledErrorResult
			defer rw.Close()
			server := NewLMTPServer("", func(message *tachikoma.IncomingEmailMessage) (bool, error) {
				return tc.shouldAccept, nil
			})

			go server.handleConnection(rw)

			testMethod(t, tc, rw)
		})
	}
}

func testMethod(t *testing.T, tc testcase, rw *common.ChannelReadWriteCloser) {
	for _, s := range strings.Split(tc.exchange, "\n") {
		if strings.HasPrefix(s, "> ") {
			rw.GetReadChan() <- strings.TrimPrefix(s, "> ") + "\r\n"
		} else if strings.HasPrefix(s, "< ") {
			expected := strings.TrimPrefix(s, "< ")
			actual := <-rw.GetWriteChan()
			assert.Equal(t, expected, actual)
		} else if strings.TrimSpace(s) != "" {
			t.Fatalf("unexpected line: %s", s)
		}
	}
}

func TestHandleConnectionEdgeCases(t *testing.T) {
	testCases := []testcase{
		{
			name: "Connection closes halfway",
			exchange: `
> LHLO localhost
< 250-localhost
< 250 SIZE 10240000
< 250 8BITMIME
> MAIL FROM:<sender@example.com>
< 250 OK
> RCPT TO:<receiver@example.com>
< 250 OK
> DATA
< 354 End data with <CR><LF>.<CR><LF>
> Hello, this is a test email.
> .
`,
			shouldAccept: true,
		},
		{
			name: "onIncomingMessage returns error",
			exchange: `
> LHLO localhost
< 250-localhost
< 250 SIZE 10240000
< 250 8BITMIME
> MAIL FROM:<sender@example.com>
< 250 OK
> RCPT TO:<receiver@example.com>
< 250 OK
> DATA
< 354 End data with <CR><LF>.<CR><LF>
> Hello, this is a test email.
> .
< 451 Unable to forward email: internal error
> QUIT
< 221 Bye
`,
			onIncomingMessageError: errors.New("internal error"),
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			rw := common.NewChannelReadWriteCloser()
			//goland:noinspection GoUnhandledErrorResult
			defer rw.Close()
			server := NewLMTPServer("/tmp/socket", func(message *tachikoma.IncomingEmailMessage) (bool, error) {
				if tc.onIncomingMessageError != nil {
					return false, tc.onIncomingMessageError
				}
				return tc.shouldAccept, nil
			})

			go server.handleConnection(rw)
			testMethod(t, tc, rw)
		})
	}
}

func TestIsValidLHLO(t *testing.T) {

	testCases := []struct {
		command  string
		expected bool
	}{
		{"LHLO example.com", true},
		{"LHLO 192.168.1.1", true},
		{"LHLO sub.example.co.uk", true},
		{"LHLO invalid_domain", false},
		{"LHLO example,com", false},
		{"EHLO example.com", false},
		{"LHLO- extra_whitespace", false},
	}

	for _, testCase := range testCases {
		assert.Equal(t, testCase.expected, isValidLHLO(testCase.command), "Command: %s", testCase.command)
	}
}
