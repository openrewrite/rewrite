# rewrite-ruby Guidelines

## Parser front end

This module parses Ruby with JRuby's **legacy `org.jruby.ast` AST**, reached through
`Ruby.parseFile(String, InputStream, DynamicScope)`. JRuby 10 ships two parser front
ends and still defaults to the legacy one, but the Prism provider is on the service
path, so `RubyParser` pins the choice with the `jruby.parser.prism=false` system
property before `JavaEmbedUtils.initialize` and guards the `RootNode` cast.

**Migrating to Prism (`org.ruby_lang.prism`) is the planned next stage.** The legacy
AST exposes only 0-based line numbers — no columns, no offsets — which is why
`RubyParserVisitor` re-lexes the source with its own `int cursor`. It also desugars
exactly the constructs a lossless tree cannot afford to lose: `{x:, y:}` is
indistinguishable from `{x: x, y: y}`, `def f(...)` is exploded into `*`/`**`/`&`
parameters, and pattern captures/pins are flattened into hash pairs and bare variable
reads. Prism gives every node a byte span plus first-class nodes for all of those.

Therefore: **do not build new-syntax support on the legacy visitor.** Ruby 3.1+
constructs (hash shorthand, `it`, endless methods, anonymous argument forwarding,
`Data.define`, one-line pattern matching) are Prism work. Fixes to syntax the legacy
visitor already handles are fair game.

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
primitive; it snapshots both the cursor and the open-heredoc queue. Any speculative
read must go through it rather than saving and restoring the cursor by hand.

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
