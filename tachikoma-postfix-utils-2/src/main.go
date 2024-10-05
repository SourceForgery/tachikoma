package main

import (
	"context"
	"crypto/tls"
	"fmt"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/src/common"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/src/incoming"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/src/lifo"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/src/syslog"
	"github.com/pkg/errors"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/metadata"
	"os"
	"strconv"
	"strings"
	"time"
)

type PostfixUtils struct {
	authedContext  common.AddAuth
	grpcConnection *grpc.ClientConn
	queue          *lifo.BadgerQueue
}

func (config *Config) readCert() credentials.TransportCredentials {

	if (config.ClientCertPath != "") != (config.ClientKeyPath != "") {
		logger.Fatal().Msg("Either specify certificate and key or neither")
	}
	var creds credentials.TransportCredentials
	if config.ClientCertPath != "" {
		var cert tls.Certificate
		cert, err := tls.LoadX509KeyPair(config.ClientCertPath, config.ClientKeyPath)
		if err != nil {
			logger.Fatal().Err(err).Msgf("Failed to load client certificates: '%s' and '%s'", config.ClientCertPath, config.ClientKeyPath)
		}
		creds = credentials.NewTLS(&tls.Config{
			Certificates: []tls.Certificate{cert},
		})
	} else {
		creds = credentials.NewClientTLSFromCert(nil, "")
	}
	return creds
}

func main() {
	var config *Config
	if len(os.Args) > 1 {
		if os.Args[1] == "--help" {
			logger.Info().Msg(
				`
Put the following into a file. By default ".env" will be read,
but the first argument on the command line will be treated as a config file to be read.
TACHIKOMA_URL=https://foo:bar@example.com/
INSECURE=false
TACHIKOMA_CLIENT_CERT=/path/to/client.crt
TACHIKOMA_CLIENT_KEY=/path/to/client.key
`)
			os.Exit(0)
		} else {
			config = readConfig(os.Args[1])
		}
	} else {
		config = readConfig()
	}
	logger = log.Output(zerolog.ConsoleWriter{Out: os.Stdout}).With().Timestamp().Logger()

	connection, authedContext, err := createConnection(config)
	if err != nil {
		logger.Fatal().Err(err).Msg("Failed to connect to server")
	}
	//goland:noinspection GoUnhandledErrorResult
	defer connection.Close()

	queue, err := lifo.NewBadgerQueue(config.BadgerQueue)
	if err != nil {
		logger.Fatal().Err(err).Msg("Failed to create queue")
	}
	//goland:noinspection GoUnhandledErrorResult
	defer queue.Close()

	utils := PostfixUtils{
		authedContext:  authedContext,
		grpcConnection: connection,
		queue:          queue,
	}
	go utils.sendDeliveryNotifications()

	sniffer := syslog.NewSyslogSniffer(utils.queue.Enqueue, config.SyslogPath)
	sniffer.Start()

	handler := incoming.NewIncomingEmailHandler(connection, authedContext)
	lmtpServer := incoming.NewLMTPServer(config.IncomingEmailSocketPath, handler.Send)
	err = lmtpServer.Listen()
	if err != nil {
		logger.Fatal().Err(err).Msg("Couldn't start listening for incoming emails")
	}
}

func (utils *PostfixUtils) sendDeliveryNotifications() {
	ctx := utils.authedContext(context.Background())
	client := tachikoma.NewMTADeliveryNotificationsClient(utils.grpcConnection)
	for {
		dequeued, err := utils.queue.Dequeue()
		if err != nil {
			logger.Warn().Err(err).Msg("Failed to dequeue")
			time.Sleep(1 * time.Second)
			continue
		}
		_, err = client.SetDeliveryStatus(ctx, dequeued.Item)
		if err != nil {
			logger.Warn().Err(err).Msg("Failed to send delivery status")
			dequeued.Nack()
		} else {
			err = dequeued.Ack()
			if err != nil {
				logger.Fatal().Err(err).Msg("Failed to ack message. Message will be duplicated.")
				return
			}
		}
	}
}

func createConnection(config *Config) (*grpc.ClientConn, common.AddAuth, error) {
	creds := config.readCert()

	var port int
	if config.TachikomaURL.Port() != "" {
		var err error
		if port, err = strconv.Atoi(config.TachikomaURL.Port()); err != nil {
			return nil, nil, errors.Wrapf(err, "invalid port: '%s'", config.TachikomaURL.Port())
		}
	} else if strings.HasSuffix(config.TachikomaURL.Scheme, "s") {
		port = 443
	} else {
		port = 80
	}
	host := fmt.Sprintf("%s:%d", config.TachikomaURL.Host, port)

	logger.Info().Msgf("Connecting to server at %s", host)
	grpcConn, err := grpc.NewClient(host, grpc.WithTransportCredentials(creds))
	if err != nil {
		logger.Fatal().Err(err).Msg("Failed to connect to server")
	}

	md := metadata.New(map[string]string{
		"x-apitoken": config.TachikomaURL.User.String(),
	})
	return grpcConn, func(ctx context.Context) (authedContext context.Context) {
		return metadata.NewOutgoingContext(context.Background(), md)
	}, nil
}

//go:generate mkdir -p ../build/generated
//go:generate protoc --go_out=../build/generated --go-grpc_out=../build/generated -I=../../tachikoma-backend-api-proto/src/main/proto/ ../../tachikoma-backend-api-proto/src/main/proto/com/sourceforgery/tachikoma/mta/delivery_notifications.proto ../../tachikoma-backend-api-proto/src/main/proto/com/sourceforgery/tachikoma/mta/message_queue.proto
