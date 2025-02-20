package disk_queue

import (
	"os"
	"time"

	"github.com/nsqio/go-diskqueue"
	"github.com/rs/zerolog"
)

// Initialize zerolog logger.
var logger = zerolog.New(os.Stderr).With().Timestamp().Logger()

// Serializer and Deserializer allow you to plug in your own encoding.
type Serializer[T any] func(obj T) ([]byte, error)
type Deserializer[T any] func(data []byte) (T, error)

// Queue is a generic persisted FIFO queue.
type Queue[T any] struct {
	dq           diskqueue.Interface
	sendFunction func(T) error
	serializer   Serializer[T]
	deserializer Deserializer[T]
}

// NewQueue creates a new diskqueue-based Queue.
// It uses the signature:
// New(name string, dataPath string, maxBytesPerFile int64,
//
//	minMsgSize int32, maxMsgSize int32,
//	syncEvery int64, syncTimeout time.Duration, logf AppLogFunc)
func NewQueue[T any](
	name string,
	dataPath string,
	maxBytesPerFile int64,
	minMsgSize int32,
	maxMsgSize int32,
	syncEvery int64,
	syncTimeout time.Duration,
	sendFunc func(T) error,
	serializer Serializer[T],
	deserializer Deserializer[T],
) *Queue[T] {
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
			zerologLevel = zerolog.InfoLevel // Default to INFO level for unknown cases
		}
		logger.WithLevel(zerologLevel).Msgf(format, args...)
	}

	dq := diskqueue.New(
		name,
		dataPath,
		maxBytesPerFile,
		minMsgSize,
		maxMsgSize,
		syncEvery,
		syncTimeout,
		logf,
	)

	q := &Queue[T]{
		dq:           dq,
		sendFunction: sendFunc,
		serializer:   serializer,
		deserializer: deserializer,
	}

	go q.process()
	return q
}

// Enqueue serializes the object and adds it to the persistent queue.
func (q *Queue[T]) Enqueue(obj T) error {
	data, err := q.serializer(obj)
	if err != nil {
		return err
	}
	return q.dq.Put(data)
}

// process reads from the diskqueue and sends objects using the sendFunction.
func (q *Queue[T]) process() {
	readChan := q.dq.ReadChan()
	for data := range readChan {
		obj, err := q.deserializer(data)
		if err != nil {
			logger.Error().Err(err).Msg("failed to deserialize data")
			continue
		}

		if err := q.sendFunction(obj); err != nil {
			logger.Error().Err(err).Msg("send failed; backing off")
			time.Sleep(5 * time.Second)
			// On failure, re-enqueue the data data.
			if err := q.dq.Put(data); err != nil {
				logger.Error().Err(err).Msg("failed to re-enqueue")
			}
		}
	}
}
