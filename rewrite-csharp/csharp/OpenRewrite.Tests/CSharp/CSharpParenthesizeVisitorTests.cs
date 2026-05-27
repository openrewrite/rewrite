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

public class CSharpParenthesizeVisitorTests
{
    [Fact]
    public void MaybeParenthesize_BinaryInsideUnary_AddsParens()
    {
        // !placeholder => !(a || b) needs parens
        var placeholder = MakeId("placeholder");
        var parent = MakeUnary(Unary.OperatorType.Not, placeholder);
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var newExpr = MakeBinary(Binary.OperatorType.Or, MakeId("a"), MakeId("b"));
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_IdentifierInsideUnary_NoParens()
    {
        // !placeholder => !x — identifier is a simple expression, fast exit
        var placeholder = MakeId("placeholder");
        var parent = MakeUnary(Unary.OperatorType.Not, placeholder);
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var newExpr = MakeId("x");
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsType<Identifier>(result);
    }

    [Fact]
    public void MaybeParenthesize_LowPrecBinaryInsideHighPrecBinary()
    {
        // placeholder * c => (a + b) * c needs parens
        var placeholder = MakeId("placeholder");
        var parent = MakeBinary(Binary.OperatorType.Multiplication, placeholder, MakeId("c"));
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var newExpr = MakeBinary(Binary.OperatorType.Addition, MakeId("a"), MakeId("b"));
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_HighPrecBinaryInsideLowPrecBinary_NoParens()
    {
        // placeholder + c => a * b + c — no parens needed
        var placeholder = MakeId("placeholder");
        var parent = MakeBinary(Binary.OperatorType.Addition, placeholder, MakeId("c"));
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var newExpr = MakeBinary(Binary.OperatorType.Multiplication, MakeId("a"), MakeId("b"));
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsNotType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_TernaryInsideBinary_AddsParens()
    {
        // placeholder + d => (a ? b : c) + d needs parens
        var placeholder = MakeId("placeholder");
        var parent = MakeBinary(Binary.OperatorType.Addition, placeholder, MakeId("d"));
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var newExpr = MakeTernary(MakeId("a"), MakeId("b"), MakeId("c"));
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_TypeCastInsideUnary_AddsParens()
    {
        // -placeholder => -((int)x) needs parens
        var placeholder = MakeId("placeholder");
        var parent = MakeUnary(Unary.OperatorType.Negative, placeholder);
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var newExpr = MakeTypeCast(MakeId("x"));
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_AssignmentInsideBinary_AddsParens()
    {
        // placeholder + y => (x = 1) + y needs parens
        var placeholder = MakeId("placeholder");
        var parent = MakeBinary(Binary.OperatorType.Addition, placeholder, MakeId("y"));
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var newExpr = MakeAssignment(MakeId("x"), MakeId("1"));
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_UnaryInsideDifferentUnary_NoParens()
    {
        // -placeholder => -(~x) — all prefix unary ops are same precedence, no parens needed
        var placeholder = MakeId("placeholder");
        var parent = MakeUnary(Unary.OperatorType.Negative, placeholder);
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var newExpr = MakeUnary(Unary.OperatorType.Complement, MakeId("x"));
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsType<Unary>(result);
    }

    [Fact]
    public void MaybeParenthesize_TernaryInsideTernary_AddsParens()
    {
        // placeholder ? d : e => (a ? b : c) ? d : e — ternary in condition needs parens
        var placeholder = MakeId("placeholder");
        var parent = MakeTernary(placeholder, MakeId("d"), MakeId("e"));
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var newExpr = MakeTernary(MakeId("a"), MakeId("b"), MakeId("c"));
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_IsPatternWithOrCombinatorInsideLogicalAnd_AddsParens()
    {
        // placeholder && flag => (m is A or B) && flag — or combinator needs parens inside &&
        var placeholder = MakeId("placeholder");
        var parent = MakeBinary(Binary.OperatorType.And, placeholder, MakeId("flag"));
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var orPattern = MakeCsBinary(CsBinary.OperatorType.Or, MakeId("A"), MakeId("B"));
        var newExpr = MakeIsPattern(MakeId("m"), orPattern);
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void MaybeParenthesize_IsPatternWithoutCombinatorInsideLogicalAnd_NoParens()
    {
        // placeholder && flag => m is A && flag — simple is pattern doesn't need parens inside &&
        var placeholder = MakeId("placeholder");
        var parent = MakeBinary(Binary.OperatorType.And, placeholder, MakeId("flag"));
        var cursor = new Cursor(new Cursor(null, "root"), parent);
        cursor = new Cursor(cursor, placeholder);

        var newExpr = MakeIsPattern(MakeId("m"), MakeConstantPattern(MakeId("A")));
        var result = CSharpParenthesizeVisitor.MaybeParenthesize(newExpr, cursor);

        Assert.IsNotType<Parentheses<Expression>>(result);
    }

    [Fact]
    public void ParenthesizeDeep_IsPatternWithOrInsideBinaryAnd_AddsParens()
    {
        // (m is A or B) && flag — recursive walk should parenthesize inner IsPattern
        var orPattern = MakeCsBinary(CsBinary.OperatorType.Or, MakeId("A"), MakeId("B"));
        var isExpr = MakeIsPattern(MakeId("m"), orPattern);
        var tree = MakeBinary(Binary.OperatorType.And, isExpr, MakeId("flag"));

        var result = (Binary)CSharpParenthesizeVisitor.ParenthesizeDeep(tree);

        Assert.IsType<Parentheses<Expression>>(result.Left);
    }

    [Fact]
    public void ParenthesizeDeep_SimpleIsPatternInsideBinaryAnd_NoParens()
    {
        // m is A && flag — no inner parens needed
        var isExpr = MakeIsPattern(MakeId("m"), MakeConstantPattern(MakeId("A")));
        var tree = MakeBinary(Binary.OperatorType.And, isExpr, MakeId("flag"));

        var result = (Binary)CSharpParenthesizeVisitor.ParenthesizeDeep(tree);

        Assert.IsType<IsPattern>(result.Left);
    }
}
