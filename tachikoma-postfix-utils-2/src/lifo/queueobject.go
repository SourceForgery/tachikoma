package lifo

import (
	"github.com/SourceForgery/tachikoma/tachikoma-postfix-utils/build/generated/github.com/SourceForgery/tachikoma"
	"github.com/dgraph-io/badger/v3"
)

type QueueObject struct {
	key   []byte
	Item  *tachikoma.DeliveryNotification
	queue *BadgerQueue
}

func (qo *QueueObject) ack(txn *badger.Txn) (err error) {
	err = txn.Delete(qo.key)
	if err == nil {
		err = txn.Commit()
	}
	return
}

func (qo *QueueObject) Ack() error {
	err := qo.queue.db.Update(qo.ack)
	if qo.queue.Count() > 1 {
		qo.queue.availableMessages.Unlock()
	}
	return err
}
func (qo *QueueObject) Nack() {
	if qo.queue.Count() > 1 {
		qo.queue.lock.Lock()
		defer qo.queue.lock.Unlock()
		qo.queue.availableMessages.TryLock()
		qo.queue.availableMessages.Unlock()
	}
}
