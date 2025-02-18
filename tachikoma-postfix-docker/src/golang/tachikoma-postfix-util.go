package main

import (
	"crypto/tls"
	"encoding/base64"
	"github.com/jessevdk/go-flags"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/rs/zerolog/pkgerrors"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/credentials/insecure"
	"net/url"
	"os"
	"runtime/debug"
	"time"
)

var args struct {
	// Slice of bool will append 'true' each time the option
	// is encountered (can be set multiple times, like -vvv)
	Verbose []bool `short:"v" long:"verbose" description:"Show verbose debug information (repeat for more verbosity)"`
	Quiet   []bool `short:"q" long:"quiet" description:"Be very quiet"`

	Version bool `short:"V" long:"version" description:"Show version and exit"`

	LoggingFormat string `short:"l" long:"logging" choice:"coloured" choice:"plain" choice:"json" default:"coloured" description:"Log output format"`

	ManPage string `long:"man-page" description:"Write manpage in groff"`

	TachikomaUrl string `long:"url" env:"TACHIKOMA_URL" required:"true" description:"Tachikoma server URL"`
	Insecure     bool   `short:"i" long:"insecure" description:"If the server certificate is allowed even if it couldn't be verified'" env:"INSECURE"`
	ClientCert   string `long:"client-cert" env:"TACHIKOMA_CLIENT_CERT" description:"Base64 encoded client certificate"`
	ClientKey    string `long:"client-key" env:"TACHIKOMA_CLIENT_KEY" description:"Base64 encoded client key"`
}

var logger zerolog.Logger

func parseOpts() {
	parser := flags.NewParser(&args, flags.Default)
	_, err := parser.ParseArgs(os.Args)
	if err != nil {
		os.Exit(1)
	}

	zerolog.ErrorStackMarshaler = pkgerrors.MarshalStack
	switch args.LoggingFormat {
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
		logger.Panic().Msgf("What the f is %s", args.LoggingFormat)
	}
	logger = logger.With().Caller().Timestamp().Logger()

	verbosity := 1 - len(args.Verbose) + len(args.Quiet)
	if verbosity <= int(zerolog.DebugLevel) {
		zerolog.SetGlobalLevel(zerolog.DebugLevel)
	} else if verbosity >= int(zerolog.FatalLevel) {
		zerolog.SetGlobalLevel(zerolog.FatalLevel)
	} else {
		zerolog.SetGlobalLevel(zerolog.Level(verbosity))
	}

	if args.Version {
		info, ok := debug.ReadBuildInfo()
		if !ok {
			logger.Fatal().Msg("Couldn't find build info")
		}
		var gitVersion string
		for _, setting := range info.Settings {
			switch setting.Key {
			case "vcs.revision":
				gitVersion = setting.Value
			}
		}
		logger.Fatal().
			Str("git.version", gitVersion).
			Str("golang.version", info.GoVersion).
			Msgf("Version")
	}

	if args.ManPage != "" {
		if args.ManPage != "-" {
			w, err := os.Create(args.ManPage)
			if err != nil {
				return
			}
			parser.WriteManPage(os.Stdout)
			parser.WriteManPage(w)
			err = w.Sync()
			if err != nil {
				logger.Fatal().Err(err).Msgf("Couldn't couldn't write file: %s", args.ManPage)
			}
			err = w.Close()
			if err != nil {
				logger.Fatal().Err(err).Msgf("Couldn't couldn't write file: %s", args.ManPage)
			}
		}
		os.Exit(0)
	}

	if (args.ClientKey == "") != (args.ClientCert == "") {
		logger.Fatal().Msg("Either both client cert and key must be provided, or neither")
	}
}

func main() {
	parseOpts()
	conn, err := createClient()
	if err != nil {
		logger.Fatal().Err(err).Msg("Unable to connect to tachikoma")
	}
	//goland:noinspection GoUnhandledErrorResult
	defer conn.Close()
}

func createClient() (*grpc.ClientConn, error) {
	hasCerts := args.ClientKey == ""

	dialOptions := make([]grpc.DialOption, 0)
	if hasCerts {
		tachikomaClientCert, err := base64.StdEncoding.DecodeString(args.ClientCert)
		if err != nil {
			logger.Fatal().Err(err).Msg("Unable to decode tachikoma client cert")
		}
		tachikomaClientKey, err := base64.StdEncoding.DecodeString(args.ClientKey)
		if err != nil {
			logger.Fatal().Err(err).Msg("Unable to decode tachikoma client key")
		}
		keyPair, err := tls.X509KeyPair(tachikomaClientCert, tachikomaClientKey)
		if err != nil {
			log.Fatal().Err(err).Msg("Unable to load client cert")
		}
		config := &tls.Config{
			Certificates: []tls.Certificate{keyPair},
		}
		transportCreds := credentials.NewTLS(config)
		dialOptions = append(dialOptions, grpc.WithTransportCredentials(transportCreds))
	}

	if args.Insecure {
		dialOptions = append(dialOptions, grpc.WithTransportCredentials(insecure.NewCredentials()))
	}

	uri, err := url.Parse(args.TachikomaUrl)
	if err != nil {
		return nil, err
	}
	port := uri.Port()
	if port == "" {
		if uri.Scheme == "https" {
			port = "443"
		} else {
			port = "80"
		}
		uri.Host = uri.Hostname() + ":" + port
	}
	return grpc.NewClient(uri.Host, dialOptions...)
}
