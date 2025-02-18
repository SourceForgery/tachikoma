package mailer

import (
    "context"
    "github.com/pkg/errors"
    "github.com/rs/zerolog/log"
    "net/smtp"
    "tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
)

type Mailer struct {
}

func (mailer *Mailer) Listen(ctx context.Context, outgoingEmail <-chan tachikoma.EmailMessageOrKeepAlive) {
    for {
        select {
        case <-ctx.Done():
            log.Info().Msg("Context closed, exiting Listener loop")
            return
        case email, ok := <-outgoingEmail:
            if !ok {
                log.Info().Msg("OutgoingEmail channel closed, exiting Listener loop")
                return
            }
            if emailMessage := email.GetEmailMessage(); emailMessage != nil {
                sendEmail, err := mailer.sendEmail(ctx, emailMessage)
                if err != nil {
                    return
                }
            }
        }
    }
}

func (mailer *Mailer) sendEmail(ctx context.Context, emailMessage *tachikoma.EmailMessage) (string, error) {

    client, err := smtp.Dial("localhost:25")
    if err != nil {
        return "", errors.Wrap(err, "Failed to connect to localhost:25")
    }
    client.R
    //goland:noinspection GoUnhandledErrorResult
    defer client.Close()

    if err = client.Hello("localhost"); err != nil {
        return "", err
    }

    // Set the sender and recipient.
    if err = client.Mail(emailMessage.From); err != nil {
        return "", err
    }

    if err = client.Rcpt(emailMessage.GetEmailAddress()); err != nil {
        return "", err
    }
    for _, r := range emailMessage.GetBcc() {
        if err = client.Rcpt(r); err != nil {
            return "", err
        }
    }

    _, err = client.Text.Cmd("DATA")
    if err != nil {
        return "", err
    }
    client.Text.DotWriter()

    // Send the email body.
    wc, err := client.Data()
    if err != nil {
        return "", err
    }
    _, err = wc.Write([]byte(emailMessage.GetBody()))
    if err != nil {
        return "", err
    }

    err = wc.Close()
    if err != nil {
        return "", err
    }
    queueId := ""
    // Get queueId from the rest of the line after 250
    // The line is regex "^250 .* queued as (.*)$". I want group 1 as queueId

    // Send the QUIT command and close the connection.
    err = client.Quit()
    if err != nil {
        return "", err
    }

    return queueId, nil
}
