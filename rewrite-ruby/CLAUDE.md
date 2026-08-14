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

Three Prism behaviors worth knowing: `partialScript` is on so that fragments with a
top-level `next`/`break`/`return` parse, `mainScript` is off so that a shebang naming
something other than ruby is just a comment, and Prism will not close a heredoc whose
terminator ends the file without a trailing newline, so it is always handed a
newline-terminated copy of the bytes.

Prism models no location for several tokens the printer has to put back, and in each
case the source is read at a node offset rather than guessed:

- **Brackets, parentheses and their absence.** An array literal and an implicit array
  (`a, b = 1, 2`) share one node type, as do a parenthesized target list and a bare
  one; they are told apart by the gap the delimiter leaves between the node's own start
  and its first element. A parameter or argument list is written on the same line as
  the name it follows, so a `(` opening the next line is a grouped expression.
- **Operators and index calls.** `a.+(b)` and `x&.[](i)` are ordinary calls that share
  a node with `a + b` and `x[i]`; what the source writes after the receiver decides.
- **The `=` of an endless method**, found by peeking past the parameter list.
- **The `begin` keyword.** The implicit begin of a `def`, block or class body spans the
  whole construct, so only a node starting at the cursor has a `begin` of its own.

Ruby syntax the visitor has not been taught reaches `defaultVisit`, which throws with
the Prism node name. There are no known gaps: every file that is Ruby in the redmine and
dependabot-core corpora parses. `README.md` has the full inventory of what maps where.

`RubyCorpusTest` measures where a new gap would bite. It is skipped unless
`-Druby.corpus.dir=<dir>` is set, and prints a parse rate plus a histogram of failure
causes; alongside its report it writes `<report>.failures` (one `cause<TAB>path` line
per file) and `<report>.messages` (the full message per file), since the histogram
keeps only one sample per cause.

## Mapping notes

**`;` is a statement separator, and lives on the statement it follows.** A `;` after a
statement is the existing `org.openrewrite.java.marker.Semicolon` marker on that
statement's `JRightPadded`, whose suffix holds the space in front of it — the same
place Java puts it, and the only place that prints in the right order. A `;` with
nothing in front of it (`def x; end`, `if c; body end`, the second `;` of `a;;b`) is a
`J.Empty` statement carrying the same marker, so the statement list still accounts for
every byte and recipes see a real element rather than a hidden one. `bodyStatement`
therefore keeps a `J.Block` whenever a separator is present instead of collapsing to
the sole statement.

**An endless method body is a `J.Block` marked `OmitBraces`** holding one statement:
the block prefix is the space before the `=`, the statement prefix the space after it.
`rewrite-scala` prints `def f = expr` the same way.

**A nested destructuring target is `J.Parentheses` around a bracket-less `Rb.Array`**,
which is the same shape as the top-level target list of `Rb.MultipleAssignment`. `for`
loops instead spread their targets across the names of one `J.VariableDeclarations`,
which is what `J.ForEachLoop.Control` can hold.

**A pattern is an expression, and its decorations are types rather than markers.**
`in Integer => n` is `Rb.PatternBinding` (not `Rb.RightwardAssignment`, which spells the
same token with the operands the other way round), `in a | b` is
`Rb.Binary.Type.PatternOr` (not `J.Binary.Type.Or`, which prints `||`), `in ^x` is
`Rb.Unary.Type.Pin` — whose parenthesized form `^(n + 2)` keeps a `J.Parentheses` operand,
since Prism models no node for those parentheses. A guard is `Rb.PatternGuard` standing in
the `J.Case` expression container, because `J.Case` has no guard slot; it carries the
existing `Unless` marker for `in [x] unless c`. `Rb.Unary.getType()` is
operator-dependent: `defined?` is always boolean, a pin is whatever it pins.

**An implicit block parameter writes nothing, so it holds nothing.** A block or lambda
using `_1`/`_2` or `it` gets a Prism parameters node spanning the whole block; `Rb.Block`
keeps `parameters == null` for those and the body refers to the names as ordinary
`J.Identifier`s. `writesParameters` is the guard, and both `visitBlockNode` and
`visitLambdaNode` need it. RSpec's `it "..." do ... end` is unaffected: it is a
`J.MethodInvocation` named `it`, not an identifier.

**`&nil` and `**nil` are the ordinary `&`/`**` shapes over a `nil` identifier**
(`Rb.BlockArgument` and `Rb.Splat`) rather than an identifier holding the whole token.
Prism gives each one node with no child, so the `nil` is read back off the source.

**A compact declaration name keeps every segment.** `Rb.Module.name` is an `Expression`, so
`module Api::V1` is the same `J.MemberReference` that `::` produces in expression position
and `module ::TopLevel` is one with an empty left operand. `J.ClassDeclaration.name` is a
`J.Identifier` upstream and cannot hold that, so `class Api::V1::Photos` keeps the whole
dotted name in one identifier — losing segments is the thing to avoid, and the asymmetry is
the price of not forking `J.ClassDeclaration`.

**Both printers restore the other's cursor.** `RubyPrinter` and its inner
`RubyJavaPrinter` hand each other the cursor to print a subtree; whoever hands it over
puts its own back afterwards. Without that the outer printer is left inside the subtree
it just delegated, and anything that reads its ancestors (`visitCase` choosing between
`when`, `in` and `=>`, `visitVariable` looking for a keyword argument) reads the wrong
ones.

See `README.md` for the type and marker inventory, and
`doc/adr/0012-ruby-parsing-via-jruby.md` for the parser front-end decision.

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
