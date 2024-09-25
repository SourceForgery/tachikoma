package lifo

import (
	"fmt"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/src/zerlogger"
	"github.com/dgraph-io/badger/v3"
	"github.com/pkg/errors"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"google.golang.org/protobuf/proto"
	"sync"
)

var logger zerolog.Logger

func init() {
	logger = log.Logger.Hook(&zerlogger.LoggerNameHook{LoggerName: "queue"})
}

type BadgerQueue struct {
	db                *badger.DB
	highestId         uint64
	lock              sync.Mutex
	availableMessages sync.Mutex
}

func NewBadgerQueue(path string) (*BadgerQueue, error) {
	opts := badger.DefaultOptions(path).WithLogger(nil)
	db, err := badger.Open(opts)
	if err != nil {
		return nil, err
	}
	var highestId uint64 = 0
	_ = db.View(func(txn *badger.Txn) error {
		iter := txn.NewIterator(badger.IteratorOptions{
			PrefetchValues: true,
			PrefetchSize:   100,
			Reverse:        true,
		})
		iter.Next()
		if iter.Valid() {
			highestId, err = btoi(iter.Item().Key())
			return err
		}
		return nil
	})

	bq := &BadgerQueue{
		db:        db,
		highestId: highestId + 1,
	}

	if bq.Count() == 0 {
		bq.availableMessages.Lock()
	}
	return bq, nil
}

func (q *BadgerQueue) Count() (count int) {
	for _, info := range q.db.Tables() {
		count += int(info.KeyCount)
	}
	return count
}

func (q *BadgerQueue) Dequeue() (*QueueObject, error) {
	q.availableMessages.Lock()
	return q.next()
}

func (q *BadgerQueue) Enqueue(notification *tachikoma.DeliveryNotification) error {
	q.lock.Lock()
	defer q.lock.Unlock()

	err := q.db.Update(func(txn *badger.Txn) error {
		val, err := proto.Marshal(notification)
		if err != nil {
			return err
		}

		q.availableMessages.Unlock()
		q.highestId++
		return txn.Set(itob(q.highestId), val)
	})
	if err != nil {
		return err
	}
	// Unlock because there's guaranteed to be at least one message now.
	_ = q.availableMessages.TryLock()
	q.availableMessages.Unlock()
	return nil
}

func (q *BadgerQueue) next() (*QueueObject, error) {
	q.lock.Lock()
	defer q.lock.Unlock()

	queueObject := &QueueObject{}
	err := q.db.View(func(txn *badger.Txn) error {
		opt := badger.DefaultIteratorOptions
		it := txn.NewIterator(opt)
		defer it.Close()

		it.Rewind()
		if it.Valid() {
			item := it.Item()
			queueObject.key = item.Key()
			err := item.Value(func(val []byte) error {
				queueObject.Item = &tachikoma.DeliveryNotification{}
				return proto.Unmarshal(val, queueObject.Item)
			})
			if err != nil {
				return err
			}
		}
		return nil
	})
	if err != nil {
		return nil, err
	}
	return queueObject, nil
}

func (q *BadgerQueue) remove(notification *tachikoma.DeliveryNotification) error {
	q.lock.Lock()
	defer q.lock.Unlock()

	return q.db.Update(func(txn *badger.Txn) error {
		opt := badger.DefaultIteratorOptions
		it := txn.NewIterator(opt)
		defer it.Close()

		it.Rewind()
		if it.Valid() {
			item := it.Item()
			var currentNotification *tachikoma.DeliveryNotification
			err := item.Value(func(val []byte) error {
				currentNotification = &tachikoma.DeliveryNotification{}
				return proto.Unmarshal(val, currentNotification)
			})
			if err != nil {
				return err
			}

			// Check if the item matches the one we just processed
			if proto.Equal(notification, currentNotification) {
				return txn.Delete(item.Key())
			}
		}
		return nil
	})
}

func (q *BadgerQueue) Close() error {
	return q.db.Close()
}

func itob(v uint64) []byte {
	b := make([]byte, 8)
	for i := uint64(0); i < 8; i++ {
		b[i] = byte(v >> (i * 8))
	}
	return b
}

func btoi(bytes []byte) (result uint64, err error) {
	if len(bytes) != 8 {
		return 0, errors.New(fmt.Sprintf("must be exactly 8 bytes, not %d", len(bytes)))
	}
	for i := 0; i < 8; i++ {
		result |= bytes[i] << (i * 8)
	}
	return
}
