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
using OpenRewrite.CSharp;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.CSharp;

public class StructuralEqualityComparatorTests
{
    private readonly CSharpParser _parser = new();

    /// <summary>
    /// Parse a top-level expression statement and return the expression.
    /// Input should be like "a + b;" — the parser handles top-level statements.
    /// </summary>
    private Expression ParseExpression(string expressionStatement)
    {
        var cu = _parser.Parse(expressionStatement);
        var member = cu.Members[0].Element;
        if (member is ExpressionStatement es)
            return es.Expression;
        throw new InvalidOperationException(
            $"Expected ExpressionStatement, got {member.GetType().Name}");
    }

    [Fact]
    public void IdenticalLiterals()
    {
        var a = ParseExpression("42;");
        var b = ParseExpression("42;");
        Assert.True(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void DifferentLiterals()
    {
        var a = ParseExpression("42;");
        var b = ParseExpression("99;");
        Assert.False(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void IdenticalBinaryExpressions()
    {
        var a = ParseExpression("x + y;");
        var b = ParseExpression("x + y;");
        Assert.True(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void DifferentOperators()
    {
        var a = ParseExpression("x + y;");
        var b = ParseExpression("x - y;");
        Assert.False(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void DifferentIdentifierNames()
    {
        var a = ParseExpression("x + y;");
        var b = ParseExpression("x + z;");
        Assert.False(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void WhitespaceDoesNotAffectEquality()
    {
        var a = ParseExpression("x+y;");
        var b = ParseExpression("x  +  y;");
        Assert.True(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void NestedExpressions()
    {
        var a = ParseExpression("(x + y) * z;");
        var b = ParseExpression("(x + y) * z;");
        Assert.True(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void DifferentNestedExpressions()
    {
        var a = ParseExpression("(x + y) * z;");
        var b = ParseExpression("(x - y) * z;");
        Assert.False(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void DifferentNodeTypes()
    {
        var a = ParseExpression("42;");
        var b = ParseExpression("x;");
        Assert.False(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void MethodInvocations()
    {
        var a = ParseExpression("Console.WriteLine(x);");
        var b = ParseExpression("Console.WriteLine(x);");
        Assert.True(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void DifferentMethodNames()
    {
        var a = ParseExpression("Console.WriteLine(x);");
        var b = ParseExpression("Console.Write(x);");
        Assert.False(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void StringLiterals()
    {
        var a = ParseExpression("\"hello\";");
        var b = ParseExpression("\"hello\";");
        Assert.True(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }

    [Fact]
    public void DifferentStringLiterals()
    {
        var a = ParseExpression("\"hello\";");
        var b = ParseExpression("\"world\";");
        Assert.False(StructuralEqualityComparator.AreStructurallyEqual(a, b));
    }
}
