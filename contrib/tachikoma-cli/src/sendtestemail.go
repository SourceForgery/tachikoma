package main

import (
	"context"
	"google.golang.org/grpc"
	"io"
	"sourceforgery.com/tachikoma-cli/build/generated/github.com/SourceForgery/tachikoma"
	"strings"
	"time"
)

type SendTestEmailCmd struct {
	From      string `long:"from" required:"yes" description:"From address of the test email"`
	Recipient string `long:"to" required:"yes" description:"Recipient of the test email"`
	Body      string `long:"body" required:"yes" description:"Body of the test email"`
}

func sendTestEmail(ctx context.Context, grpcConn *grpc.ClientConn, cmd SendTestEmailCmd) {
	client := tachikoma.NewMailDeliveryServiceClient(grpcConn)
	domain := strings.Split(cmd.From, "@")[1]
	req := &tachikoma.OutgoingEmail{
		From: &tachikoma.NamedEmailAddress{
			Email: cmd.From,
			Name:  "Tachikoma CLI",
		},
		Recipients: []*tachikoma.EmailRecipient{
			{
				NamedEmail: &tachikoma.NamedEmailAddress{
					Email: cmd.Recipient,
					Name:  "",
				},
			},
		},
		Body: &tachikoma.OutgoingEmail_Static{
			Static: &tachikoma.StaticBody{
				PlaintextBody: "This is a test email sent at " + time.Now().Format(time.RFC3339),
				Subject:       "Test email " + time.Now().Format(time.RFC3339),
			},
		},
		SigningDomain: domain,
	}
	result, err := client.SendEmail(ctx, req)
	if err != nil {
		logger.Fatal().Err(err).Msgf("Failed to send email to %s", cmd.Recipient)
	}

	for {
		var status *tachikoma.EmailQueueStatus
		status, err = result.Recv()
		if err == io.EOF {
			break
		} else if err != nil {
			logger.Fatal().Err(err).Msgf("Failed to send email to %s", cmd.Recipient)
		}
		logger.Info().Any("status", status).Msg("Received status")
	}
	logger.Info().Any("result", result).Msgf("Sent email to %s", cmd.Recipient)
}
