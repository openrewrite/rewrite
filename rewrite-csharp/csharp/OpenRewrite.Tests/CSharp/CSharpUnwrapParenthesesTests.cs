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

public class CSharpUnwrapParenthesesTests
{
    // =============================================================
    // IsUnwrappable
    // =============================================================

    [Fact]
    public void IsUnwrappable_SimpleParensAroundIdentifier_True()
    {
        // a + (x) => parens around simple identifier can be unwrapped
        var parens = MakeParens(MakeId("x"));
        var parent = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), parens);
        var root = new Cursor(null, "root");
        var parentCursor = new Cursor(root, parent);
        var parensCursor = new Cursor(parentCursor, parens);
        Assert.True(CSharpUnwrapParentheses<int>.IsUnwrappable(parensCursor));
    }

    [Fact]
    public void IsUnwrappable_BinaryInsideUnary_False()
    {
        // !(a + b) => cannot unwrap parens around binary inside unary
        var parens = MakeParens(MakeBinary(Binary.OperatorType.Addition, MakeId("a"), MakeId("b")));
        var parent = MakeUnary(Unary.OperatorType.Not, parens);
        var root = new Cursor(null, "root");
        var parentCursor = new Cursor(root, parent);
        var parensCursor = new Cursor(parentCursor, parens);
        Assert.False(CSharpUnwrapParentheses<int>.IsUnwrappable(parensCursor));
    }

    [Fact]
    public void IsUnwrappable_LowPrecInsideHighPrec_False()
    {
        // (a + b) * c => cannot unwrap, lower precedence inside higher
        var parens = MakeParens(MakeBinary(Binary.OperatorType.Addition, MakeId("a"), MakeId("b")));
        var parent = MakeBinary(Binary.OperatorType.Multiplication, parens, MakeId("c"));
        var root = new Cursor(null, "root");
        var parentCursor = new Cursor(root, parent);
        var parensCursor = new Cursor(parentCursor, parens);
        Assert.False(CSharpUnwrapParentheses<int>.IsUnwrappable(parensCursor));
    }

    [Fact]
    public void IsUnwrappable_HighPrecInsideLowPrec_True()
    {
        // (a * b) + c => can unwrap, higher precedence inside lower
        var parens = MakeParens(MakeBinary(Binary.OperatorType.Multiplication, MakeId("a"), MakeId("b")));
        var parent = MakeBinary(Binary.OperatorType.Addition, parens, MakeId("c"));
        var root = new Cursor(null, "root");
        var parentCursor = new Cursor(root, parent);
        var parensCursor = new Cursor(parentCursor, parens);
        Assert.True(CSharpUnwrapParentheses<int>.IsUnwrappable(parensCursor));
    }

    [Fact]
    public void IsUnwrappable_InsideTypeCast_False()
    {
        // (int)(x) => cannot unwrap parens that are the expression of a type cast
        var parens = MakeParens(MakeId("x"));
        var parent = MakeTypeCast(parens);
        var root = new Cursor(null, "root");
        var parentCursor = new Cursor(root, parent);
        var parensCursor = new Cursor(parentCursor, parens);
        Assert.False(CSharpUnwrapParentheses<int>.IsUnwrappable(parensCursor));
    }

    // =============================================================
    // Unwrap (visitor)
    // =============================================================

    [Fact]
    public void Unwrap_TransfersPrefix()
    {
        // Unwrapping should transfer the Parentheses prefix to the inner expression
        var inner = MakeId("x");
        var parens = new Parentheses<Expression>(
            Guid.NewGuid(), new Space(" ", []), Markers.Empty,
            new JRightPadded<Expression>(inner, Space.Empty, Markers.Empty));
        var outer = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), parens);

        var visitor = new CSharpUnwrapParentheses<int>(parens);
        var root = new Cursor(null, "root");
        var result = (Binary)visitor.Visit(outer, 0, root)!;

        // The right side should now be the identifier with the parens prefix transferred
        Assert.IsType<Identifier>(result.Right);
        Assert.Equal(" ", result.Right.Prefix.Whitespace);
    }
}
