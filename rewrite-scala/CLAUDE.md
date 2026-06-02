# rewrite-scala Guidelines

## J.Unknown is forbidden

**NOTHING should map to J.Unknown.** The parser must throw an exception for any
Scala syntax it cannot map to a proper J-type or S-type. This ensures we discover
gaps immediately rather than silently degrading to lossy source-text preservation.

If the parser encounters a tree node it doesn't handle:
1. Map it to the correct J-type — think carefully about which one fits.
2. If no J-type works, create an S-type (Scala-specific AST node).
3. If the syntax is genuinely new/unknown, **throw an exception** so the gap is
   caught by tests, not silently swallowed.

**Never use J.Unknown, visitUnknown, or raw source text as the value of a J-type
field.** These break the semantic model and prevent recipes from operating on code.

## LST Mapping Rules

**Never fall back to raw source text as the value of a J-type field.** Every AST element
must be mapped to a proper J-type (or S-type) with correct structure. Stuffing source
text into an identifier name, unknown source, or string field breaks the semantic model
and prevents recipes from operating on that code.

**Map to the semantically correct type.** Don't use `J.TypeCast` for type ascription
(`expr: Type`) — that's not a cast. Don't use `J.Literal` for interpolated strings —
they have internal structure. If the right type doesn't exist in J.*, create an S.* type.

**Never store LST elements inside markers.** Markers are metadata that influence how
an LST element is printed (like `Curried`, `OmitBraces`), not containers for additional
AST subtrees.

## Critical Principles

**Never regress from rich types to J.Unknown.** Once a syntax element has been mapped
to a rich type (J.* or S.*), never revert it back to J.Unknown.

**Cursor management:** When a visitor method calls `extractPrefix` or `extractSource`
and then falls back, always restore the cursor first.

## Testing

Run tests with: `./gradlew :rewrite-scala:test`

The `Scala2CompatTest` class covers Scala 2 and 3 compatibility patterns. When fixing
a parser issue, always add a round-trip test that verifies `rewriteRun(scala("..."))`.
