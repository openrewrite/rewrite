package main

type Container[T any] struct {
	Items []Box[T]
}

type Box[T any] struct {
	V T
}

func New[T any]() Container[T] {
	return Container[T]{Items: []Box[T]{}}
}
