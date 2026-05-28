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

public class CSharpVisitorTests
{
    /// <summary>
    /// When VisitStatement returns a different type, the caller should
    /// return that result instead of attempting to cast it.
    /// </summary>
    [Fact]
    public void VisitStatement_ReturnsReplacedType()
    {
        var original = new GotoStatement(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, MakeId("foo")
        );

        var replacement = MakeId("replaced");
        var visitor = new StatementReplacingVisitor(original.Id, replacement);
        var result = visitor.Visit(original, 0, new Cursor(null, "root"));

        Assert.IsType<Identifier>(result);
        Assert.Same(replacement, result);
    }

    /// <summary>
    /// When VisitExpression returns a different type, the caller should
    /// return that result instead of attempting to cast it.
    /// </summary>
    [Fact]
    public void VisitExpression_ReturnsReplacedType()
    {
        var original = new SizeOf(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            expression: MakeId("int"),
            type: null
        );

        var replacement = MakeId("replaced");
        var visitor = new ExpressionReplacingVisitor(original.Id, replacement);
        var result = visitor.Visit(original, 0, new Cursor(null, "root"));

        Assert.IsType<Identifier>(result);
        Assert.Same(replacement, result);
    }

    /// <summary>
    /// Replaces a specific Statement with an arbitrary J node via VisitStatement.
    /// </summary>
    private class StatementReplacingVisitor(Guid targetId, J replacement) : CSharpVisitor<int>
    {
        public override J VisitStatement(Statement statement, int p)
        {
            return statement is J j && j.Id == targetId ? replacement : (J)statement;
        }
    }

    /// <summary>
    /// Replaces a specific Expression with an arbitrary J node via VisitExpression.
    /// </summary>
    private class ExpressionReplacingVisitor(Guid targetId, J replacement) : CSharpVisitor<int>
    {
        public override J VisitExpression(Expression expression, int p)
        {
            return expression is J j && j.Id == targetId ? replacement : (J)expression;
        }
    }
}
