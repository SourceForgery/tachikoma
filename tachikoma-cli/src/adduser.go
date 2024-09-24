package main

import (
	"context"
	"google.golang.org/grpc"
	"sourceforgery.com/tachikoma-cli/build/generated/github.com/SourceForgery/tachikoma"
)

type AddUserCmd struct {
	User           string `long:"username" description:"Username for the new user"`
	Password       string `long:"password" description:"Password for the new user"`
	CreateApiToken bool   `long:"api-token" description:"If API token should be created"`
	AdminRole      bool   `long:"admin-role" description:"If this is an admin role"`
	OverrideEmail  string `long:"override-email" description:"Override recipient with this email for all emails"`
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
