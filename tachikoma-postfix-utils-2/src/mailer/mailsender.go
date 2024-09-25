package main

import (
	"crypto/tls"
	"fmt"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
	"net/smtp"
	"regexp"
)

var (
	getMailqId = regexp.MustCompile(`.* queued as (.*)$`)
)

// Copied from smtp.go
// cmd is a convenience function that sends a command and returns the response
func cmd(c *smtp.Client, expectCode int, format string, args ...any) (int, string, error) {
	id, err := c.Text.Cmd(format, args...)
	if err != nil {
		return 0, "", err
	}
	c.Text.StartResponse(id)
	defer c.Text.EndResponse(id)
	code, msg, err := c.Text.ReadResponse(expectCode)
	return code, msg, err
}

func ssmtpSendEmail(emailMessage *tachikoma.EmailMessage) (queueId string, err error) {

	// Connect to the server
	conn, err := tls.Dial("tcp", "localhost:25", nil)
	if err != nil {
		return "", fmt.Errorf("failed to connect to server: %w", err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer conn.Close()

	client, err := smtp.NewClient(conn, "localhost")
	if err != nil {
		return "", fmt.Errorf("failed to create client: %w", err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer client.Quit()

	// Perform EHLO
	if err = client.Hello("localhost"); err != nil {
		return "", fmt.Errorf("failed to say EHLO: %w", err)
	}

	// Set the sender
	if err = client.Mail(emailMessage.From); err != nil {
		return "", fmt.Errorf("failed to set sender: %w", err)
	}

	// Set the recipients
	if err = client.Rcpt(emailMessage.EmailAddress); err != nil {
		return "", fmt.Errorf("failed to set recipient: %w", err)
	}

	for _, bcc := range emailMessage.Bcc {
		if err = client.Rcpt(bcc); err != nil {
			return "", fmt.Errorf("failed to set BCC recipient %s: %w", bcc, err)
		}
	}

	// Start data command
	w, err := Data(client)
	if err != nil {
		return "", fmt.Errorf("failed to start data command: %w", err)
	}

	// Send the email body
	_, err = w.Write([]byte(emailMessage.Body))
	if err != nil {
		return "", fmt.Errorf("failed to write body: %w", err)
	}

	// Close data command
	err = w.Close()
	if err != nil {
		return "", fmt.Errorf("failed to close data command: %w", err)
	}

	// Quit the SMTP server
	err = client.Quit()
	if err != nil {
		return "", fmt.Errorf("failed to quit: %w", err)
	}

	queueId = getMailqId.FindStringSubmatch(emailMessage.Body)[1]
	return
}
