.PHONY: test parity sweep reduce

# Default test run — what CI executes. Build-tag-gated tests stay out.
test:
	go test ./...

# Printer fidelity audit. Loads every fixture under
# test/printer-corpus/, parses + prints, and asserts byte-equality.
# Gated behind the `parityaudit` build tag so it never runs in CI;
# devs invoke it manually when investigating printer regressions.
#
# Trade-off (per the eng review's P2 decision): corpus regressions ship
# undetected by automation in exchange for fast iteration and simple
# test infra. Re-evaluate if a corpus-detectable bug ever lands.
parity:
	go test -tags parityaudit ./pkg/printer/...

# Classify a corpus of checked-out Go repositories as sound, unsound, or
# a parse error, and bucket the failures by cause. See
# test/testdata/printer-corpus/README.md for what the classes mean and
# how to read the report.
#
#   make sweep CORPUS=/tmp/go-corpus
CORPUS ?= /tmp/go-corpus
SWEEP_OUT ?= /tmp/go-sweep
sweep:
	GO_CORPUS=$(CORPUS) GO_CORPUS_OUT=$(SWEEP_OUT) \
		go test -tags parityaudit ./test/ -run TestSweep -timeout 180m -v

# Shrink one failing corpus file to a minimal reproducer.
#
#   make reduce FILE=/tmp/go-corpus/some/file.go
reduce:
	GO_REDUCE=$(FILE) GO_REDUCE_OUT=$(REDUCE_OUT) \
		go test -tags parityaudit ./test/ -run TestReduce -timeout 60m -v
