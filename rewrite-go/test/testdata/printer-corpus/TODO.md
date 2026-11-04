# Printer corpus — known failures

Track parse → print byte-equality failures here. Each entry should
describe the failing fixture, paste a minimal diff, and guess at the
broken printer code path so the next dev (or you, in a week) can pick
up the trail.

## Status

As of the initial corpus (13 fixtures across `gofmt/` and `generics/`),
**all cases pass byte-equality**. The list below is empty.

When `make parity` fails on a new fixture:

1. Add a heading here with the fixture path.
2. Paste the minimal expected/actual diff from the test output.
3. Note one or two suspect locations in `pkg/printer/go_printer.go`.
4. Open a PR that adds the failing fixture **and** the printer fix in
   the same change so the corpus stays green.

## Open

_(none)_

## Adjacent work that surfaced through the corpus driver

_(none)_ — the previously-noted semicolon-between-statements parser bug,
the AddImport-on-empty-file whitespace bug, and the OrderImports
reorder-loses-newlines bug have all shipped. See
`test/import_recipes_test.go` for the regression coverage.
