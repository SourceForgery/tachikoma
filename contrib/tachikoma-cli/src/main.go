package main

import (
	"context"
	"crypto/tls"
	"fmt"
	"github.com/rs/zerolog/pkgerrors"
	"google.golang.org/grpc/credentials"
	"net/url"
	"os"
	"time"

	"github.com/jessevdk/go-flags"
	"github.com/rs/zerolog"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
)

var logger zerolog.Logger

type Options struct {
	Address         string           `short:"a" long:"address" description:"Address of the tachikoma server e.g. https://example.com" required:"true"`
	LoggingFormat   string           `short:"l" long:"logging" choice:"coloured" choice:"plain" choice:"json" default:"coloured" description:"Log output format"`
	Verbose         []bool           `short:"v" long:"verbose" description:"Show verbose debug information"`
	Quiet           bool             `short:"q" long:"quiet" description:"Be very quiet"`
	CertificatePath string           `short:"c" long:"certificate" description:"Path to client certificate"`
	KeyPath         string           `short:"k" long:"key" description:"Path to Key"`
	AddUserCommand  AddUserCmd       `command:"add-user" description:"Create new user"`
	ListUsers       struct{}         `command:"list-users" description:"List users user"`
	RemoveUser      RemoveUserCmd    `command:"remove-users" description:"Remove users user"`
	ModifyUser      ModifyUserCmd    `command:"modify-user" description:"Modify user"`
	SendTestEmail   SendTestEmailCmd `command:"send-test-email" description:"Send test email"`
}

func main() {
	var opts Options
	parser := flags.NewParser(&opts, flags.Default)

	_, err := parser.Parse()
	if err != nil {
		logger.Fatal().Err(err).Msg("Failed to parse options")
	}

	tachikomaUrl, err := url.Parse(opts.Address)
	if err != nil {
		logger.Fatal().Err(err).Msg("Failed to parse address")
	}

	zerolog.ErrorStackMarshaler = pkgerrors.MarshalStack
	switch opts.LoggingFormat {
	case "json":
		logger = zerolog.New(os.Stdout)
		break
	case "coloured":
		logger = zerolog.New(zerolog.ConsoleWriter{Out: os.Stderr, TimeFormat: time.RFC3339})
		break
	case "plain":
		logger = zerolog.New(zerolog.ConsoleWriter{Out: os.Stderr, TimeFormat: time.RFC3339, NoColor: true})
		break
	default:
		logger.Panic().Msgf("What the f is %s", opts.LoggingFormat)
	}
	logger = logger.With().Timestamp().Logger()

	if (opts.CertificatePath != "") != (opts.KeyPath != "") {
		logger.Fatal().Msg("Either specify certificate and key or neither")
	}
	var creds credentials.TransportCredentials
	if opts.CertificatePath != "" {
		var cert tls.Certificate
		cert, err = tls.LoadX509KeyPair(opts.CertificatePath, opts.KeyPath)
		if err != nil {
			logger.Fatal().Err(err).Msgf("failed to load client certificates: '%s' and '%s'", opts.CertificatePath, opts.KeyPath)
		}
		creds = credentials.NewTLS(&tls.Config{
			Certificates: []tls.Certificate{cert},
		})
	} else {
		creds = credentials.NewClientTLSFromCert(nil, "")
	}

	port := "443"
	if tachikomaUrl.Port() != "" {
		port = tachikomaUrl.Port()
	}
	host := fmt.Sprintf("%s:%s", tachikomaUrl.Host, port)
	logger.Info().Msgf("Connecting to server at %s", host)
	grpcConn, err := grpc.NewClient(host, grpc.WithTransportCredentials(creds))

	if err != nil {
		logger.Fatal().Err(err).Msg("Failed to connect to server")
	}
	defer func(conn *grpc.ClientConn) {
		_ = conn.Close()
	}(grpcConn)

	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	md := metadata.New(map[string]string{
		"x-apitoken": tachikomaUrl.User.String(),
	})
	ctx = metadata.NewOutgoingContext(ctx, md)

	mailDomain := tachikomaUrl.User.Username()
	switch parser.Active.Name {
	case "add-user":
		addUser(ctx, grpcConn, mailDomain, opts.AddUserCommand)
	case "list-users":
		listUsers(ctx, grpcConn, mailDomain)
	case "remove-users":
		removeUser(ctx, grpcConn, opts.RemoveUser)
	case "modify-user":
		modifyUser(ctx, grpcConn, opts.ModifyUser)
	case "send-test-email":
		sendTestEmail(ctx, grpcConn, opts.SendTestEmail)
	default:
		logger.Panic().Msgf("Didn't cater to %s", parser.Active.Name)
	}
}

//go:generate mkdir -p build/generated
//go:generate protoc --go_out=build/generated --go-grpc_out=build/generated -I=../tachikoma-frontend-api-proto/src/main/proto/ ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/frontend/maildelivery/maildelivery.proto ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/frontend/tracking/mailtracking.proto ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/frontend/common.proto ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/frontend/auth/auth.proto ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/admin/users/users.proto ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/admin/blockedemail/blockedemail.proto
