.PHONY: test parity

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
