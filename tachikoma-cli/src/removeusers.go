package main

import (
	"context"
	"google.golang.org/grpc"
	"sourceforgery.com/tachikoma-cli/build/generated/github.com/SourceForgery/tachikoma"
)

type RemoveUserCmd struct {
	UserId int64 `positional-args:"yes" required:"yes" description:"User IDs to remove"`
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
