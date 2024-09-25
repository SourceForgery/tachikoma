package main

import (
	"github.com/joho/godotenv"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/rs/zerolog/pkgerrors"
	"net/url"
	"os"
	"strconv"
	"time"
)

var logger zerolog.Logger

type Config struct {
	TachikomaURL   *url.URL
	Insecure       bool
	ClientCertPath string
	ClientKeyPath  string
}

func readConfig(file ...string) *Config {
	err := godotenv.Load(file...) // Load .env file if exists.
	if err != nil {
		log.Fatal().Msgf("Error loading environment file")
	}

	zerolog.ErrorStackMarshaler = pkgerrors.MarshalStack
	var tmpLog zerolog.Logger
	switch getEnv("LOGGER", false) {
	case "json":
		tmpLog = zerolog.New(os.Stdout)
		break
	default:
	case "coloured":
		tmpLog = zerolog.New(zerolog.ConsoleWriter{Out: os.Stderr, TimeFormat: time.RFC3339})
		break
	case "plain":
		tmpLog = zerolog.New(zerolog.ConsoleWriter{Out: os.Stderr, TimeFormat: time.RFC3339, NoColor: true})
		break
	}
	log.Logger = tmpLog.With().Timestamp().Logger()
	logger = tmpLog.Hook()

	tachikomaURL, err := url.Parse(getEnv("TACHIKOMA_URL", true))
	if err != nil {
		log.Fatal().Err(err).Msgf("Error parsing TACHIKOMA_URL: %s", err)
	}
	insecure := getEnvAsBool("INSECURE", false)
	clientCert := getEnv("TACHIKOMA_CLIENT_CERT", false)
	clientKey := getEnv("TACHIKOMA_CLIENT_KEY", false)

	return &Config{
		TachikomaURL:   tachikomaURL,
		Insecure:       insecure,
		ClientCertPath: clientCert,
		ClientKeyPath:  clientKey,
	}
}

func getEnv(key string, required bool) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	} else if required {
		logger.Fatal().Msgf("Environment variable %s is not set", key)
	}
	return ""
}

func getEnvAsBool(name string, required bool) bool {
	valStr := getEnv(name, required)
	if val, err := strconv.ParseBool(valStr); err == nil {
		return val
	} else if required {
		logger.Fatal().Msgf("Environment variable %s is not set", name)
	}
	return false
}
