package main

func Combine[
	K comparable,
	V any,
	R any,
](k K, v V, fn func(K, V) R) R {
	return fn(k, v)
}
