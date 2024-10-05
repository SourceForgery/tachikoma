package syslog

import (
	"bufio"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/src/zerlogger"
	"github.com/pkg/errors"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"io"
	"os"
	"strings"
	"time"
)

var logger zerolog.Logger

func init() {
	logger = log.Logger.Hook(&zerlogger.LoggerNameHook{LoggerName: "queue"})
}

type SyslogSniffer struct {
	deliverer  func(deliveryNotification *tachikoma.DeliveryNotification) error
	syslogPath string
}

func NewSyslogSniffer(deliverer func(deliveryNotification *tachikoma.DeliveryNotification) error, syslogPath string) *SyslogSniffer {
	return &SyslogSniffer{deliverer: deliverer,
		syslogPath: syslogPath}
}

func (s *SyslogSniffer) Start() {
	go s.retryingBlockingSniffer()
}

func (s *SyslogSniffer) retryingBlockingSniffer() {
	for {
		s.blockingSniffer()
		logger.Warn().Msg("Restarting syslog sniffer")
		time.Sleep(500 * time.Millisecond)
	}
}

func (s *SyslogSniffer) blockingSniffer() {
	file, err := os.Open(s.syslogPath)
	if err != nil {
		logger.Fatal().Msgf("failed to open file: %v", err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer file.Close()

	reader := bufio.NewReader(file)
	for {
		line, err := reader.ReadString('\n')
		if errors.Is(err, io.EOF) {
			return
		} else if err != nil {
			logger.Fatal().Err(err).Msgf("failed to read file: %s", s.syslogPath)
		}
		line = strings.TrimSpace(line)

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
