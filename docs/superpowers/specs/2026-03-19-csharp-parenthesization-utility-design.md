# C# Parenthesization Utility — Design Spec

## Problem

Across the recipes-csharp repo, 8+ recipes manually construct `new Unary(... Not ...)` with conditional `Parentheses<Expression>` wrapping. Each recipe re-implements its own `NeedsParentheses(expr)` check with inconsistent logic: some always wrap, some dispatch on expression type, some use partial precedence checks. The `WrapInParens` prefix-lifting idiom is copy-pasted in ~6 places.

There is no shared precedence table or parenthesization utility in the C# SDK.

## Prior Art

### Java

Java has two complementary visitors:

- **`ParenthesizeVisitor`** — adds parentheses where needed based on operator precedence and parent context. Contains a precedence table mapping `J.Binary.Type` to integer levels. Provides a static `maybeParenthesize(Expression, Cursor)` entry point used by `JavaTemplate` and `Substitutions` for auto-parenthesization after template substitution.
- **`UnwrapParentheses`** — removes parentheses from a specific `Parentheses` node when safe. Has a static `isUnwrappable(Cursor)` method that checks parent context (can't unwrap structural parens in `if`/`while`/`switch`/`TypeCast`, can't unwrap inside `Unary` when inner is `Binary`/`Ternary`/`Assignment`/`InstanceOf`).

Both share implicit precedence knowledge. Java's `ParenthesizeVisitor` is integrated into `JavaTemplate.doApply()` with a 3-line call.

### JavaScript/TypeScript

JS has no precedence logic. Parentheses are parsed verbatim from source and preserved as-is. Templates substitute without parenthesization.

## Design

Three components sharing a common precedence table.

### 1. `CSharpPrecedences` — Shared Precedence Utility

**File:** `OpenRewrite/CSharp/CSharpPrecedences.cs`

A static class containing the C# operator precedence table and shared decision logic.

#### Precedence Table

Derived from the [C# language specification](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/operators/), higher number = tighter binding:

| Level | Category | Operators / Expression Forms |
|-------|----------|------------------------------|
| 14 | Primary | Member access, invocation, index, `?.`, `?[]`, postfix `++`/`--`, `new`, `typeof`, `sizeof`, `default`, `checked`, `unchecked`, `nameof` |
| 13 | Unary | `+`, `-`, `!`, `~`, prefix `++`/`--`, cast, `await`, `^` (from-end), `not` (pattern) |
| 12 | Range | `..` |
| 11 | Switch/With | `switch` expression, `with` expression |
| 10 | Multiplicative | `*`, `/`, `%` |
| 9 | Additive | `+`, `-` |
| 8 | Shift | `<<`, `>>`, `>>>` |
| 7 | Relational | `<`, `>`, `<=`, `>=`, `is`, `as` |
| 6 | Equality | `==`, `!=` |
| 5 | Bitwise AND | `&` |
| 4 | Bitwise XOR | `^` |
| 3 | Bitwise OR | `|` |
| 2 | Logical AND | `&&` |
| 1 | Logical OR | `||` |
| 0 | Null-coalescing | `??` |
| -1 | Ternary | `? :` |
| -2 | Assignment | `=`, `+=`, `-=`, etc., lambda `=>` |

#### Public Methods

```csharp
// Get the precedence level of any expression node
static int GetPrecedence(Expression expr)

// Determine if child needs parens when placed as operand of parent
static bool NeedsParentheses(Expression child, object parent, bool isRightOperand)

// Wrap expression in Parentheses<Expression> with correct prefix lifting
static Parentheses<Expression> Parenthesize(Expression expr)

// Check if a binary operator is associative (for same-precedence decisions)
static bool IsAssociative(Expression expr)
```

#### `GetPrecedence` Logic

Pattern-matches on expression type:
- `Binary` → maps `Binary.OperatorType` to levels 2-10 (see table above)
- `CsBinary` → maps `As` to 7, `NullCoalescing` to 0. Pattern combinators `And`/`Or` are handled separately (see Pattern Combinator Precedence below)
- `Unary` → 13 for all `Unary.OperatorType` variants
- `CsUnary` → 13 for all `CsUnary.OperatorKind` variants: `SuppressNullableWarning` (null-forgiving `!`), `PointerType`, `AddressOf`, `Spread`, `FromEnd` (`^`), `Not` (pattern `not`)
- `TypeCast` → 13
- `IsPattern` (`Cs.IsPattern`) → 7 (relational level, same as `is` keyword)
- `RangeExpression` (`Cs.RangeExpression`) → 12
- `SwitchExpression` (`Cs.SwitchExpression`) → 11
- `WithExpression` (`Cs.WithExpression`) → 11
- `Ternary` → -1
- `Assignment` → -2
- `AssignmentOperation` → -2 (covers all compound assignments including `??=`, which appears as both `NullCoalescing` and `Coalesce` variants in the enum — both map to -2)
- `Lambda` (`J.Lambda`) → -2
- `Parentheses<Expression>` → int.MaxValue (already parenthesized, never needs more)
- Everything else (identifiers, literals, method invocations, etc.) → int.MaxValue (primary expressions, never need parens)

#### Pattern Combinator Precedence

C# 9+ pattern combinators (`and`, `or`, `not`) operate in a separate precedence domain from expression operators. They appear inside `is` expressions (e.g., `x is > 0 and < 10`). The C# spec defines their relative precedence as: `not` (highest) > `and` > `or` (lowest).

Since pattern combinators never compete with expression-level operators (they only appear within pattern contexts), they are handled as a sub-domain:
- `CsBinary.And` (pattern `and`) → precedence within patterns, not directly comparable to expression levels
- `CsBinary.Or` (pattern `or`) → lower than `and` within patterns
- `CsUnary.Not` (pattern `not`) → highest within patterns

In practice, `NeedsParentheses` treats pattern combinators by comparing within the pattern sub-domain: `or` inside `and` needs parens, `and` inside `or` does not. This avoids conflating pattern precedence with expression-level precedence.

#### `NeedsParentheses` Logic

1. If `child` precedence > `parent` precedence → `false` (child binds tighter)
2. If `child` precedence < `parent` precedence → `true` (child binds too loosely)
3. Equal precedence:
   - Same operator and both associative → `false`
   - Right-associative operators (`??`): `isRightOperand` → `false` (natural grouping is right-to-left)
   - Same mathematical group (add/sub or mul/div/mod) and `isRightOperand` with non-associative operator → `true`
   - Different operators at same level, not in same group → `true` (clarity)

#### `IsAssociative` Logic

Returns `true` for operators where `(a op b) op c == a op (b op c)`:
- `Binary`: `Addition`, `Multiplication`, `BitAnd`, `BitOr`, `BitXor`, `And`, `Or`
- `CsBinary`: `NullCoalescing` (right-associative — treated specially in `NeedsParentheses`)
- All other operators → `false`

#### `Parenthesize` Helper

```csharp
static Parentheses<Expression> Parenthesize(Expression expr)
{
    if (expr is Parentheses<Expression> p) return p;
    return new Parentheses<Expression>(
        Guid.NewGuid(), expr.Prefix, Markers.Empty,
        new JRightPadded<Expression>(expr.WithPrefix(Space.Empty), Space.Empty, Markers.Empty));
}
```

Prefix is lifted from the inner expression to the outer `Parentheses` wrapper — matching the idiom used in all existing recipes.

### 2. `CSharpParenthesizeVisitor` — Adds Parentheses

**File:** `OpenRewrite/CSharp/CSharpParenthesizeVisitor.cs`

Extends `CSharpVisitor<P>`. Mirrors Java's `ParenthesizeVisitor`.

#### Constructor

- Public no-arg: `recursive = true` (full-tree pass)
- Private `(bool recursive)`: for `MaybeParenthesize` single-node checks

#### Visit Methods

Each follows the pattern: optionally recurse, get parent from cursor, delegate to `CSharpPrecedences.NeedsParentheses`:

- **`VisitBinary`** — binary-vs-binary precedence, binary inside unary/ternary/is
- **`VisitCsBinary`** — `as`, `??`, pattern `and`/`or` in same contexts
- **`VisitUnary`** — inside another unary with different operator (except `Not`+`Not` which is `!!` — valid C# null-forgiving)
- **`VisitCsUnary`** — `not` pattern, null-forgiving `!`, `^` from-end, `PointerType`, `AddressOf`, `Spread`
- **`VisitTernary`** — inside binary or unary
- **`VisitTypeCast`** — inside binary, unary, or `is`/`as`
- **`VisitAssignment` / `VisitAssignmentOperation`** — inside binary, unary, or ternary
- **`VisitIsPattern`** — `is` expression inside higher-precedence binary operators
- **`VisitRangeExpression`** — `..` inside higher-precedence contexts
- **`VisitSwitchExpression`** — `switch` expression (may need unconditional parens, like Java)
- **`VisitWithExpression`** — `with` expression in operator contexts

No string concatenation special-casing needed (C# uses interpolation/`string.Format`).

#### Static Entry Point

```csharp
public static Expression MaybeParenthesize(Expression newTree, Cursor cursor)
```

Same algorithm as Java:
1. Fast exit if `newTree` is not a potentially-problematic type. The check must include both shared types (`Binary`, `Unary`, `Ternary`, `Assignment`, `TypeCast`) and C#-specific types (`CsBinary`, `CsUnary`, `IsPattern`, `RangeExpression`, `SwitchExpression`, `WithExpression`)
2. Temporarily give `newTree` the original node's ID
3. Run non-recursive visitor with parent cursor
4. If result is `Parentheses`, reconstruct with fresh ID around original `newTree`

### 3. `CSharpUnwrapParentheses` — Removes Parentheses When Safe

**File:** `OpenRewrite/CSharp/CSharpUnwrapParentheses.cs`

Extends `CSharpVisitor<P>`. Mirrors Java's `UnwrapParentheses`.

#### Constructor

Takes a `Parentheses<Expression> scope` — the specific node to unwrap.

#### `VisitParentheses`

If the node matches scope and `IsUnwrappable` returns true:
- Return inner expression with prefix transferred from the parens wrapper
- Add `Space.SingleSpace` prefix when unwrapping after `return` or `throw` would leave empty prefix

#### `IsUnwrappable(Cursor)` Static Method

- **Cannot unwrap** when parent is structural: `If`, `Switch`, `While`, `DoWhile` (condition), `ForEach`, `ForLoop`, `TypeCast`, `Lock`, `Using`, `FixedStatement`, `CheckedStatement`
- **Cannot unwrap inside `Unary`/`CsUnary`** when inner is: `Binary`, `CsBinary`, `Assignment`, `Ternary`, `AssignmentOperation`, `IsPattern`
- **General case**: delegates to `CSharpPrecedences.NeedsParentheses(inner, parent, isRightOperand)` — if parens are needed, can't unwrap

### 4. Template Integration

**File:** `OpenRewrite/CSharp/Template/CSharpTemplate.cs` — modify `Apply()` method.

Add after `ApplySubstitutions` (between phase 1 and phase 2):

```csharp
// Phase 1.5: auto-parenthesization after substitution
if (tree is Expression expr && cursor.Value is J)
{
    tree = CSharpParenthesizeVisitor.MaybeParenthesize(expr, cursor);
}
```

### 5. Testing Strategy

#### `CSharpPrecedencesTests.cs`

Unit tests for the precedence table using direct AST construction:
- `GetPrecedence` returns correct levels for all operator types
- `NeedsParentheses` handles: lower-prec child in higher-prec parent, equal-prec associativity, right-operand non-associative operators
- `Parenthesize` lifts prefix correctly
- `IsAssociative` returns correct values

#### `CSharpParenthesizeVisitorTests.cs`

Round-trip tests using the "strip all parens, re-add" pattern (a test utility visitor strips all `Parentheses<Expression>` nodes by unconditionally unwrapping, then `CSharpParenthesizeVisitor` re-adds only the necessary ones):
- Arithmetic: `a + b * c` stays as-is, `(a + b) * c` gets re-parenthesized
- Logical: `a || b && c` stays, `(a || b) && c` gets parens
- Mixed: `!(a && b)`, `(a + b) as object`, `a ?? b ? c : d`
- Unary nesting: `!!x` no parens, `-(-x)` gets parens
- Assignment in ternary/binary contexts
- `is` pattern: `(x is int) == true` gets parens, `x is > 0 and < 10` no extra parens
- Range: `a..b + c` vs `(a..b) + c`
- Switch/with expressions in operator contexts
- Null-coalescing right-associativity: `a ?? b ?? c` no parens
- Lambda in binary/ternary contexts
- `MaybeParenthesize` static method tests

#### `CSharpUnwrapParenthesesTests.cs`

- Safe unwrap: `(a) + b` → `a + b`, `return (x)` → `return x`
- Unsafe (no change): `!(a + b)`, `(a + b) * c`, structural parens in `if`/`while`
- Prefix preservation: space after `return`/`throw`

#### Template Integration Test

- `CSharpTemplate.Expression($"!{expr}")` where `expr` is `a || b` → produces `!(a || b)`
- `CSharpTemplate.Expression($"{a} + {b}")` where `a` is `x * y` → produces `x * y + ...` (no unnecessary parens)

## Files

| File | Action | Purpose |
|------|--------|---------|
| `OpenRewrite/CSharp/CSharpPrecedences.cs` | Create | Precedence table + shared logic |
| `OpenRewrite/CSharp/CSharpParenthesizeVisitor.cs` | Create | Adds parens visitor + `MaybeParenthesize` |
| `OpenRewrite/CSharp/CSharpUnwrapParentheses.cs` | Create | Removes parens visitor + `IsUnwrappable` |
| `OpenRewrite/CSharp/Template/CSharpTemplate.cs` | Modify | 4-line auto-parenthesization addition |
| `OpenRewrite/Tests/CSharp/CSharpPrecedencesTests.cs` | Create | Precedence table unit tests |
| `OpenRewrite/Tests/CSharp/CSharpParenthesizeVisitorTests.cs` | Create | Round-trip parenthesization tests |
| `OpenRewrite/Tests/CSharp/CSharpUnwrapParenthesesTests.cs` | Create | Unwrap safety tests |

## Design Constraints

- Precedence table strictly follows the C# language specification
- No Java-isms: no string concatenation special-casing, no `instanceof` (C# uses `is`), separate equality/relational levels
- `??` is right-associative — `a ?? b ?? c` is `a ?? (b ?? c)`, no parens needed
- Pattern combinators `and`/`or`/`not` have defined precedence in C# 9+ and must be handled
- Null-forgiving `!` (postfix, `CsUnary.SuppressNullableWarning`) is primary-level, distinct from logical `!` (prefix, `Unary.Not`)
- `await` is not modeled as `CsUnary` in the AST — it does not need explicit handling in the precedence table
