package main

import (
	"context"
	"google.golang.org/grpc"
	"sourceforgery.com/tachikoma-cli/build/generated/github.com/SourceForgery/tachikoma"
)

type ModifyUserCmd struct {
	UserId        int64   `long:"userid" description:"Userid for the new user"`
	Password      string  `long:"password" description:"Password for the new user"`
	ApiToken      string  `long:"api-token" choice:"remove" choice:"reset" choice:"keep" default:"keep" description:"If API token should be modified"`
	AdminRole     bool    `long:"admin-role" description:"If this is an admin role"`
	Active        string  `long:"active" choice:"keep" choice:"true" choice:"false" default:"keep" description:"If the user is active"`
	OverrideEmail *string `long:"override-email" description:"Override recipient with this email for all emails"`
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
