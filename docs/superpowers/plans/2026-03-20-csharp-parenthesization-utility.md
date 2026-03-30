# C# Parenthesization Utility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add precedence-aware parenthesization and unwrapping utilities to the C# SDK so recipe authors don't manually decide when parentheses are needed.

**Architecture:** Three components — `CSharpPrecedences` (internal precedence table + decision logic), `CSharpParenthesizeVisitor` (adds parens, integrates with templates), `CSharpUnwrapParentheses` (removes parens when safe). All live in `OpenRewrite/CSharp/`. TDD with direct AST construction for unit tests and parsed C# for round-trip tests.

**Tech Stack:** C# / .NET 10, xUnit, OpenRewrite C# SDK AST types (`J.*`, `Cs.*`)

**Spec:** `docs/superpowers/specs/2026-03-19-csharp-parenthesization-utility-design.md`

**Key reference files:**
- Java prior art: `rewrite-java/src/main/java/org/openrewrite/java/ParenthesizeVisitor.java`
- Java prior art: `rewrite-java/src/main/java/org/openrewrite/java/UnwrapParentheses.java`
- C# AST types: `rewrite-csharp/csharp/OpenRewrite/Java/J.cs` (Binary, Unary, Ternary, TypeCast, Assignment, AssignmentOperation, Parentheses, Lambda)
- C# AST types: `rewrite-csharp/csharp/OpenRewrite/CSharp/Cs.cs` (CsBinary, CsUnary, IsPattern, RangeExpression, SwitchExpression, WithExpression)
- C# visitor: `rewrite-csharp/csharp/OpenRewrite/CSharp/CSharpVisitor.cs`
- Existing helpers: `rewrite-csharp/csharp/OpenRewrite/CSharp/Cs.Helpers.cs`
- Test helpers: `rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/TestHelpers.cs`
- Template engine: `rewrite-csharp/csharp/OpenRewrite/CSharp/Template/CSharpTemplate.cs`

**Build/test commands:**
```bash
# All paths relative to rewrite-csharp/csharp/
# Run all C# tests
dotnet test --verbosity normal

# Run specific test class
dotnet test --filter "FullyQualifiedName~OpenRewrite.Tests.CSharp.CSharpPrecedencesTests"

# Run specific test method
dotnet test --filter "FullyQualifiedName~OpenRewrite.Tests.CSharp.CSharpPrecedencesTests.GetPrecedence_BinaryMultiplication"

# Apply license headers (run from repo root)
./gradlew licenseFormatCsharp
```

**Important conventions:**
- All files need the Moderne Source Available License header (see any existing `.cs` file for format)
- Use `gw` instead of `./gradlew` when running Gradle commands
- AST nodes use `Guid.NewGuid()` for IDs, `Space.Empty` for no whitespace, `Markers.Empty` for no markers
- `J.SetPrefix<T>(node, prefix)` is the generic way to set prefix on any J node via reflection
- `expr.WithPrefix(Space.Empty)` works on concrete types that have `WithPrefix`
- `cursor.ParentTree` is the C# equivalent of Java's `getParentTreeCursor()`
- `Visit(tree, p, parentCursor)` sets the cursor to `parentCursor` before visiting
- There is no `IsScope` method in C# — use `node.Id == other.Id` for identity checks
- `CSharpPrecedences` should be `internal static` (not public)

---

### Task 1: CSharpPrecedences — Precedence Table and Helpers

**Files:**
- Create: `rewrite-csharp/csharp/OpenRewrite/CSharp/CSharpPrecedences.cs`
- Create: `rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/CSharpPrecedencesTests.cs`
- Modify: `rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/TestHelpers.cs` (add binary/unary construction helpers)

This task builds the foundation: the precedence table, `GetPrecedence`, `NeedsParentheses`, `Parenthesize`, and `IsAssociative`. Everything else depends on this.

#### Step 1: Add AST construction helpers to TestHelpers

- [ ] **Step 1.1: Add helpers to TestHelpers.cs**

Add these helpers to `rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/TestHelpers.cs` for constructing AST nodes in tests. Add `using OpenRewrite.CSharp;` to the imports.

```csharp
public static Binary MakeBinary(Binary.OperatorType op, Expression left, Expression right) =>
    new(Guid.NewGuid(), Space.Empty, Markers.Empty, left,
        new JLeftPadded<Binary.OperatorType>(Space.Empty, op, Markers.Empty),
        right, null);

public static CsBinary MakeCsBinary(CsBinary.OperatorType op, Expression left, Expression right) =>
    new(Guid.NewGuid(), Space.Empty, Markers.Empty, left,
        new JLeftPadded<CsBinary.OperatorType>(Space.Empty, op, Markers.Empty),
        right, null);

public static Unary MakeUnary(Unary.OperatorType op, Expression expr) =>
    new(Guid.NewGuid(), Space.Empty, Markers.Empty,
        new JLeftPadded<Unary.OperatorType>(Space.Empty, op, Markers.Empty),
        expr, null);

public static Ternary MakeTernary(Expression cond, Expression trueExpr, Expression falseExpr) =>
    new(Guid.NewGuid(), Space.Empty, Markers.Empty, cond,
        new JLeftPadded<Expression>(Space.Empty, trueExpr, Markers.Empty),
        new JLeftPadded<Expression>(Space.Empty, falseExpr, Markers.Empty),
        null);

public static TypeCast MakeTypeCast(Expression expr) =>
    new(Guid.NewGuid(), Space.Empty, Markers.Empty,
        new ControlParentheses<TypeTree>(Guid.NewGuid(), Space.Empty, Markers.Empty,
            new JRightPadded<TypeTree>(MakeId("int"), Space.Empty, Markers.Empty)),
        expr);

public static Assignment MakeAssignment(Expression variable, Expression assignment) =>
    new(Guid.NewGuid(), Space.Empty, Markers.Empty, variable,
        new JLeftPadded<Expression>(Space.Empty, assignment, Markers.Empty),
        null);

public static Parentheses<Expression> MakeParens(Expression expr) =>
    new(Guid.NewGuid(), Space.Empty, Markers.Empty,
        new JRightPadded<Expression>(expr, Space.Empty, Markers.Empty));
```

Verify these compile by running:
```bash
cd rewrite-csharp/csharp && dotnet build
```

- [ ] **Step 1.2: Commit**

```bash
git add rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/TestHelpers.cs
git commit -m "Add AST construction helpers to TestHelpers for precedence tests"
```

#### Step 2: Write failing tests for GetPrecedence

- [ ] **Step 2.1: Create test file with GetPrecedence tests**

Create `rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/CSharpPrecedencesTests.cs`:

```csharp
/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using static OpenRewrite.Tests.CSharp.TestHelpers;

namespace OpenRewrite.Tests.CSharp;

public class CSharpPrecedencesTests
{
    // =============================================================
    // GetPrecedence
    // =============================================================

    [Theory]
    [InlineData(Binary.OperatorType.Multiplication, 10)]
    [InlineData(Binary.OperatorType.Division, 10)]
    [InlineData(Binary.OperatorType.Modulo, 10)]
    [InlineData(Binary.OperatorType.Addition, 9)]
    [InlineData(Binary.OperatorType.Subtraction, 9)]
    [InlineData(Binary.OperatorType.LeftShift, 8)]
    [InlineData(Binary.OperatorType.RightShift, 8)]
    [InlineData(Binary.OperatorType.LessThan, 7)]
    [InlineData(Binary.OperatorType.GreaterThan, 7)]
    [InlineData(Binary.OperatorType.LessThanOrEqual, 7)]
    [InlineData(Binary.OperatorType.GreaterThanOrEqual, 7)]
    [InlineData(Binary.OperatorType.Equal, 6)]
    [InlineData(Binary.OperatorType.NotEqual, 6)]
    [InlineData(Binary.OperatorType.BitAnd, 5)]
    [InlineData(Binary.OperatorType.BitXor, 4)]
    [InlineData(Binary.OperatorType.BitOr, 3)]
    [InlineData(Binary.OperatorType.And, 2)]
    [InlineData(Binary.OperatorType.Or, 1)]
    public void GetPrecedence_Binary(Binary.OperatorType op, int expected)
    {
        var expr = MakeBinary(op, MakeId("a"), MakeId("b"));
        Assert.Equal(expected, CSharpPrecedences.GetPrecedence(expr));
    }

    [Fact]
    public void GetPrecedence_CsBinary_NullCoalescing()
    {
        var expr = MakeCsBinary(CsBinary.OperatorType.NullCoalescing, MakeId("a"), MakeId("b"));
        Assert.Equal(0, CSharpPrecedences.GetPrecedence(expr));
    }

    [Fact]
    public void GetPrecedence_Ternary_WithNullCoalescingMarker()
    {
        // ?? is parsed as Ternary with NullCoalescing marker, not CsBinary
        var expr = new Ternary(Guid.NewGuid(), Space.Empty,
            Markers.Build([NullCoalescing.Instance]),
            MakeId("a"),
            new JLeftPadded<Expression>(Space.Empty, new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty)),
            new JLeftPadded<Expression>(Space.Empty, MakeId("b")),
            null);
        Assert.Equal(0, CSharpPrecedences.GetPrecedence(expr));
    }

    [Fact]
    public void GetPrecedence_CsBinary_As()
    {
        var expr = MakeCsBinary(CsBinary.OperatorType.As, MakeId("a"), MakeId("b"));
        Assert.Equal(7, CSharpPrecedences.GetPrecedence(expr));
    }

    [Fact]
    public void GetPrecedence_Unary()
    {
        var expr = MakeUnary(Unary.OperatorType.Not, MakeId("a"));
        Assert.Equal(13, CSharpPrecedences.GetPrecedence(expr));
    }

    [Fact]
    public void GetPrecedence_Ternary()
    {
        var expr = MakeTernary(MakeId("a"), MakeId("b"), MakeId("c"));
        Assert.Equal(-1, CSharpPrecedences.GetPrecedence(expr));
    }

    [Fact]
    public void GetPrecedence_TypeCast()
    {
        var expr = MakeTypeCast(MakeId("a"));
        Assert.Equal(13, CSharpPrecedences.GetPrecedence(expr));
    }

    [Fact]
    public void GetPrecedence_Assignment()
    {
        var expr = MakeAssignment(MakeId("a"), MakeId("b"));
        Assert.Equal(-2, CSharpPrecedences.GetPrecedence(expr));
    }

    [Fact]
    public void GetPrecedence_Parenthesized_ReturnsMaxValue()
    {
        var expr = MakeParens(MakeId("a"));
        Assert.Equal(int.MaxValue, CSharpPrecedences.GetPrecedence(expr));
    }

    [Fact]
    public void GetPrecedence_Identifier_ReturnsMaxValue()
    {
        var expr = MakeId("a");
        Assert.Equal(int.MaxValue, CSharpPrecedences.GetPrecedence(expr));
    }

    // =============================================================
    // NeedsParentheses
    // =============================================================

    [Fact]
    public void NeedsParentheses_LowerPrecChildInHigherPrecParent()
    {
        // a + b inside * context → needs parens
        var child = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), MakeId("b"));
        var parent = MakeBinary(Binary.OperatorType.Multiplication, child, MakeId("c"));
        Assert.True(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: false));
    }

    [Fact]
    public void NeedsParentheses_HigherPrecChildInLowerPrecParent()
    {
        // a * b inside + context → no parens
        var child = MakeBinary(Binary.OperatorType.Multiplication, MakeId("a"), MakeId("b"));
        var parent = MakeBinary(Binary.OperatorType.Addition, child, MakeId("c"));
        Assert.False(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: false));
    }

    [Fact]
    public void NeedsParentheses_SameAssociativeOp_NoParens()
    {
        // a + b inside + context, left operand → no parens
        var child = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), MakeId("b"));
        var parent = MakeBinary(Binary.OperatorType.Addition, child, MakeId("c"));
        Assert.False(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: false));
    }

    [Fact]
    public void NeedsParentheses_SubtractionOnRight_NeedsParens()
    {
        // a - b as right operand of + → needs parens: a + (b - c)
        var child = MakeBinary(Binary.OperatorType.Subtraction, MakeId("b"), MakeId("c"));
        var parent = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), child);
        Assert.True(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: true));
    }

    [Fact]
    public void NeedsParentheses_SubtractionOnLeft_NoParens()
    {
        // a - b as left operand of + → no parens (left-to-right evaluation is correct)
        var child = MakeBinary(Binary.OperatorType.Subtraction, MakeId("a"), MakeId("b"));
        var parent = MakeBinary(Binary.OperatorType.Addition, child, MakeId("c"));
        Assert.False(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: false));
    }

    [Fact]
    public void NeedsParentheses_NullCoalescingRightAssociative_NoParens()
    {
        // a ?? b as right operand of ?? → no parens (right-associative)
        // ?? is parsed as Ternary with NullCoalescing marker
        Ternary MakeNullCoalescing(Expression left, Expression right) =>
            new(Guid.NewGuid(), Space.Empty,
                Markers.Build([NullCoalescing.Instance]),
                left,
                new JLeftPadded<Expression>(Space.Empty, new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty)),
                new JLeftPadded<Expression>(Space.Empty, right),
                null);

        var child = MakeNullCoalescing(MakeId("b"), MakeId("c"));
        var parent = MakeNullCoalescing(MakeId("a"), child);
        Assert.False(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: true));
    }

    [Fact]
    public void NeedsParentheses_DifferentOpsAtSameLevel_NeedsParens()
    {
        // a == b inside != context → needs parens for clarity
        var child = MakeBinary(Binary.OperatorType.Equal, MakeId("a"), MakeId("b"));
        var parent = MakeBinary(Binary.OperatorType.NotEqual, child, MakeId("c"));
        Assert.True(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: false));
    }

    // =============================================================
    // Parenthesize
    // =============================================================

    [Fact]
    public void Parenthesize_LiftsPrefix()
    {
        var id = new Identifier(Guid.NewGuid(), new Space("\n", []), Markers.Empty, [], "x", null, null);
        var result = CSharpPrecedences.Parenthesize(id);

        Assert.IsType<Parentheses<Expression>>(result);
        // Prefix moved to outer Parentheses
        Assert.Equal("\n", result.Prefix.Whitespace);
        // Inner expression has empty prefix
        Assert.Equal(Space.Empty, result.Tree.Element.Prefix);
    }

    [Fact]
    public void Parenthesize_AlreadyParenthesized_ReturnsSame()
    {
        var parens = MakeParens(MakeId("x"));
        var result = CSharpPrecedences.Parenthesize(parens);
        Assert.Same(parens, result);
    }

    // =============================================================
    // IsAssociative
    // =============================================================

    [Theory]
    [InlineData(Binary.OperatorType.Addition, true)]
    [InlineData(Binary.OperatorType.Multiplication, true)]
    [InlineData(Binary.OperatorType.BitAnd, true)]
    [InlineData(Binary.OperatorType.BitOr, true)]
    [InlineData(Binary.OperatorType.BitXor, true)]
    [InlineData(Binary.OperatorType.And, true)]
    [InlineData(Binary.OperatorType.Or, true)]
    [InlineData(Binary.OperatorType.Subtraction, false)]
    [InlineData(Binary.OperatorType.Division, false)]
    [InlineData(Binary.OperatorType.Modulo, false)]
    public void IsAssociative_Binary(Binary.OperatorType op, bool expected)
    {
        var expr = MakeBinary(op, MakeId("a"), MakeId("b"));
        Assert.Equal(expected, CSharpPrecedences.IsAssociative(expr));
    }
}
```

- [ ] **Step 2.2: Run tests to verify they fail**

```bash
cd rewrite-csharp/csharp && dotnet test --filter "FullyQualifiedName~CSharpPrecedencesTests" --verbosity normal
```

Expected: Compilation error — `CSharpPrecedences` does not exist.

#### Step 3: Implement CSharpPrecedences

- [ ] **Step 3.1: Create CSharpPrecedences.cs**

Create `rewrite-csharp/csharp/OpenRewrite/CSharp/CSharpPrecedences.cs`:

```csharp
/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// C# operator precedence table and parenthesization decision logic.
/// Derived from the C# language specification.
/// Higher precedence number = tighter binding.
/// </summary>
internal static class CSharpPrecedences
{
    // Precedence levels from the C# spec (higher = tighter binding)
    // 14: Primary (member access, invocation, index, postfix ++/--)
    // 13: Unary (+, -, !, ~, prefix ++/--, cast, await, ^ from-end, not pattern)
    // 12: Range (..)
    // 11: Switch/With expressions
    // 10: Multiplicative (*, /, %)
    //  9: Additive (+, -)
    //  8: Shift (<<, >>, >>>)
    //  7: Relational (<, >, <=, >=, is, as)
    //  6: Equality (==, !=)
    //  5: Bitwise AND (&)
    //  4: Bitwise XOR (^)
    //  3: Bitwise OR (|)
    //  2: Logical AND (&&)
    //  1: Logical OR (||)
    //  0: Null-coalescing (??)
    // -1: Ternary (?:)
    // -2: Assignment (=, +=, etc.), Lambda (=>)

    public static int GetPrecedence(Expression expr) => expr switch
    {
        Parentheses<Expression> => int.MaxValue,

        Binary b => b.Operator.Element switch
        {
            Binary.OperatorType.Multiplication or
            Binary.OperatorType.Division or
            Binary.OperatorType.Modulo => 10,

            Binary.OperatorType.Addition or
            Binary.OperatorType.Subtraction => 9,

            Binary.OperatorType.LeftShift or
            Binary.OperatorType.RightShift or
            Binary.OperatorType.UnsignedRightShift => 8,

            Binary.OperatorType.LessThan or
            Binary.OperatorType.GreaterThan or
            Binary.OperatorType.LessThanOrEqual or
            Binary.OperatorType.GreaterThanOrEqual => 7,

            Binary.OperatorType.Equal or
            Binary.OperatorType.NotEqual => 6,

            Binary.OperatorType.BitAnd => 5,
            Binary.OperatorType.BitXor => 4,
            Binary.OperatorType.BitOr => 3,
            Binary.OperatorType.And => 2,
            Binary.OperatorType.Or => 1,

            _ => 0
        },

        CsBinary csb => csb.Operator.Element switch
        {
            CsBinary.OperatorType.As => 7,
            CsBinary.OperatorType.NullCoalescing => 0,
            // Pattern combinators: and > or within pattern domain
            CsBinary.OperatorType.And => 2,
            CsBinary.OperatorType.Or => 1,
            _ => 0
        },

        Unary => 13,
        CsUnary => 13,
        TypeCast => 13,

        IsPattern => 7,
        RangeExpression => 12,
        SwitchExpression => 11,
        WithExpression => 11,

        // ?? is modeled as Ternary with NullCoalescing marker, not CsBinary
        Ternary t when t.Markers.MarkerList.Any(m => m is NullCoalescing) => 0,
        Ternary => -1,
        Assignment => -2,
        AssignmentOperation => -2,
        Lambda => -2,

        _ => int.MaxValue
    };

    public static bool NeedsParentheses(Expression child, object parent, bool isRightOperand)
    {
        if (parent is not Expression parentExpr)
            return false;

        int childPrec = GetPrecedence(child);
        int parentPrec = GetPrecedence(parentExpr);

        if (childPrec > parentPrec) return false;
        if (childPrec < parentPrec) return true;

        // Equal precedence — check associativity and grouping

        // Right-associative operators: ?? on the right side doesn't need parens
        if (isRightOperand && IsRightAssociative(child))
            return false;

        // Same operator, both associative → no parens
        if (IsSameOperator(child, parentExpr) && IsAssociative(child))
            return false;

        // Same mathematical group (add/sub or mul/div/mod)
        if (IsInSameMathGroup(child, parentExpr))
        {
            // Right operand with non-associative op or different op → needs parens
            return isRightOperand;
        }

        // Different operators at same precedence, not in same group → parens for clarity
        return !IsSameOperator(child, parentExpr);
    }

    public static Parentheses<Expression> Parenthesize(Expression expr)
    {
        if (expr is Parentheses<Expression> p) return p;

        return new Parentheses<Expression>(
            Guid.NewGuid(), expr.Prefix, Markers.Empty,
            new JRightPadded<Expression>(
                J.SetPrefix(expr, Space.Empty), Space.Empty, Markers.Empty));
    }

    public static bool IsAssociative(Expression expr) => expr switch
    {
        Binary b => b.Operator.Element is
            Binary.OperatorType.Addition or
            Binary.OperatorType.Multiplication or
            Binary.OperatorType.BitAnd or
            Binary.OperatorType.BitOr or
            Binary.OperatorType.BitXor or
            Binary.OperatorType.And or
            Binary.OperatorType.Or,
        // ?? is modeled as Ternary with NullCoalescing marker
        Ternary t when t.Markers.MarkerList.Any(m => m is NullCoalescing) => true,
        CsBinary csb => csb.Operator.Element is CsBinary.OperatorType.NullCoalescing,
        _ => false
    };

    private static bool IsRightAssociative(Expression expr) => expr switch
    {
        // ?? is modeled as Ternary with NullCoalescing marker
        Ternary t when t.Markers.MarkerList.Any(m => m is NullCoalescing) => true,
        CsBinary csb => csb.Operator.Element is CsBinary.OperatorType.NullCoalescing,
        Assignment => true,
        AssignmentOperation => true,
        _ => false
    };

    private static bool IsSameOperator(Expression a, Expression b) => (a, b) switch
    {
        (Binary ba, Binary bb) => ba.Operator.Element == bb.Operator.Element,
        (CsBinary ca, CsBinary cb) => ca.Operator.Element == cb.Operator.Element,
        _ => false
    };

    private static bool IsInSameMathGroup(Expression a, Expression b)
    {
        if (a is not Binary ba || b is not Binary bb) return false;
        return (IsAddSub(ba.Operator.Element) && IsAddSub(bb.Operator.Element)) ||
               (IsMulDivMod(ba.Operator.Element) && IsMulDivMod(bb.Operator.Element));
    }

    private static bool IsAddSub(Binary.OperatorType op) =>
        op is Binary.OperatorType.Addition or Binary.OperatorType.Subtraction;

    private static bool IsMulDivMod(Binary.OperatorType op) =>
        op is Binary.OperatorType.Multiplication or Binary.OperatorType.Division or Binary.OperatorType.Modulo;
}
```

Note: `CSharpPrecedences` is `internal static`. Test project needs `InternalsVisibleTo`. Check if it already exists; if not, add `[assembly: InternalsVisibleTo("OpenRewrite.Tests")]` or equivalent. Look at the `.csproj` files to determine the test project assembly name and whether `InternalsVisibleTo` is already configured.

- [ ] **Step 3.2: Run tests**

```bash
cd rewrite-csharp/csharp && dotnet test --filter "FullyQualifiedName~CSharpPrecedencesTests" --verbosity normal
```

Expected: All tests pass. If `internal` visibility is an issue, add `InternalsVisibleTo` to the main project's `AssemblyInfo.cs` or `.csproj`.

- [ ] **Step 3.3: Commit**

```bash
git add rewrite-csharp/csharp/OpenRewrite/CSharp/CSharpPrecedences.cs rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/CSharpPrecedencesTests.cs
git commit -m "Add CSharpPrecedences: precedence table and decision logic"
```

---

### Task 2: CSharpParenthesizeVisitor — Adds Parentheses

**Files:**
- Create: `rewrite-csharp/csharp/OpenRewrite/CSharp/CSharpParenthesizeVisitor.cs`
- Create: `rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/CSharpParenthesizeVisitorTests.cs`

**Depends on:** Task 1

#### Step 1: Write failing tests

- [ ] **Step 1.1: Create test file**

Create `rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/CSharpParenthesizeVisitorTests.cs`. These tests use direct AST construction with a manual "strip-then-re-add" approach. The strip phase unwraps all `Parentheses<Expression>` nodes, then the `CSharpParenthesizeVisitor` re-adds only necessary ones.

```csharp
/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * ...license header...
 */
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using static OpenRewrite.Tests.CSharp.TestHelpers;

namespace OpenRewrite.Tests.CSharp;

public class CSharpParenthesizeVisitorTests
{
    // =============================================================
    // MaybeParenthesize — static entry point
    // =============================================================

    [Fact]
    public void MaybeParenthesize_BinaryInsideUnary_AddsParens()
    {
        // Substituting (a || b) into the operand of !
        var inner = MakeBinary(Binary.OperatorType.Or, MakeId("a"), MakeId("b"));
        var originalOperand = MakeId("placeholder");
        var unary = MakeUnary(Unary.OperatorType.Not, originalOperand);

        var cursor = new Cursor(new Cursor(null, "root"), unary);
        cursor = new Cursor(cursor, originalOperand);

        var result = CSharpParenthesizeVisitor.MaybeParenthesize(inner, cursor);
        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_IdentifierInsideUnary_NoParens()
    {
        // Substituting a simple identifier into ! — no parens needed
        var inner = MakeId("x");
        var originalOperand = MakeId("placeholder");
        var unary = MakeUnary(Unary.OperatorType.Not, originalOperand);

        var cursor = new Cursor(new Cursor(null, "root"), unary);
        cursor = new Cursor(cursor, originalOperand);

        var result = CSharpParenthesizeVisitor.MaybeParenthesize(inner, cursor);
        // Should return the original expression, not wrapped
        Assert.IsType<Identifier>(result);
    }

    [Fact]
    public void MaybeParenthesize_LowPrecBinaryInsideHighPrecBinary()
    {
        // Substituting (a + b) into left of * → needs parens
        var inner = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), MakeId("b"));
        var originalLeft = MakeId("placeholder");
        var outer = MakeBinary(Binary.OperatorType.Multiplication, originalLeft, MakeId("c"));

        var cursor = new Cursor(new Cursor(null, "root"), outer);
        cursor = new Cursor(cursor, originalLeft);

        var result = CSharpParenthesizeVisitor.MaybeParenthesize(inner, cursor);
        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_HighPrecBinaryInsideLowPrecBinary_NoParens()
    {
        // Substituting (a * b) into left of + → no parens needed
        var inner = MakeBinary(Binary.OperatorType.Multiplication, MakeId("a"), MakeId("b"));
        var originalLeft = MakeId("placeholder");
        var outer = MakeBinary(Binary.OperatorType.Addition, originalLeft, MakeId("c"));

        var cursor = new Cursor(new Cursor(null, "root"), outer);
        cursor = new Cursor(cursor, originalLeft);

        var result = CSharpParenthesizeVisitor.MaybeParenthesize(inner, cursor);
        Assert.IsType<Binary>(result);
    }

    [Fact]
    public void MaybeParenthesize_TernaryInsideBinary_AddsParens()
    {
        var inner = MakeTernary(MakeId("a"), MakeId("b"), MakeId("c"));
        var originalLeft = MakeId("placeholder");
        var outer = MakeBinary(Binary.OperatorType.Addition, originalLeft, MakeId("d"));

        var cursor = new Cursor(new Cursor(null, "root"), outer);
        cursor = new Cursor(cursor, originalLeft);

        var result = CSharpParenthesizeVisitor.MaybeParenthesize(inner, cursor);
        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_TypeCastInsideUnary_AddsParens()
    {
        var inner = MakeTypeCast(MakeId("x"));
        var originalOperand = MakeId("placeholder");
        var unary = MakeUnary(Unary.OperatorType.Negative, originalOperand);

        var cursor = new Cursor(new Cursor(null, "root"), unary);
        cursor = new Cursor(cursor, originalOperand);

        var result = CSharpParenthesizeVisitor.MaybeParenthesize(inner, cursor);
        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_AssignmentInsideBinary_AddsParens()
    {
        var inner = MakeAssignment(MakeId("x"), MakeId("1"));
        var originalLeft = MakeId("placeholder");
        var outer = MakeBinary(Binary.OperatorType.Addition, originalLeft, MakeId("y"));

        var cursor = new Cursor(new Cursor(null, "root"), outer);
        cursor = new Cursor(cursor, originalLeft);

        var result = CSharpParenthesizeVisitor.MaybeParenthesize(inner, cursor);
        Assert.IsType<Parentheses<Expression>>(result);
    }
}
```

- [ ] **Step 1.2: Run tests to verify they fail**

```bash
cd rewrite-csharp/csharp && dotnet test --filter "FullyQualifiedName~CSharpParenthesizeVisitorTests" --verbosity normal
```

Expected: Compilation error — `CSharpParenthesizeVisitor` does not exist.

#### Step 2: Implement CSharpParenthesizeVisitor

- [ ] **Step 2.1: Create CSharpParenthesizeVisitor.cs**

Create `rewrite-csharp/csharp/OpenRewrite/CSharp/CSharpParenthesizeVisitor.cs`:

```csharp
/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * ...license header...
 */
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Static helper providing <see cref="MaybeParenthesize"/> for template integration
/// and point-use by recipes.
/// </summary>
public static class CSharpParenthesizeVisitor
{
    /// <summary>
    /// Determines whether an expression needs to be parenthesized when replacing another
    /// expression in the tree.
    /// </summary>
    public static Expression MaybeParenthesize(Expression newTree, Cursor cursor)
    {
        if (newTree is not (Binary or Unary or Ternary or Assignment or AssignmentOperation
            or TypeCast or CsBinary or CsUnary or IsPattern or RangeExpression
            or SwitchExpression or WithExpression or Lambda))
        {
            return newTree;
        }

        var originalTree = cursor.Value;
        if (originalTree is not Tree original) return newTree;

        var newTreeWithOriginalId = (Expression)((Tree)newTree).WithId(original.Id);

        var result = new CSharpParenthesizeVisitor<int>(false)
            .Visit(newTreeWithOriginalId, 0, cursor.Parent!);

        if (result is Parentheses<Expression>)
        {
            return CSharpPrecedences.Parenthesize(newTree);
        }

        return newTree;
    }
}

/// <summary>
/// Visitor that adds parentheses to C# expressions where needed based on operator precedence
/// and context to ensure correct evaluation order.
/// </summary>
public class CSharpParenthesizeVisitor<P> : CSharpVisitor<P>
{
    private readonly bool _recursive;

    public CSharpParenthesizeVisitor()
    {
        _recursive = true;
    }

    internal CSharpParenthesizeVisitor(bool recursive)
    {
        _recursive = recursive;
    }

    public override J VisitBinary(Binary binary, P p)
    {
        var j = _recursive ? base.VisitBinary(binary, p) : binary;
        if (j is not Binary b) return j;

        var parent = Cursor.ParentTree;
        if (NeedsParenthesesInContext(b, parent))
            return CSharpPrecedences.Parenthesize(b);

        return b;
    }

    public override J VisitCsBinary(CsBinary csBinary, P p)
    {
        var j = _recursive ? base.VisitCsBinary(csBinary, p) : csBinary;
        if (j is not CsBinary csb) return j;

        var parent = Cursor.ParentTree;
        if (NeedsParenthesesInContext(csb, parent))
            return CSharpPrecedences.Parenthesize(csb);

        return csb;
    }

    public override J VisitUnary(Unary unary, P p)
    {
        var j = _recursive ? base.VisitUnary(unary, p) : unary;
        if (j is not Unary u) return j;

        var parent = Cursor.ParentTree;
        if (parent.Value is Unary parentUnary)
        {
            // !!x is valid (double logical NOT), no parens
            if (u.Operator.Element == Unary.OperatorType.Not &&
                parentUnary.Operator.Element == Unary.OperatorType.Not)
                return u;
            // Different unary operators: -(-x), ~(-x) etc. → parens
            if (u.Operator.Element != parentUnary.Operator.Element)
                return CSharpPrecedences.Parenthesize(u);
        }
        else if (NeedsParenthesesInContext(u, parent))
        {
            return CSharpPrecedences.Parenthesize(u);
        }

        return u;
    }

    public override J VisitCsUnary(CsUnary csUnary, P p)
    {
        var j = _recursive ? base.VisitCsUnary(csUnary, p) : csUnary;
        if (j is not CsUnary csu) return j;

        var parent = Cursor.ParentTree;
        if (NeedsParenthesesInContext(csu, parent))
            return CSharpPrecedences.Parenthesize(csu);

        return csu;
    }

    public override J VisitTernary(Ternary ternary, P p)
    {
        var j = _recursive ? base.VisitTernary(ternary, p) : ternary;
        if (j is not Ternary t) return j;

        var parent = Cursor.ParentTree;
        if (parent.Value is Binary or Unary or CsBinary or CsUnary or IsPattern)
            return CSharpPrecedences.Parenthesize(t);

        return t;
    }

    public override J VisitTypeCast(TypeCast cast, P p)
    {
        var j = _recursive ? base.VisitTypeCast(cast, p) : cast;
        if (j is not TypeCast tc) return j;

        var parent = Cursor.ParentTree;
        if (parent.Value is Binary or Unary or CsBinary or CsUnary or IsPattern)
            return CSharpPrecedences.Parenthesize(tc);

        return tc;
    }

    public override J VisitAssignment(Assignment assignment, P p)
    {
        var j = _recursive ? base.VisitAssignment(assignment, p) : assignment;
        if (j is not Assignment a) return j;

        var parent = Cursor.ParentTree;
        if (parent.Value is Binary or Unary or Ternary or CsBinary or CsUnary)
            return CSharpPrecedences.Parenthesize(a);

        return a;
    }

    public override J VisitAssignmentOperation(AssignmentOperation assignment, P p)
    {
        var j = _recursive ? base.VisitAssignmentOperation(assignment, p) : assignment;
        if (j is not AssignmentOperation ao) return j;

        var parent = Cursor.ParentTree;
        if (parent.Value is Binary or Unary or Ternary or CsBinary or CsUnary)
            return CSharpPrecedences.Parenthesize(ao);

        return ao;
    }

    public override J VisitIsPattern(IsPattern isPattern, P p)
    {
        var j = _recursive ? base.VisitIsPattern(isPattern, p) : isPattern;
        if (j is not IsPattern ip) return j;

        var parent = Cursor.ParentTree;
        if (NeedsParenthesesInContext(ip, parent))
            return CSharpPrecedences.Parenthesize(ip);

        return ip;
    }

    public override J VisitRangeExpression(RangeExpression rangeExpression, P p)
    {
        var j = _recursive ? base.VisitRangeExpression(rangeExpression, p) : rangeExpression;
        if (j is not RangeExpression re) return j;

        var parent = Cursor.ParentTree;
        if (NeedsParenthesesInContext(re, parent))
            return CSharpPrecedences.Parenthesize(re);

        return re;
    }

    public override J VisitSwitchExpression(SwitchExpression switchExpression, P p)
    {
        var j = _recursive ? base.VisitSwitchExpression(switchExpression, p) : switchExpression;
        if (j is not SwitchExpression swe) return j;

        var parent = Cursor.ParentTree;
        if (NeedsParenthesesInContext(swe, parent))
            return CSharpPrecedences.Parenthesize(swe);

        return swe;
    }

    public override J VisitWithExpression(WithExpression withExpression, P p)
    {
        var j = _recursive ? base.VisitWithExpression(withExpression, p) : withExpression;
        if (j is not WithExpression we) return j;

        var parent = Cursor.ParentTree;
        if (NeedsParenthesesInContext(we, parent))
            return CSharpPrecedences.Parenthesize(we);

        return we;
    }

    private static bool NeedsParenthesesInContext(Expression expr, Cursor parentCursor)
    {
        var parent = parentCursor.Value;

        // Inside a unary, most compound expressions need parens
        if (parent is Unary or CsUnary)
            return true;

        // Inside a binary — use precedence comparison
        if (parent is Binary parentBinary)
        {
            bool isRight = parentBinary.Right.Id == expr.Id;
            return CSharpPrecedences.NeedsParentheses(expr, parentBinary, isRight);
        }

        if (parent is CsBinary parentCsBinary)
        {
            bool isRight = parentCsBinary.Right.Id == expr.Id;
            return CSharpPrecedences.NeedsParentheses(expr, parentCsBinary, isRight);
        }

        // Inside IsPattern — binary/ternary need parens
        if (parent is IsPattern)
            return expr is Binary or CsBinary or Ternary or Assignment;

        return false;
    }
}
```

- [ ] **Step 2.2: Run tests**

```bash
cd rewrite-csharp/csharp && dotnet test --filter "FullyQualifiedName~CSharpParenthesizeVisitorTests" --verbosity normal
```

Expected: All tests pass.

- [ ] **Step 2.3: Commit**

```bash
git add rewrite-csharp/csharp/OpenRewrite/CSharp/CSharpParenthesizeVisitor.cs rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/CSharpParenthesizeVisitorTests.cs
git commit -m "Add CSharpParenthesizeVisitor with MaybeParenthesize entry point"
```

---

### Task 3: CSharpUnwrapParentheses — Removes Parentheses When Safe

**Files:**
- Create: `rewrite-csharp/csharp/OpenRewrite/CSharp/CSharpUnwrapParentheses.cs`
- Create: `rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/CSharpUnwrapParenthesesTests.cs`

**Depends on:** Task 1

#### Step 1: Write failing tests

- [ ] **Step 1.1: Create test file**

Create `rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/CSharpUnwrapParenthesesTests.cs`:

```csharp
/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * ...license header...
 */
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using static OpenRewrite.Tests.CSharp.TestHelpers;

namespace OpenRewrite.Tests.CSharp;

public class CSharpUnwrapParenthesesTests
{
    [Fact]
    public void IsUnwrappable_SimpleParensAroundIdentifier_True()
    {
        // (x) in a + (x) context → can unwrap
        var parens = MakeParens(MakeId("x"));
        var binary = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), parens);

        var root = new Cursor(null, "root");
        var binaryCursor = new Cursor(root, binary);
        var parensCursor = new Cursor(binaryCursor, parens);

        Assert.True(CSharpUnwrapParentheses<int>.IsUnwrappable(parensCursor));
    }

    [Fact]
    public void IsUnwrappable_BinaryInsideUnary_False()
    {
        // !(a + b) → cannot unwrap
        var inner = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), MakeId("b"));
        var parens = MakeParens(inner);
        var unary = MakeUnary(Unary.OperatorType.Not, parens);

        var root = new Cursor(null, "root");
        var unaryCursor = new Cursor(root, unary);
        var parensCursor = new Cursor(unaryCursor, parens);

        Assert.False(CSharpUnwrapParentheses<int>.IsUnwrappable(parensCursor));
    }

    [Fact]
    public void IsUnwrappable_LowPrecInsideHighPrec_False()
    {
        // (a + b) * c → cannot unwrap
        var inner = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), MakeId("b"));
        var parens = MakeParens(inner);
        var outer = MakeBinary(Binary.OperatorType.Multiplication, parens, MakeId("c"));

        var root = new Cursor(null, "root");
        var outerCursor = new Cursor(root, outer);
        var parensCursor = new Cursor(outerCursor, parens);

        Assert.False(CSharpUnwrapParentheses<int>.IsUnwrappable(parensCursor));
    }

    [Fact]
    public void IsUnwrappable_HighPrecInsideLowPrec_True()
    {
        // (a * b) + c → can unwrap
        var inner = MakeBinary(Binary.OperatorType.Multiplication, MakeId("a"), MakeId("b"));
        var parens = MakeParens(inner);
        var outer = MakeBinary(Binary.OperatorType.Addition, parens, MakeId("c"));

        var root = new Cursor(null, "root");
        var outerCursor = new Cursor(root, outer);
        var parensCursor = new Cursor(outerCursor, parens);

        Assert.True(CSharpUnwrapParentheses<int>.IsUnwrappable(parensCursor));
    }

    [Fact]
    public void IsUnwrappable_InsideTypeCast_False()
    {
        // (int)(x) — structural parens of TypeCast, cannot unwrap
        var parens = MakeParens(MakeId("x"));
        var cast = MakeTypeCast(parens);

        var root = new Cursor(null, "root");
        var castCursor = new Cursor(root, cast);
        var parensCursor = new Cursor(castCursor, parens);

        Assert.False(CSharpUnwrapParentheses<int>.IsUnwrappable(parensCursor));
    }

    [Fact]
    public void Unwrap_TransfersPrefix()
    {
        // Unwrapping should transfer the Parentheses prefix to the inner expression
        var inner = MakeId("x");
        var parens = new Parentheses<Expression>(
            Guid.NewGuid(), new Space(" ", []), Markers.Empty,
            new JRightPadded<Expression>(inner, Space.Empty, Markers.Empty));

        var outer = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), parens);
        var root = new Cursor(null, "root");

        var visitor = new CSharpUnwrapParentheses<int>(parens);
        var result = visitor.Visit(outer, 0, root);

        Assert.IsType<Binary>(result);
        var resultBinary = (Binary)result!;
        // The right side should now be the unwrapped identifier with the parens prefix
        Assert.IsType<Identifier>(resultBinary.Right);
        Assert.Equal(" ", resultBinary.Right.Prefix.Whitespace);
    }
}
```

- [ ] **Step 1.2: Run tests to verify they fail**

```bash
cd rewrite-csharp/csharp && dotnet test --filter "FullyQualifiedName~CSharpUnwrapParenthesesTests" --verbosity normal
```

Expected: Compilation error — `CSharpUnwrapParentheses` does not exist.

#### Step 2: Implement CSharpUnwrapParentheses

- [ ] **Step 2.1: Create CSharpUnwrapParentheses.cs**

Create `rewrite-csharp/csharp/OpenRewrite/CSharp/CSharpUnwrapParentheses.cs`:

```csharp
/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * ...license header...
 */
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Visitor that removes parentheses from a specific expression when safe to do so.
/// Mirrors Java's <c>UnwrapParentheses</c>.
/// </summary>
public class CSharpUnwrapParentheses<P> : CSharpVisitor<P>
{
    private readonly Parentheses<Expression> _scope;

    public CSharpUnwrapParentheses(Parentheses<Expression> scope)
    {
        _scope = scope;
    }

    public override J VisitParentheses(Parentheses<Expression> parens, P p)
    {
        if (_scope.Id == parens.Id && IsUnwrappable(Cursor))
        {
            var tree = J.SetPrefix(parens.Tree.Element, parens.Prefix);
            if (tree.Prefix.Equals(Space.Empty))
            {
                var parent = Cursor.ParentTree.Value;
                if (parent is Return or Throw)
                {
                    tree = J.SetPrefix(tree, Space.SingleSpace);
                }
            }
            return tree;
        }
        return base.VisitParentheses(parens, p);
    }

    public static bool IsUnwrappable(Cursor parensCursor)
    {
        if (parensCursor.Value is not Parentheses<Expression> parens)
            return false;

        var parent = parensCursor.ParentTree.Value;

        // Cannot unwrap structural parens
        if (parent is If or Switch or WhileLoop or ForLoop or ForEachLoop or TypeCast)
            return false;

        if (parent is DoWhileLoop doWhile && parens.Id == doWhile.Condition.Element.Id)
            return false;

        // Cannot unwrap inside a unary when inner is a compound expression
        if (parent is Unary or CsUnary)
        {
            var inner = parens.Tree.Element;
            if (inner is Binary or CsBinary or Assignment or AssignmentOperation
                or Ternary or IsPattern)
                return false;
        }

        // General case: if the inner expression needs parens given the parent, can't unwrap
        if (parent is Expression parentExpr)
        {
            var inner = parens.Tree.Element;
            if (inner is Expression innerExpr)
            {
                bool isRight = false;
                if (parent is Binary parentBinary)
                    isRight = parentBinary.Right.Id == parens.Id;
                else if (parent is CsBinary parentCsBinary)
                    isRight = parentCsBinary.Right.Id == parens.Id;

                if (CSharpPrecedences.NeedsParentheses(innerExpr, parentExpr, isRight))
                    return false;
            }
        }

        return true;
    }
}
```

- [ ] **Step 2.2: Run tests**

```bash
cd rewrite-csharp/csharp && dotnet test --filter "FullyQualifiedName~CSharpUnwrapParenthesesTests" --verbosity normal
```

Expected: All tests pass.

- [ ] **Step 2.3: Commit**

```bash
git add rewrite-csharp/csharp/OpenRewrite/CSharp/CSharpUnwrapParentheses.cs rewrite-csharp/csharp/OpenRewrite/Tests/CSharp/CSharpUnwrapParenthesesTests.cs
git commit -m "Add CSharpUnwrapParentheses with IsUnwrappable"
```

---

### Task 4: Template Integration

**Files:**
- Modify: `rewrite-csharp/csharp/OpenRewrite/CSharp/Template/CSharpTemplate.cs` (lines ~198-224, the `Apply` method)

**Depends on:** Task 2

#### Step 1: Wire MaybeParenthesize into CSharpTemplate.Apply

- [ ] **Step 1.1: Modify CSharpTemplate.Apply**

In `rewrite-csharp/csharp/OpenRewrite/CSharp/Template/CSharpTemplate.cs`, add parenthesization after `ApplySubstitutions` (after line 207, before phase 2). The current code:

```csharp
// Phase 1: placeholder substitution
if (values != null)
{
    tree = TemplateEngine.ApplySubstitutions(tree, values);
}
```

Add after it:

```csharp
// Phase 1.5: auto-parenthesization after substitution
if (tree is Expression expr && cursor.Value is J)
{
    tree = CSharpParenthesizeVisitor.MaybeParenthesize(expr, cursor);
}
```

- [ ] **Step 1.2: Build to verify compilation**

```bash
cd rewrite-csharp/csharp && dotnet build
```

Expected: Build succeeds.

- [ ] **Step 1.3: Commit**

```bash
git add rewrite-csharp/csharp/OpenRewrite/CSharp/Template/CSharpTemplate.cs
git commit -m "Wire CSharpParenthesizeVisitor.MaybeParenthesize into CSharpTemplate.Apply"
```

---

### Task 5: License Headers and Final Verification

**Files:** All new files from Tasks 1-4

**Depends on:** Tasks 1-4

- [ ] **Step 5.1: Apply license headers**

```bash
gw licenseFormatCsharp
```

- [ ] **Step 5.2: Run all C# tests**

```bash
cd rewrite-csharp/csharp && dotnet test --verbosity normal
```

Expected: All tests pass (both new and existing).

- [ ] **Step 5.3: Commit any license header changes**

```bash
git add -u && git commit -m "Apply license headers to new files"
```

(Skip if no changes.)

- [ ] **Step 5.4: Run full module build via Gradle**

```bash
gw :rewrite-csharp:csharpBuild
```

Expected: Build succeeds.
