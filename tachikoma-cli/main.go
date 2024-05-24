package main

import (
	"context"
	"crypto/tls"
	"fmt"
	"github.com/rs/zerolog/pkgerrors"
	"google.golang.org/grpc/credentials"
	"io"
	"net/url"
	"os"
	"sourceforgery.com/tachikoma-cli/build/generated/github.com/SourceForgery/tachikoma"
	"time"

	"github.com/jessevdk/go-flags"
	"github.com/rs/zerolog"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
)

var logger zerolog.Logger

type Options struct {
	Address         string        `short:"a" long:"address" description:"Address of the tachikoma server e.g. https://example.com" required:"true"`
	LoggingFormat   string        `short:"l" long:"logging" choice:"coloured" choice:"plain" choice:"json" default:"coloured" description:"Log output format"`
	Verbose         []bool        `short:"v" long:"verbose" description:"Show verbose debug information"`
	Quiet           bool          `short:"q" long:"quiet" description:"Be very quiet"`
	CertificatePath string        `short:"c" long:"certificate" description:"Path to client certificate"`
	KeyPath         string        `short:"k" long:"key" description:"Path to Key"`
	AddUserCommand  AddUserCmd    `command:"add-user" description:"Create new user"`
	ListUsers       struct{}      `command:"list-users" description:"List users user"`
	RemoveUser      RemoveUserCmd `command:"remove-users" description:"Remove users user"`
	ModifyUser      ModifyUserCmd `command:"modify-user" description:"Modify user"`
}

type RemoveUserCmd struct {
	UserId int64 `positional-args:"yes" required:"yes" description:"User IDs to remove"`
}

type AddUserCmd struct {
	User           string `long:"username" description:"Username for the new user"`
	Password       string `long:"password" description:"Password for the new user"`
	CreateApiToken bool   `long:"api-token" description:"If API token should be created"`
	AdminRole      bool   `long:"admin-role" description:"If this is an admin role"`
	OverrideEmail  string `long:"override-email" description:"Override recipient with this email for all emails"`
}

type ModifyUserCmd struct {
	UserId        int64   `long:"userid" description:"Userid for the new user"`
	Password      string  `long:"password" description:"Password for the new user"`
	ApiToken      string  `long:"api-token" choice:"remove" choice:"reset" choice:"keep" default:"keep" description:"If API token should be modified"`
	AdminRole     bool    `long:"admin-role" description:"If this is an admin role"`
	Active        string  `long:"active" choice:"keep" choice:"true" choice:"false" default:"keep" description:"If the user is active"`
	OverrideEmail *string `long:"override-email" description:"Override recipient with this email for all emails"`
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
	default:
		logger.Panic().Msgf("Didn't cater to %s", parser.Active.Name)
	}
}

func addUser(ctx context.Context, grpcConn *grpc.ClientConn, mailDomain string, createUserCmd AddUserCmd) {
	client := tachikoma.NewUserServiceClient(grpcConn)

	var passwordAuth *tachikoma.PasswordAuth
	if createUserCmd.User != "" || createUserCmd.Password != "" {
		if createUserCmd.User == "" || createUserCmd.Password == "" {
			logger.Fatal().Msg("Both username and password must be provided, or neither")
		}
		passwordAuth = &tachikoma.PasswordAuth{
			Login:    createUserCmd.User,
			Password: createUserCmd.Password,
		}
	} else if !createUserCmd.CreateApiToken {
		logger.Fatal().Msg("Either username+password or create api token must be specified")
	}

	var role tachikoma.FrontendUserRole
	if createUserCmd.AdminRole {
		role = tachikoma.FrontendUserRole_FRONTEND_ADMIN
	} else {
		role = tachikoma.FrontendUserRole_FRONTEND
	}

	var overrideEmail *tachikoma.EmailAddress
	if createUserCmd.OverrideEmail != "" {
		overrideEmail = &tachikoma.EmailAddress{
			Email: createUserCmd.OverrideEmail,
		}
	}

	user, err := client.AddFrontendUser(ctx, &tachikoma.AddUserRequest{
		Active:             true,
		PasswordAuth:       passwordAuth,
		AddApiToken:        createUserCmd.CreateApiToken,
		AuthenticationRole: role,
		MailDomain:         mailDomain,
		RecipientOverride:  overrideEmail,
	})
	if err != nil {
		logger.Fatal().Err(err).Msg("Failed to create user")
	}
	logger.Info().
		Str("apitoken", user.GetApiToken()).
		Msgf("Successfully created user %d", user.GetUser().GetAuthId().GetId())
}

func listUsers(ctx context.Context, grpcConn *grpc.ClientConn, mailDomain string) {
	client := tachikoma.NewUserServiceClient(grpcConn)
	req := &tachikoma.GetUsersRequest{
		MailDomain: mailDomain,
	}
	stream, err := client.GetFrontendUsers(ctx, req)
	if err != nil {
		logger.Fatal().Err(err).Msg("Failed to get frontend users")
	}
	for {
		user, err := stream.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			logger.Fatal().Err(err).Msg("Failed to receive frontend user")
		}
		logger.Info().Any("result", user).Msg("Found user")
	}
}

func removeUser(ctx context.Context, grpcConn *grpc.ClientConn, cmd RemoveUserCmd) {
	client := tachikoma.NewUserServiceClient(grpcConn)
	req := &tachikoma.RemoveUserRequest{
		UserToRemove: &tachikoma.UserId{Id: cmd.UserId},
	}
	result, err := client.RemoveUser(ctx, req)
	if err != nil {
		logger.Fatal().Err(err).Msgf("Failed to remove user %d", cmd.UserId)
	}
	logger.Info().Any("result", result).Msg("Removed user")
}

func modifyUser(ctx context.Context, grpcConn *grpc.ClientConn, cmd ModifyUserCmd) {
	client := tachikoma.NewUserServiceClient(grpcConn)
	req := &tachikoma.ModifyUserRequest{
		AuthId: &tachikoma.UserId{
			Id: cmd.UserId,
		},
		ApiToken:                tachikoma.ApiToken_NO_ACTION,
		ActiveToggle:            nil,
		AuthenticationRole:      0,
		RecipientOverrideToggle: nil,
	}
	if cmd.OverrideEmail != nil {
		req.RecipientOverrideToggle = &tachikoma.ModifyUserRequest_RecipientOverride{
			RecipientOverride: &tachikoma.EmailAddress{
				Email: *cmd.OverrideEmail,
			},
		}
	}
	if cmd.Active != "keep" {
		req.ActiveToggle = &tachikoma.ModifyUserRequest_Active{
			Active: cmd.Active == "true",
		}
	}

	result, err := client.ModifyFrontendUser(ctx, req)
	if err != nil {
		logger.Fatal().Err(err).Msgf("Failed to remove user %d", cmd.UserId)
	}
	logger.Info().Any("result", result).Msg("Removed user")
}

//go:generate mkdir -p build/generated
//go:generate protoc --go_out=build/generated --go-grpc_out=build/generated -I=../tachikoma-frontend-api-proto/src/main/proto/ ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/frontend/maildelivery/maildelivery.proto ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/frontend/tracking/mailtracking.proto ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/frontend/common.proto ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/frontend/auth/auth.proto ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/admin/users/users.proto ../tachikoma-frontend-api-proto/src/main/proto/com/sourceforgery/tachikoma/grpc/admin/blockedemail/blockedemail.proto
