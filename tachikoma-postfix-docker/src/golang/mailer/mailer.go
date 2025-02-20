package mailer

import (
    "bufio"
    "context"
    "fmt"
    "github.com/rs/zerolog/log"
    "google.golang.org/grpc"
    "net"
    "net/textproto"
    "strings"
    "tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
)

type Mailer struct {
    queueCLient tachikoma.MTAEmailQueueClient
}

func NewMailer(client *grpc.ClientConn) *Mailer {
    notificationClient := tachikoma.NewMTAEmailQueueClient(client)
    return &Mailer{
        queueCLient: notificationClient,
    }
}

func (mailer *Mailer) Listen(ctx context.Context, outgoingEmail <-chan tachikoma.EmailMessageOrKeepAlive, notificationChannel chan<- *tachikoma.MTAQueuedNotification) {
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
                notification := &tachikoma.MTAQueuedNotification{
                    Success: true,
                    EmailId: emailMessage.GetEmailId(),
                }
                queueId, err := mailer.sendEmail(ctx, emailMessage)
                if err != nil {
                    notification.Success = false
                } else {
                    notification.QueueId = queueId
                }
                notificationChannel <- notification
            }
        }
    }
}

// sendMail connects to the SMTP server at 'server' (e.g. "smtp.example.com:25"),
// sends the email from 'sender' to all 'recipients' with the given 'data' as the full message.
// It returns the queue id (if found) from the DATA command response.
func (mailer *Mailer) sendEmail(ctx context.Context, emailMessage *tachikoma.EmailMessage) (string, error) {
    conn, err := net.Dial("tcp", "localhost:25")
    if err != nil {
        return "", fmt.Errorf("failed to dial SMTP server: %v", err)
    }
    //goland:noinspection GoUnhandledErrorResult
    defer conn.Close()

    tp := textproto.NewConn(conn)
    //goland:noinspection GoUnhandledErrorResult
    defer tp.Close()

    if _, msg, err := tp.ReadResponse(220); err != nil {
        return "", fmt.Errorf("error reading greeting: %v", err)
    } else {
        fmt.Printf("Server greeting: %s\n", msg)
    }

    hostname := "localhost"
    if err := tp.PrintfLine("EHLO %s", hostname); err != nil {
        return "", fmt.Errorf("error sending EHLO: %v", err)
    }
    if _, msg, err := tp.ReadResponse(250); err != nil {
        return "", fmt.Errorf("EHLO error: %v", err)
    } else {
        fmt.Printf("EHLO response: %s\n", msg)
    }

    if err := tp.PrintfLine("MAIL FROM:<%s>", emailMessage.From); err != nil {
        return "", fmt.Errorf("error sending MAIL FROM: %v", err)
    }
    if _, msg, err := tp.ReadResponse(250); err != nil {
        return "", fmt.Errorf("MAIL FROM error: %v", err)
    } else {
        fmt.Printf("MAIL FROM response: %s\n", msg)
    }

    for _, rcpt := range emailMessage.GetBcc() {
        if err := tp.PrintfLine("RCPT TO:<%s>", rcpt); err != nil {
            return "", fmt.Errorf("error sending RCPT TO for %s: %v", rcpt, err)
        }
        if _, msg, err := tp.ReadResponse(250); err != nil {
            return "", fmt.Errorf("RCPT TO error for %s: %v", rcpt, err)
        } else {
            fmt.Printf("RCPT TO response for %s: %s\n", rcpt, msg)
        }
    }

    if err := tp.PrintfLine("DATA"); err != nil {
        return "", fmt.Errorf("error sending DATA command: %v", err)
    }
    if _, msg, err := tp.ReadResponse(354); err != nil {
        return "", fmt.Errorf("DATA command error: %v", err)
    } else {
        fmt.Printf("DATA command response: %s\n", msg)
    }

    scanner := bufio.NewScanner(strings.NewReader(emailMessage.GetBody()))
    for scanner.Scan() {
        line := scanner.Text()
        if strings.HasPrefix(line, ".") {
            line = "." + line
        }
        if err := tp.PrintfLine("%s", line); err != nil {
            return "", fmt.Errorf("error sending data line: %v", err)
        }
    }
    if err := scanner.Err(); err != nil {
        return "", fmt.Errorf("scanner error: %v", err)
    }

    if err := tp.PrintfLine("."); err != nil {
        return "", fmt.Errorf("error sending DATA termination: %v", err)
    }

    _, dataResp, err := tp.ReadResponse(250)
    if err != nil {
        return "", fmt.Errorf("error after sending DATA: %v", err)
    }
    fmt.Printf("DATA final response: %s\n", dataResp)

    // Try to extract the queue id from the response.
    // Postfix often replies with something like:
    // "250 2.0.0 Ok: queued as ABC1234"
    var queueID string
    if parts := strings.Split(dataResp, "queued as "); len(parts) > 1 {
        queueID = strings.TrimSpace(parts[1])
    }

    if err := tp.PrintfLine("QUIT"); err != nil {
        return queueID, fmt.Errorf("error sending QUIT: %v", err)
    }
    _, _, _ = tp.ReadResponse(221)

    return queueID, nil
}
