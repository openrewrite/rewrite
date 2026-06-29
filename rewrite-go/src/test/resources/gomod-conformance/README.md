# GoMod conformance corpus

A shared corpus of `go.mod` (and occasionally sibling `go.sum`) inputs used
to enforce field-for-field parity between the Java `GoModParser` and the
Go `parser.ParseGoMod` / `parser.ParseGoSum`.

Each case has:

- `<case>.gomod` — the input go.mod text.
- `<case>.gomod.json` — the expected `GoResolutionResult`, serialized as
  the canonical conformance shape (lower-camelCase JSON, defined by both
  language test harnesses).
- (optional) `<case>.gosum` — sibling go.sum content. When present, the
  test suite parses it via `parseSumContent` (Java) / `ParseGoSum` (Go) and
  attaches the resulting `resolvedDependencies` to the marker before
  comparison.

Both languages run the same test corpus:

- Java: `org.openrewrite.golang.GoModConformanceTest`
- Go: `test/gomod_conformance_test.go`

The canonical JSON shape is:

```json
{
  "modulePath": "example.com/foo",
  "goVersion": "1.22",
  "toolchain": "go1.22.3",
  "requires": [{"modulePath": "github.com/x/y", "version": "v1.2.3", "indirect": false}],
  "replaces": [{"oldPath": "...", "oldVersion": null, "newPath": "...", "newVersion": null}],
  "excludes": [{"modulePath": "...", "version": "..."}],
  "retracts": [{"versionRange": "v1.0.0", "rationale": "..."}],
  "resolvedDependencies": [{"modulePath": "...", "version": "...", "moduleHash": "h1:...", "goModHash": "h1:..."}]
}
```

Optional fields are present in all cases; empty lists are `[]` (not omitted).
String fields default to `""` when absent on the parsed marker; nullable
string fields are written as JSON `null`.
