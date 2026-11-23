package main

type Counter struct {
	count int
}

func (c *Counter) Inc() {
	c.count++
}

func (c Counter) Value() int {
	return c.count
}
