# rewrite-scala Guidelines

## LST Mapping Rules

**Never fall back to raw source text as the value of a J-type field.** Every AST element
must be mapped to a proper J-type (or S-type) with correct structure. Stuffing source
text into an identifier name, unknown source, or string field breaks the semantic model
and prevents recipes from operating on that code.

If a Scala construct doesn't map cleanly to an existing J-type:
1. Think harder about which J-type fits — type bounds, annotations, modifiers, and
   containers are more expressive than they first appear.
2. If no J-type works, consider whether an S-type (Scala-specific AST node) is needed.
3. Ask before resorting to `J.Unknown` — it should only be used as a last resort for
   truly unmappable syntax, and even then the tree inside the Unknown should be
   properly structured when possible.

## Critical Principles

**Never regress from rich types to J.Unknown.** Once a syntax element has been mapped
to a rich type (J.* or S.*), never revert it back to J.Unknown.

**Cursor management:** When a visitor method calls `extractPrefix` or `extractSource`
and then falls back to `visitUnknown`, always restore the cursor first so the Unknown
node gets the correct prefix whitespace.

## Testing

Run tests with: `./gradlew :rewrite-scala:test`

The `Scala2CompatTest` class covers Scala 2 and 3 compatibility patterns. When fixing
a parser issue, always add a round-trip test that verifies `rewriteRun(scala("..."))`.
