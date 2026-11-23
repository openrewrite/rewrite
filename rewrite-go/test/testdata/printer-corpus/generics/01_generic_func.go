package main

func Map[T any](xs []T, f func(T) T) []T {
	out := make([]T, len(xs))
	for i, x := range xs {
		out[i] = f(x)
	}
	return out
}
