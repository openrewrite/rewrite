# 12. Ruby parsing via JRuby

## Status

Accepted

## Context

`rewrite-ruby` parses Ruby with JRuby, which ships two parser front ends in the same artifact
(`org.jruby:jruby-base:10.1.1.0`).

**Legacy `org.jruby.ast`** is still the default in 10.1, selected by `Options.PARSER_PRISM` when the
`ParserManager` is constructed. Its node set is essentially unchanged since 9.4 — only `NewlineNode`
and `ClassVarDeclNode` were removed and `ErrorNode` added — so the visitor written against it in 2023
still compiles and runs. But `org.jruby.ast.Node` exposes only `getLine()` (0-based) — no columns, no
offsets — which forces the parser to re-lex the source with its own cursor to attribute whitespace.
It also desugars several constructs a lossless tree cannot afford to lose:

- `{x:, y:}` produces exactly the same `HashNode` pairs as `{x: x, y: y}`.
- `def f(...)` is exploded into `*` / `**` / `&` parameters, with call sites rebuilt as
  `ArgsPushNode` + `BlockPassNode`; nothing records that `...` was written.
- Pattern captures (`42 => n`) become hash key/value pairs with a synthetic `NilImplicitNode`, and a
  pin (`^x`) degrades to a bare `LocalVarNode` with no token position.
- `it` and `_1` block parameters are synthesized into parameter lists that do not exist in the source.

**Prism** (`org.ruby_lang.prism`, arriving transitively through `jruby-prism`) is one system property
away. Every node carries `startOffset` / `length` / `endOffset()`, `Nodes.Source.line(offset)` gives
1-based lines, and it has first-class nodes for all of the above (`ImplicitNode`,
`ForwardingParameterNode`, `CapturePatternNode`, `PinnedVariableNode`, `ItParametersNode`,
`NumberedParametersNode`), plus recoverable, typed parse errors. Its JRuby-shipped Java binding is a
subset of upstream Prism: no `comments` array and no per-field locations (`nameLoc`, `operatorLoc`,
`openingLoc`, …), so gap re-lexing from the source text is required either way — but driven by exact
byte offsets rather than line numbers.

The module also carries roughly 270 round-trip tests that exercise the LST model and printer, neither
of which is coupled to the choice of front end.

## Decision

Stage A ports the existing legacy-AST visitor to JRuby 10.1.1.0 and pins the front end with the
`jruby.parser.prism=false` system property, set before `JavaEmbedUtils.initialize`, with an
`instanceof RootNode` guard on the parse result. This revalidates the LST, the printer and the test
suite against a current JRuby and a current OpenRewrite API surface for a day of work rather than a
rewrite.

Stage B retargets `RubyParserVisitor` at Prism, using the green Stage A suite as the regression net.

New Ruby 3.1+ syntax coverage is built only against Prism. Filling those gaps on the legacy AST would
mean reconstructing tokens the parser deliberately threw away, and every such workaround would be
discarded at the Stage B cut-over.

## Consequences

- The module depends on `jruby-base` at a pinned version. That is a ~39-jar transitive tree including
  asm, the jnr/jffi native stack and the Chicory WASM runtime, and it sets the module's Java floor at
  21 (JRuby 10 class files are version 65).
- Pinning the parser is load-bearing, not defensive: `META-INF/services/org.jruby.parser.ParserProvider`
  already names the Prism provider, so a future default flip would turn the `RootNode` cast into a
  runtime failure.
- Ruby 3.1+ constructs that the legacy visitor has never seen reach `defaultVisit`, which throws, so
  they surface as `ParseError`s rather than silently degrading. That is the intended behavior — the
  module forbids `J.Unknown` — but it means a real-world corpus will have a `ParseError` tail until
  Stage B lands.
- `RubyParserVisitor`'s cursor arithmetic, backtracking and heredoc bookkeeping are written against
  line-granular positions. Stage B replaces that machinery rather than adapting it, so investment in
  it should be limited to keeping the current tests green.
