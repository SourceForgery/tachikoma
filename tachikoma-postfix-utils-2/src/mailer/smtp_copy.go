package main

import (
	"io"
	"net/smtp"
)

// Copied from net/smtp.go

var (
	_ io.WriteCloser = (*DataCloser)(nil)
)

type DataCloser struct {
	c          *smtp.Client
	EndMessage string
	io.WriteCloser
}

func (d *DataCloser) Close() (err error) {
	_ = d.WriteCloser.Close()
	_, d.EndMessage, err = d.c.Text.ReadResponse(250)
	return
}

// Data issues a DATA command to the server and returns a writer that
// can be used to write the mail headers and body. The caller should
// close the writer before calling any more methods on c. A call to
// Data must be preceded by one or more calls to [Client.Rcpt].
func Data(c *smtp.Client) (*DataCloser, error) {
	_, _, err := cmd(c, 354, "DATA")
	if err != nil {
		return nil, err
	}
	return &DataCloser{c, "", c.Text.DotWriter()}, nil
}
