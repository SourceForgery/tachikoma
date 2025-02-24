package persistedqueue

import (
	"github.com/rs/zerolog/log"
	"time"

	"github.com/nsqio/go-diskqueue"
	"github.com/rs/zerolog"
)

type Serializer[T any] func(obj T) ([]byte, error)
type Deserializer[T any] func(data []byte) (T, error)

type Queue[T any] struct {
	dq           diskqueue.Interface
	sendFunction func(T) error
	serializer   Serializer[T]
	deserializer Deserializer[T]
	inputChan    chan T
}

// NewQueue creates a new diskqueue-based Queue with simplified configuration.
// Only requires the essential parameters:
// - dataPath: directory path for queue storage
// - sendFunc: function to process dequeued items
// - serializer: function to convert items to bytes
// - deserializer: function to convert bytes back to items
func NewQueue[T any](
	queueName string,
	dataPath string,
	sendFunc func(T) error,
	serializer Serializer[T],
	deserializer Deserializer[T],
) *Queue[T] {
	const (
		defaultMaxBytesPerFile = 100 * 1024 * 1024 // 100MB
		defaultMinMsgSize      = 0
		defaultMaxMsgSize      = 1024 * 1024 // 1MB
		defaultSyncEvery       = 1000
		defaultSyncTimeout     = time.Second * 2
	)

	logf := func(level diskqueue.LogLevel, format string, args ...interface{}) {
		var zerologLevel zerolog.Level
		switch level {
		case diskqueue.DEBUG:
			zerologLevel = zerolog.DebugLevel
		case diskqueue.INFO:
			zerologLevel = zerolog.InfoLevel
		case diskqueue.WARN:
			zerologLevel = zerolog.WarnLevel
		case diskqueue.ERROR:
			zerologLevel = zerolog.ErrorLevel
		case diskqueue.FATAL:
			zerologLevel = zerolog.FatalLevel
		default:
			zerologLevel = zerolog.InfoLevel
		}
		log.WithLevel(zerologLevel).Msgf(format, args...)
	}

	dq := diskqueue.New(
		queueName,
		dataPath,
		defaultMaxBytesPerFile,
		defaultMinMsgSize,
		defaultMaxMsgSize,
		defaultSyncEvery,
		defaultSyncTimeout,
		logf,
	)

	q := &Queue[T]{
		dq:           dq,
		sendFunction: sendFunc,
		serializer:   serializer,
		deserializer: deserializer,
		inputChan:    make(chan T, 100),
	}

	go q.handleInput()
	go q.process()
	return q
}

// InputChan returns the channel for enqueueing new items
func (q *Queue[T]) InputChan() chan<- T {
	return q.inputChan
}

// Close cleanly shuts down the queue
func (q *Queue[T]) Close() error {
	close(q.inputChan)
	return q.dq.Close()
}

func (q *Queue[T]) handleInput() {
	for obj := range q.inputChan {
		data, err := q.serializer(obj)
		if err != nil {
			log.Error().Err(err).Msg("failed to serialize data")
			continue
		}
		if err := q.dq.Put(data); err != nil {
			log.Error().Err(err).Msg("failed to enqueue data")
		}
	}
}

func (q *Queue[T]) process() {
	readChan := q.dq.ReadChan()
	for data := range readChan {
		obj, err := q.deserializer(data)
		if err != nil {
			log.Error().Err(err).Msg("failed to deserialize data")
			continue
		}

		if err := q.sendFunction(obj); err != nil {
			log.Error().Err(err).Msg("send failed; backing off")
			time.Sleep(5 * time.Second)
			if err := q.dq.Put(data); err != nil {
				log.Error().Err(err).Msg("failed to re-enqueue")
			}
		}
	}
}
