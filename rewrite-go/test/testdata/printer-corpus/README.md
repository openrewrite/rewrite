# Printer fidelity corpus

A growing collection of `.go` fixtures used to detect regressions in
`pkg/printer/go_printer.go`. Each fixture is parsed and printed; the
output must be byte-equal to the input.

## Layout

```
test/testdata/printer-corpus/
  gofmt/      ← non-gofmt'd inputs (mixed tabs/spaces, brace placement, etc.)
  generics/   ← multi-line type parameters, union constraints, nested generics
  README.md
  TODO.md     ← known failures with notes on the suspected fix area
```

Lives under `testdata/` so `go test ./...` skips it (Go treats
`testdata/` as a magic directory). Every `.go` file under any
subdirectory is included automatically by the corpus driver in
`pkg/printer/parity_test.go`.

## Running

The corpus is gated behind the `parityaudit` build tag so it never runs
in CI. Locally:

```sh
make parity
```

That target invokes `go test -tags parityaudit ./pkg/printer/...`,
which picks up the corpus driver and walks the fixtures.

## Adding cases

1. Drop a `.go` file under `gofmt/` or `generics/` with whatever shape
   you suspect breaks the printer.
2. Run `make parity`. If your case fails, file the diff in `TODO.md`
   alongside a one-line guess at the broken printer code path.
3. Fix the printer; re-run; the test passes.

## Why isn't this in CI?

P2 in the eng review: corpus runs are open-ended (a new bug can land
without a corpus regression, and a corpus diff can take longer to triage
than tests like `go test`). Keeping it manual gives fast iteration on
real bug reports without making the CI pipeline noisy.
