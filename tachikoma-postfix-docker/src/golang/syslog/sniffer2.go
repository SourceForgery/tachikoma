package syslog

import (
	"github.com/rs/zerolog/log"
	"gopkg.in/mcuadros/go-syslog.v2"
	"gopkg.in/mcuadros/go-syslog.v2/format"
	"strings"
	"tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
)

const (
	udpListeningAddr = "127.0.0.1:1514" // Standard syslog port
)

var (
	_ syslog.Handler = (*logParserHandler)(nil)
)

type logParserHandler struct {
	notificationsChannel chan<- *tachikoma.DeliveryNotification
}

func Listen(notificationsChannel chan<- *tachikoma.DeliveryNotification) {
	server := syslog.NewServer()
	server.SetHandler(
		&logParserHandler{
			notificationsChannel: notificationsChannel,
		},
	)

	log.Info().Msgf("Listening for syslog messages on UDP port %s", udpListeningAddr)

	err := server.ListenUDP(udpListeningAddr)
	if err != nil {
		log.Fatal().Err(err).Msg("Error creating UDP listener")
	}

	err = server.Boot()
	if err != nil {
		log.Fatal().Err(err).Msg("Error creating UDP listener")
	}
	//goland:noinspection GoUnhandledErrorResult
	defer server.Kill()
	server.Wait()
}

func (f logParserHandler) Handle(data format.LogParts, x int64, err error) {
	if err != nil {
		log.Error().Err(err).Msg("Error handling syslog message")
		return
	}
	result := tachikoma.DeliveryNotification{
		QueueId: "queueId",
	}

	//timestamp := data["timestamp"].(time.Time)
	//hostname := data["hostname"].(string)
	//tag := data["tag"].(string)
	content := data["content"].(string)
	//priority := data["priority"].(int)
	//facility := data["facility"].(int)
	//severity := data["severity"].(int)

	dataMap := splitLineToMap(content)
	hasFields := false
	log.Debug().Msgf("  ID: %s", dataMap["id"])
	dsn, hasDsn := dataMap["dsn"]
	to, hasTo := dataMap["to"]
	status, hasStatus := dataMap["status"]
	if !hasDsn || !hasTo || !hasStatus {
		hasFields = true
		result.OriginalRecipient = to
		result.Status = dsn
		result.DiagnoseText = status
		result.Reason = strings.Split(status, " ")[0]
	}
	if hasFields {
		f.notificationsChannel <- &result
	}
}

func splitLineToMap(rest string) map[string]string {
	result := make(map[string]string)
	lines := strings.Split(rest, ", ")
	for _, line := range lines {
		if strings.Contains(line, "=") && !strings.HasPrefix(line, "=") {
			parts := strings.SplitN(line, "=", 2)
			if len(parts) == 2 {
				result[parts[0]] = parts[1]
			}
		}
	}
	return result
}
