package incoming

import (
	"bytes"
	"errors"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
	"github.com/stretchr/testify/assert"
	"io"
	"strings"
	"testing"
)

func TestHandleConnectionSunnyDay(t *testing.T) {
	testCases := []struct {
		name         string
		input        string
		expectedLogs []string
		shouldAccept bool
	}{
		{
			name: "Simple email exchange",
			input: `LHLO localhost
MAIL FROM:<sender@example.com>
RCPT TO:<receiver@example.com>
DATA
Hello, this is a test email.
.
QUIT
`,
			expectedLogs: []string{
				"250-localhost",
				"250 SIZE 10240000",
				"250 8BITMIME",
				"250 OK",
				"250 OK",
				"354 End data with <CR><LF>.<CR><LF>",
				"250 email queued",
				"221 Bye",
			},
			shouldAccept: true,
		},
		{
			name: "Recipient not accepted",
			input: `LHLO localhost
MAIL FROM:<sender@example.com>
RCPT TO:<nonexistent@example.com>
DATA
Hello, this is a test email.
.
QUIT
`,
			expectedLogs: []string{
				"250-localhost",
				"250 SIZE 10240000",
				"250 8BITMIME",
				"250 OK",
				"250 OK",
				"354 End data with <CR><LF>.<CR><LF>",
				"550 nobody here with that email",
				"221 Bye",
			},
			shouldAccept: false,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			buffer := bytes.Buffer{}
			rw := &mockReadWriter{Reader: strings.NewReader(tc.input), Writer: &buffer}

			server := NewLMTPServer("", func(message *tachikoma.IncomingEmailMessage) (bool, error) {
				return tc.shouldAccept, nil
			})

			server.handleConnection(rw)

			for _, expected := range tc.expectedLogs {
				output := buffer.String()
				assert.Contains(t, output, expected)
				buffer.Reset()
			}
		})
	}
}

func TestHandleConnectionEdgeCases(t *testing.T) {
	testCases := []struct {
		name                   string
		input                  string
		expectedLogs           []string
		onIncomingMessageError error
		shouldAccept           bool
	}{
		{
			name: "CRLF End Of Line",
			input: `LHLO localhost\r
MAIL FROM:<sender@example.com>\r
RCPT TO:<receiver@example.com>\r
DATA\r
Hello, this is a test email.\r
.\r
QUIT\r
`,
			expectedLogs: []string{
				"250-localhost",
				"250 SIZE 10240000",
				"250 8BITMIME",
				"250 OK",
				"250 OK",
				"354 End data with <CR><LF>.<CR><LF>",
				"250 email queued",
				"221 Bye",
			},
			shouldAccept: true,
		},
		{
			name: "Connection closes halfway",
			input: `LHLO localhost
MAIL FROM:<sender@example.com>
RCPT TO:<receiver@example.com>
DATA
Hello, this is a test email.
.
`,
			expectedLogs: []string{
				"250-localhost",
				"250 SIZE 10240000",
				"250 8BITMIME",
				"250 OK",
				"250 OK",
				"354 End data with <CR><LF>.<CR><LF>",
			},
			shouldAccept: true,
		},
		{
			name: "onIncomingMessage returns error",
			input: `LHLO localhost
MAIL FROM:<sender@example.com>
RCPT TO:<receiver@example.com>
DATA
Hello, this is a test email.
.
QUIT
`,
			expectedLogs: []string{
				"250-localhost",
				"250 SIZE 10240000",
				"250 8BITMIME",
				"250 OK",
				"250 OK",
				"354 End data with <CR><LF>.<CR><LF>",
				"451 Unable to forward email: internal error",
				"221 Bye",
			},
			onIncomingMessageError: errors.New("internal error"),
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			buffer := bytes.Buffer{}
			rw := &mockReadWriter{Reader: strings.NewReader(tc.input), Writer: &buffer}

			server := NewLMTPServer("/tmp/socket", func(message *tachikoma.IncomingEmailMessage) (bool, error) {
				if tc.onIncomingMessageError != nil {
					return false, tc.onIncomingMessageError
				}
				return tc.shouldAccept, nil
			})

			server.handleConnection(rw)

			for _, expected := range tc.expectedLogs {
				output := buffer.String()
				assert.Contains(t, output, expected)
				buffer.Reset()
			}
		})
	}
}

type mockReadWriter struct {
	Reader io.Reader
	Writer io.Writer
}

func (rw *mockReadWriter) Read(p []byte) (n int, err error) {
	return rw.Reader.Read(p)
}

func (rw *mockReadWriter) Write(p []byte) (n int, err error) {
	return rw.Writer.Write(p)
}

func (rw *mockReadWriter) Close() error {
	return nil
}
