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
using static OpenRewrite.Tests.CSharp.TestHelpers;

namespace OpenRewrite.Tests.Java;

public class JavaVisitorTests
{
    /// <summary>
    /// When VisitStatement returns a different J type, the visit method
    /// should return it instead of throwing a cast exception.
    /// </summary>
    [Fact]
    public void VisitStatement_ReturnsReplacedType()
    {
        var original = new Block(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
            [], Space.Empty
        );

        var replacement = MakeId("replaced");
        var visitor = new StatementReplacingVisitor(original.Id, replacement);
        var result = visitor.Visit(original, 0, new Cursor(null, "root"));

        Assert.IsType<Identifier>(result);
        Assert.Same(replacement, result);
    }

    /// <summary>
    /// When VisitExpression returns a different J type, the visit method
    /// should return it instead of throwing a cast exception.
    /// </summary>
    [Fact]
    public void VisitExpression_ReturnsReplacedType()
    {
        var original = new Literal(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            42, "42", null, null
        );

        var replacement = MakeId("replaced");
        var visitor = new ExpressionReplacingVisitor(original.Id, replacement);
        var result = visitor.Visit(original, 0, new Cursor(null, "root"));

        Assert.IsType<Identifier>(result);
        Assert.Same(replacement, result);
    }

    private class StatementReplacingVisitor(Guid targetId, J replacement) : JavaVisitor<int>
    {
        public override J VisitStatement(Statement statement, int p)
        {
            return statement is J j && j.Id == targetId ? replacement : (J)statement;
        }
    }

    private class ExpressionReplacingVisitor(Guid targetId, J replacement) : JavaVisitor<int>
    {
        public override J VisitExpression(Expression expression, int p)
        {
            return expression is J j && j.Id == targetId ? replacement : (J)expression;
        }
    }
}
