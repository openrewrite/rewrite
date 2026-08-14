# rewrite-ruby Guidelines

## Parser front end

This module parses Ruby with **Prism**, run standalone: `RubyParser` hands the source
bytes to `org.ruby_lang.prism.wasm.Prism` with explicit `ParsingOptions` and loads the
result with `Loader`. There is no `org.jruby.Ruby` runtime. The `jruby-base` dependency
is kept only because it supplies and pins the Prism artifacts (`jruby-prism` →
`prism-parser-api`/`prism-parser-wasm`).

Compile against `org.ruby_lang.prism.*`. The same classes appear relocated as
`org.jruby.internal.prism.*` inside `jruby-complete` — never depend on that jar.

Three things shape `RubyParserVisitor`:

- **Byte offsets.** Every Prism node carries `startOffset`/`length` as offsets into the
  source *bytes*, while the LST and the printer work on the decoded `String`.
  `PrismSource` owns both and translates between them; that translation is what makes
  non-ASCII source round-trip. Use `prefix(node)` to consume up to a node's start — it
  asserts the cursor landed exactly there, so a desync fails with file and offset
  context instead of corrupting everything downstream.
- **No comments, no token positions.** `ParseResult` has no comment array and the node
  classes carry no `nameLoc`/`operatorLoc`/`openingLoc`, so whitespace, comments and
  keywords are still re-lexed from the source with a linear `int cursor`. Node offsets
  anchor that scanning; `peekWhitespace` survives only for genuinely optional tokens
  (`then`, `do`, trailing commas).
- **Heredocs live outside their node's span.** A heredoc node covers only its `<<~ID`
  marker. Markers are queued as they are seen and their bodies are claimed the first
  time the cursor crosses a newline, then folded into the tree by a final pass keyed by
  node id.

Two Prism behaviors worth knowing: `partialScript` is on so that fragments with a
top-level `next`/`break`/`return` parse, and Prism will not close a heredoc whose
terminator ends the file without a trailing newline, so it is always handed a
newline-terminated copy of the bytes.

Ruby syntax the visitor has not been taught reaches `defaultVisit`, which throws with
the Prism node name. Known gaps: endless method definitions (`def x = expr`), `;` as a
statement separator, `RescueModifierNode`, `MultiTargetNode`, pattern pins and captures.

`RubyCorpusTest` measures where those gaps bite. It is skipped unless
`-Druby.corpus.dir=<dir>` is set, and prints a parse rate plus a histogram of failure
causes.

See `doc/adr/0012-ruby-parsing-via-jruby.md`.

## J.Unknown is forbidden

**NOTHING should map to J.Unknown.** The parser must throw an exception for any
Ruby syntax it cannot map to a proper J-type or Rb-type. This ensures we discover
gaps immediately rather than silently degrading to lossy source-text preservation.

If the parser encounters a node it doesn't handle:
1. Map it to the correct J-type — think carefully about which one fits.
2. If no J-type works, create an Rb-type (Ruby-specific AST node).
3. If the syntax is genuinely new/unknown, **throw an exception** so the gap is
   caught by tests, not silently swallowed.

**Never use J.Unknown, visitUnknown, or raw source text as the value of a J-type
field.** These break the semantic model and prevent recipes from operating on code.

## LST Mapping Rules

**Never fall back to raw source text as the value of a J-type field.** Every AST element
must be mapped to a proper J-type (or Rb-type) with correct structure. Stuffing source
text into an identifier name, unknown source, or string field breaks the semantic model
and prevents recipes from operating on that code.

**Map to the semantically correct type.** Don't use `J.Literal` for interpolated strings
(`"hi #{name}"`) or heredocs — they have internal structure and must be modelled as an
interpolation node holding real expressions. Don't use `J.MethodInvocation` alone to
represent a call whose block argument (`do ... end` vs `{ ... }`) is part of its syntax.
Don't force Ruby's symbols (`:sym`), ranges (`1..5`), or hash literals into whichever J
type is closest — if the right type doesn't exist in J.*, create an Rb.* type.

**Prefer markers over new types for printing-only differences.** Anything that is
structurally a J element but printed differently — `unless` vs `if`, statement modifiers
(`do_it if cond`), `do ... end` vs `{ ... }` block delimiters, parenthesis-less calls,
`and`/`or` vs `&&`/`||`, safe navigation (`&.`) — should be a J element plus a marker,
not a new node type.

**Never store LST elements inside markers.** Markers are metadata that influence how
an LST element is printed, not containers for additional AST subtrees.

## Critical Principles

**Never regress from rich types to J.Unknown.** Once a syntax element has been mapped
to a rich type (J.* or Rb.*), never revert it back to J.Unknown.

**Cursor management:** `RubyParserVisitor.peekWhitespace` is the only backtracking
primitive; it snapshots the cursor and both heredoc maps. Any speculative read must go
through it rather than saving and restoring the cursor by hand. Prefer anchoring to a
node's offsets over speculating at all.

**Whitespace is the parser's responsibility, not the printer's.** Every byte of the
source must land in exactly one prefix, suffix or literal value. `rewriteRun(ruby("..."))`
with no expected output is the assertion that proves it.

## Java version

This module targets Java 21, unlike most of the repo (which compiles to Java 8
bytecode). JRuby 10 ships class file version 65 and cannot load below Java 21, so
there is no Java 8 consumer to protect. `build.gradle.kts` nulls the `--release 8`
that `org.openrewrite.build.java-base` sets. Java 21 language features are fine here.

## Testing

Run tests with: `./gradlew :rewrite-ruby:test`

Keep fixtures inline as `ruby("...")` text blocks rather than `.rb` files on disk —
`.rb` files inside a source set get a `#` license header stamped by `licenseFormat`,
which breaks round-trip assertions. When fixing a parser issue, always add a
round-trip test that verifies `rewriteRun(ruby("..."))`.
