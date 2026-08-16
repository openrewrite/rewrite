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

## Sweeping real repositories

The fixtures above are cases someone already reduced. To find new ones,
`make sweep` runs the same pipeline over checked-out Go repositories —
clone them somewhere outside this repository — and sorts every file into
one of three classes:

- **parse error** — the parser rejects it, panics, or prints back
  something other than its input. A loud failure.
- **unsound** — it round-trips, but source text is sitting in a `Space`,
  where no recipe can see or rewrite it. Silent: a `rewriteRun` test on
  such a file passes.
- **sound** — round-trips and hides nothing.

```sh
mkdir -p /tmp/go-corpus && cd /tmp/go-corpus
for r in golang/go kubernetes/kubernetes prometheus/prometheus \
         hashicorp/terraform etcd-io/etcd golang/tools uber-go/zap gin-gonic/gin; do
  git clone --depth 1 https://github.com/$r.git ${r//\//_}
done
cd - && make sweep CORPUS=/tmp/go-corpus SWEEP_OUT=/tmp/go-sweep
```

`$SWEEP_OUT/report.txt` holds the counts and the failures grouped by
cause; `results.jsonl` holds a row per file, for querying. Buckets are
keyed on normalized signatures — error text with source offsets
stripped, hidden text with identifiers collapsed — because unnormalized
keys shatter one cause into hundreds of apparent singletons and hide the
large clusters worth fixing.

The run journals each result as it lands, so it resumes where it left
off. That matters because a runaway parser recursion is a stack
overflow, which `recover` cannot catch: the process dies, and the file
it was on is named by the leftover in-flight markers on the next run.

Pick a bucket, then shrink one of its files to something small enough to
become a test:

```sh
make reduce FILE=/tmp/go-corpus/golang_go/src/some/file.go
```

`make reduce` drops lines while the failure keeps its exact
classification, so it converges on the construct at fault rather than on
some other bug. Turn the result into an ordinary test in the topic file
that covers that syntax — `rewriteRun` already asserts both byte-equal
round-trip and a clean tree, so a minimal reproducer is a complete test.
