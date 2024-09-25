package zerlogger

import (
	"github.com/rs/zerolog"
)

var (
	_ zerolog.Hook = (*LoggerNameHook)(nil)
)

type LoggerNameHook struct {
	LoggerName string
}

func (hook *LoggerNameHook) Run(e *zerolog.Event, level zerolog.Level, message string) {
	e.Str("logger", hook.LoggerName)
}
