package syslog

import (
	"bufio"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/src/zerlogger"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"os"
	"strings"
)

var logger zerolog.Logger

func init() {
	logger = log.Logger.Hook(&zerlogger.LoggerNameHook{LoggerName: "queue"})
}

type SyslogSniffer struct {
	deliverer func(deliveryNotification *tachikoma.DeliveryNotification) error
}

func NewSyslogSniffer(deliverer func(deliveryNotification *tachikoma.DeliveryNotification) error) *SyslogSniffer {
	return &SyslogSniffer{deliverer: deliverer}
}

func (s *SyslogSniffer) Start() {
	go s.blockingSniffer()
}

func (s *SyslogSniffer) blockingSniffer() {
	const socketPath = "/opt/maillog_pipe"

	file, err := os.Open(socketPath)
	if err != nil {
		logger.Fatal().Msgf("failed to open file: %v", err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer file.Close()

	reader := bufio.NewReader(file)
	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			logger.Fatal().Err(err).Msgf("failed to open file: %s", socketPath)
		}

		notification := parseLine(line)
		if notification != nil {
			logger.Debug().Msgf("Parsed notification: %v\n", notification)
			err := s.deliverer(notification)
			if err != nil {
				logger.Error().Err(err).Msgf("Failed to deliver notification: %v", notification)
				return
			}
		}
	}
}

func parseLine(line string) *tachikoma.DeliveryNotification {
	// Split the line using ": " delimiter and limit of 3
	split := strings.SplitN(line, ": ", 3)
	logger.Debug().Msgf("[[[[%s]]]] (%d)", line, len(split))

	if len(split) == 3 && !strings.Contains(line, "postfix/lmtp") {
		queueId := split[1]
		rest := split[2]

		mapResult, err := splitLineToMap(rest)
		if err != nil {
			logger.Warn().Err(err).Msgf("Failed to parse line %s", line)
			return nil
		}

		if dsn, ok := mapResult["dsn"]; ok {
			if originalRecipient, ok := mapResult["to"]; ok {
				if status, ok := mapResult["status"]; ok {
					return &tachikoma.DeliveryNotification{
						DiagnoseText:      status,
						Reason:            strings.Split(status, " ")[0],
						QueueId:           queueId,
						Status:            dsn,
						OriginalRecipient: strings.Trim(originalRecipient, "<>"),
					}
				}
			}
		}
	}
	return nil
}

func splitLineToMap(rest string) (map[string]string, error) {
	// Split the rest using ", " delimiter
	parts := strings.Split(rest, ", ")
	result := make(map[string]string)

	for _, part := range parts {
		if idx := strings.Index(part, "="); idx > 0 {
			key := part[:idx]
			value := part[idx+1:]
			result[key] = value
		}
	}

	return result, nil
}
