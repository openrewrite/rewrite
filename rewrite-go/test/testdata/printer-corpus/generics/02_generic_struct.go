package main

type Box[T any] struct {
	V T
}

func (b Box[T]) Get() T {
	return b.V
}
