package common

import (
	"bytes"
	"context"
	"errors"
	"io"
	"strings"
	"sync"
)

type ChannelReadWriteCloser struct {
	readCh     chan string
	writeCh    chan string
	ctx        context.Context
	cancelFunc context.CancelFunc
	wg         sync.WaitGroup
	writeBuf   *bytes.Buffer
}

func NewChannelReadWriteCloser() *ChannelReadWriteCloser {
	ctx, cancelFunc := context.WithCancel(context.Background())
	return &ChannelReadWriteCloser{
		readCh:     make(chan string),
		writeCh:    make(chan string),
		ctx:        ctx,
		cancelFunc: cancelFunc,
		writeBuf:   new(bytes.Buffer),
	}
}

func (c *ChannelReadWriteCloser) Write(p []byte) (n int, err error) {
	if c.ctx.Err() != nil {
		return 0, errors.New("write to closed ChannelReadWriteCloser")
	}

	data := string(p)
	lines := strings.Split(data, "\n")

	for i, line := range lines {
		if i == 0 {
			c.writeBuf.WriteString(line)
		} else {
			c.writeCh <- c.writeBuf.String()
			c.writeBuf.Reset()
			c.writeBuf.WriteString(line)
		}
	}

	return len(p), nil
}

func (c *ChannelReadWriteCloser) Read(p []byte) (n int, err error) {
	c.wg.Add(1)
	defer c.wg.Done()

	select {
	case <-c.ctx.Done():
		return 0, io.EOF
	case data := <-c.readCh:
		return copy(p, data), nil
	}
}

func (c *ChannelReadWriteCloser) Close() error {
	c.cancelFunc()
	c.wg.Wait()
	//close(c.writeCh)
	return nil
}

func (c *ChannelReadWriteCloser) GetReadChan() chan<- string {
	return c.readCh
}

func (c *ChannelReadWriteCloser) GetWriteChan() <-chan string {
	return c.writeCh
}
