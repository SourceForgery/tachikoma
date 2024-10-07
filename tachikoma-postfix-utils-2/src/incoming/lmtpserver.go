package incoming

import (
	"bufio"
	"fmt"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
	"io"
	"net"
	"os"
	"regexp"
	"strings"
)

type LMTPServer struct {
	socketPath        string
	onIncomingMessage func(message *tachikoma.IncomingEmailMessage) (accepted bool, err error)
}

func NewLMTPServer(socketPath string, onIncomingMessage func(message *tachikoma.IncomingEmailMessage) (accepted bool, err error)) *LMTPServer {
	return &LMTPServer{
		socketPath:        socketPath,
		onIncomingMessage: onIncomingMessage,
	}
}

func (server *LMTPServer) Listen() error {
	if err := os.RemoveAll(server.socketPath); err != nil {
		return err
	}

	ln, err := net.Listen("unix", server.socketPath)
	if err != nil {
		return err
	}

	//goland:noinspection GoUnhandledErrorResult
	defer ln.Close()

	for {
		conn, err := ln.Accept()
		if err != nil {
			return err
		}
		go server.handleConnection(conn)
	}
}

func isValidLHLO(command string) bool {
	re := regexp.MustCompile(`^LHLO\s+([a-zA-Z0-9][-a-zA-Z0-9]*[a-zA-Z0-9]\.)*[a-zA-Z]{2,}$`)
	return re.MatchString(command)
}

func (server *LMTPServer) handleConnection(rw io.ReadWriteCloser) {
	//goland:noinspection GoUnhandledErrorResult
	defer rw.Close()
	scanner := bufio.NewScanner(rw)
	email := &tachikoma.IncomingEmailMessage{}
	var inData bool

	writeLn := func(lines ...string) (err error) {
		for _, line := range lines {
			_, err = rw.Write([]byte(line + "\r\n"))
			if err != nil {
				return err
			}
		}
		return nil
	}

	for scanner.Scan() {
		line := scanner.Text()
		var data string

		var err error
		if inData {
			if line == "." {
				inData = false
				email.Body = []byte(data)
				var accepted bool
				accepted, err = server.onIncomingMessage(email)
				if err != nil {
					_ = writeLn(fmt.Sprintf("451 Unable to forward email: %v", err))
				} else if accepted {
					err = writeLn("250 email queued")
				} else {
					err = writeLn("550 nobody here with that email")
				}
				if err != nil {
					logger.Fatal().Err(err).Msg("Failed to forward email")
				}
				logger.Info().Msgf("Received email: %+v", email)
				email = &tachikoma.IncomingEmailMessage{}
			} else {
				data += line + "\r\n"
			}
		} else if strings.HasPrefix(line, "LHLO") {
			if isValidLHLO(line) {
				err = writeLn(
					"250-localhost",
					"250 SIZE 10240000",
					"250 8BITMIME",
				)
			} else {
				err = writeLn("501 Syntax error in parameters or arguments")
			}
		} else if strings.HasPrefix(line, "MAIL FROM:") {
			email.From = strings.TrimPrefix(line, "MAIL FROM:")
			err = writeLn("250 OK")
		} else if strings.HasPrefix(line, "RCPT TO:") {
			email.EmailAddress = strings.TrimPrefix(line, "RCPT TO:")
			err = writeLn("250 OK")
		} else if line == "DATA" {
			data = ""
			inData = true
			err = writeLn("354 End data with <CR><LF>.<CR><LF>")
		} else if line == "QUIT" {
			err = writeLn("221 Bye")
			if err == nil {
				return
			}
		}
		if err != nil {
			logger.Error().Err(err).Msg("Error handling LMTP connection")
		}
	}

	if err := scanner.Err(); err != nil {
		logger.Error().Err(err).Msg("Error reading from connection")
	}
}
