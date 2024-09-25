package incoming

import (
	"context"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/src/common"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/src/zerlogger"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"google.golang.org/grpc"
)

type IncomingEmailHandler struct {
	client tachikoma.MTAEmailQueueClient
	ctx    context.Context
}

var logger zerolog.Logger

func init() {
	logger = log.Logger.Hook(&zerlogger.LoggerNameHook{"incoming"})
}

func NewIncomingEmailHandler(grpc *grpc.ClientConn, addAuth common.AddAuth) *IncomingEmailHandler {
	client := tachikoma.NewMTAEmailQueueClient(grpc)
	ctx := addAuth(context.Background())
	return &IncomingEmailHandler{
		client: client,
		ctx:    ctx,
	}
}

func (h *IncomingEmailHandler) Send(message *tachikoma.IncomingEmailMessage) (accepted bool, err error) {
	email, err := h.client.IncomingEmail(h.ctx, message)
	if err != nil {
		return
	}
	switch email.AcceptanceStatus {
	case tachikoma.MailAcceptanceResult_ACCEPTED:
		accepted = true
	case tachikoma.MailAcceptanceResult_IGNORED:
		logger.Info().Msgf("Email from %s ignored", message.From)
		accepted = true
	}
	return
}
