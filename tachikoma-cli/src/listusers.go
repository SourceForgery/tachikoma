package main

import (
	"context"
	"google.golang.org/grpc"
	"io"
	"sourceforgery.com/tachikoma-cli/build/generated/github.com/SourceForgery/tachikoma"
)

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
