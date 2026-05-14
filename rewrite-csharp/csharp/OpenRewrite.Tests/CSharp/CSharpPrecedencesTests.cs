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
        var expr = new Ternary(
            Guid.NewGuid(), Space.Empty, Markers.Build([NullCoalescing.Instance]),
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
        // (a + b) inside *  =>  needs parens
        var child = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), MakeId("b"));
        var parent = MakeBinary(Binary.OperatorType.Multiplication, child, MakeId("c"));
        Assert.True(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: false));
    }

    [Fact]
    public void NeedsParentheses_HigherPrecChildInLowerPrecParent()
    {
        // a * b inside +  =>  no parens
        var child = MakeBinary(Binary.OperatorType.Multiplication, MakeId("a"), MakeId("b"));
        var parent = MakeBinary(Binary.OperatorType.Addition, child, MakeId("c"));
        Assert.False(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: false));
    }

    [Fact]
    public void NeedsParentheses_SameAssociativeOp_NoParens()
    {
        // a + b inside +, on the left  =>  no parens (associative)
        var child = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), MakeId("b"));
        var parent = MakeBinary(Binary.OperatorType.Addition, child, MakeId("c"));
        Assert.False(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: false));
    }

    [Fact]
    public void NeedsParentheses_SubtractionOnRight_NeedsParens()
    {
        // b - c on the right of +  =>  needs parens (same group, right operand)
        var child = MakeBinary(Binary.OperatorType.Subtraction, MakeId("b"), MakeId("c"));
        var parent = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), child);
        Assert.True(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: true));
    }

    [Fact]
    public void NeedsParentheses_SubtractionOnLeft_NoParens()
    {
        // a - b on the left of +  =>  no parens (same group, left operand)
        var child = MakeBinary(Binary.OperatorType.Subtraction, MakeId("a"), MakeId("b"));
        var parent = MakeBinary(Binary.OperatorType.Addition, child, MakeId("c"));
        Assert.False(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: false));
    }

    [Fact]
    public void NeedsParentheses_NullCoalescingRightAssociative_NoParens()
    {
        // a ?? b on right of ??  =>  no parens (right-associative)
        var child = new Ternary(
            Guid.NewGuid(), Space.Empty, Markers.Build([NullCoalescing.Instance]),
            MakeId("a"),
            new JLeftPadded<Expression>(Space.Empty, new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty)),
            new JLeftPadded<Expression>(Space.Empty, MakeId("b")),
            null);
        var parent = new Ternary(
            Guid.NewGuid(), Space.Empty, Markers.Build([NullCoalescing.Instance]),
            MakeId("x"),
            new JLeftPadded<Expression>(Space.Empty, new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty)),
            new JLeftPadded<Expression>(Space.Empty, child),
            null);
        Assert.False(CSharpPrecedences.NeedsParentheses(child, parent, isRightOperand: true));
    }

    [Fact]
    public void NeedsParentheses_DifferentOpsAtSameLevel_NeedsParens()
    {
        // == inside !=  =>  needs parens (same precedence, different op)
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
        var prefix = new Space("\n", []);
        var id = new Identifier(Guid.NewGuid(), prefix, Markers.Empty, [], "x", null, null);
        var result = CSharpPrecedences.Parenthesize(id);

        Assert.IsType<Parentheses<Expression>>(result);
        Assert.Equal("\n", result.Prefix.Whitespace);
        // Inner expression should have empty prefix
        var inner = result.Tree.Element;
        Assert.Equal("", inner.Prefix.Whitespace);
    }

    [Fact]
    public void Parenthesize_AlreadyParenthesized_ReturnsSame()
    {
        var parens = MakeParens(MakeId("a"));
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
