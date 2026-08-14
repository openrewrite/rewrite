# 12. Ruby parsing via Prism

## Status

Accepted

## Context

`rewrite-ruby`'s visitor was written in 2023 against JRuby's legacy `org.jruby.ast`, which is still
the default front end in JRuby 10.1 and whose node set is essentially unchanged since 9.4. It is
cheap to keep compiling against, and expensive to keep: `org.jruby.ast.Node` exposes only
`getLine()` (0-based) — no columns, no offsets — so the parser has to re-lex the source to attribute
whitespace, and it desugars constructs a lossless tree cannot afford to lose:

- `{x:, y:}` produces exactly the same `HashNode` pairs as `{x: x, y: y}`.
- `def f(...)` is exploded into `*` / `**` / `&` parameters, with call sites rebuilt as
  `ArgsPushNode` + `BlockPassNode`; nothing records that `...` was written.
- Pattern captures (`42 => n`) become hash key/value pairs with a synthetic `NilImplicitNode`, and a
  pin (`^x`) degrades to a bare `LocalVarNode` with no token position.
- `it` and `_1` block parameters are synthesized into parameter lists that do not exist in the source.

[Prism](https://github.com/ruby/prism) (`org.ruby_lang.prism`) is the parser Ruby itself is moving
to. Every node carries `startOffset` / `length` / `endOffset()`, `Nodes.Source.line(offset)` gives
1-based lines, and it has first-class nodes for all of the above (`ImplicitNode`,
`ForwardingParameterNode`, `CapturePatternNode`, `PinnedVariableNode`, `ItParametersNode`,
`NumberedParametersNode`), plus recoverable, typed parse errors. Its Java binding is a subset of
upstream Prism: no `comments` array and no per-field locations (`nameLoc`, `operatorLoc`,
`openingLoc`, …), so gap re-lexing from the source text is required either way — but driven by exact
byte offsets rather than line numbers.

The module also carries several hundred round-trip tests that exercise the LST model and printer,
neither of which is coupled to the choice of front end.

## Decision

`rewrite-ruby` parses with Prism, run standalone. `RubyParser` hands the source bytes to
`org.ruby_lang.prism.wasm.Prism` with explicit `ParsingOptions` and loads the serialized result with
`Loader`; there is no `org.jruby.Ruby` runtime and no system property selecting a front end. The
module depends on `prism-parser-api` and `prism-parser-wasm` directly, pinned to the versions JRuby
10.1.1.0 ships, plus `jffi`, which the WASM runner needs and declares `provided`.

Node boundaries come from byte spans translated to char offsets, which is what makes non-ASCII
source round-trip. `partialScript` is on, because OpenRewrite is routinely handed fragments where
`next`, `break` and `return` appear at the top level; `mainScript` is off, so a shebang naming
something other than ruby is just a comment. Prism will not close a heredoc terminated at EOF
without a trailing newline, so it is always handed a newline-terminated copy of the bytes.

Ruby syntax the visitor has not been taught reaches `defaultVisit`, which throws: the module forbids
`J.Unknown`, so a gap surfaces as a `ParseError` rather than silently degrading.

The port ran in three stages, each with the round-trip suite as its regression net: (A) the legacy
visitor was moved onto JRuby 10.1.1.0 with the legacy front end pinned, revalidating the LST and
printer against a current API surface; (B) `RubyParserVisitor` was retargeted at Prism, which
replaced the line-granular cursor arithmetic with offset anchoring, retired the open-parenthesis
backtracking (`ParenthesesNode`), and let heredoc bodies be claimed off exact offsets; (C) the
remaining syntax gaps were closed — pattern bindings, alternations, pins and guards, the implicit
block parameters `_1`/`_2` and `it`, the `&nil`/`**nil` refusals, `undef`, compact module and class
names, and the `# shareable_constant_value:` wrapper.

## Consequences

- The dependency is Prism itself rather than a Ruby runtime: `prism-parser-api`,
  `prism-parser-wasm`, their Chicory/redline WASM stack and `jffi`.
- Those jars are class file version 65, which sets the module's Java floor at 21. `build.gradle.kts`
  drops the `--release 8` the repo convention applies, and `rewrite-ruby` publishes only an
  `org.gradle.jvm.version=21` variant — so any module depending on it has to target and run Java 21
  as well (or override the `TargetJvmVersion` attribute on the dependency).
- Whitespace, comments and keywords are re-lexed from the source, anchored to node offsets. Every
  byte lands in exactly one prefix, suffix or literal value, and `prefix(node)` asserts it: a desync
  fails with file and offset context instead of corrupting everything downstream.
- Measured over two corpora, every file that is Ruby parses: redmine 1128/1129 and dependabot-core
  2016/2017. The two failures are not Ruby — an ERB template named `.rb`
  (`lib/generators/redmine_plugin_model/templates/migration.rb`) and a fixture that is deliberately
  invalid (`bundler/spec/fixtures/projects/bundler2/invalid_ruby/Gemfile`).
