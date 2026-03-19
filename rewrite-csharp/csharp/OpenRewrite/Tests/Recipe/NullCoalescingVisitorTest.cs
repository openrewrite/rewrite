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
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Recipe;

/// <summary>
/// Tests that returning null from a child visit method propagates correctly
/// instead of being silently swallowed by the ?? (null-coalescing) operator.
/// </summary>
public class NullCoalescingVisitorTest : RewriteTest
{
    /// <summary>
    /// Verifies that when VisitBlock returns null for a class body,
    /// the parent VisitClassDeclaration reflects the null child
    /// rather than silently restoring the original via ??.
    /// </summary>
    [Fact]
    public void NullReturnFromChildVisitIsNotSwallowedByNullCoalescing()
    {
        var parser = new CSharpParser();
        var source = parser.Parse("class C { }");

        var visitor = new NullifyBlockVisitor();
        var result = visitor.Visit(source, new Core.ExecutionContext());

        // When VisitBlock returns null, the parent should NOT silently restore
        // the original. The tree should be modified (different reference).
        Assert.False(ReferenceEquals(source, result),
            "When VisitBlock returns null for the class body, " +
            "VisitClassDeclaration should not silently restore the original via ??. " +
            "The tree should reflect the null child.");
    }

    /// <summary>
    /// Verifies that returning null from VisitMethodInvocation within an
    /// ExpressionStatement properly propagates, allowing the containing
    /// statement to be affected.
    /// </summary>
    [Fact]
    public void NullReturnFromExpressionStatementChildPropagates()
    {
        var parser = new CSharpParser();
        var source = parser.Parse(
            """
            class C {
                void M() {
                    System.Console.WriteLine();
                }
            }
            """);

        var visitor = new NullifyMethodInvocationVisitor();
        var result = visitor.Visit(source, new Core.ExecutionContext());

        // The visitor returned null for the MethodInvocation inside the
        // ExpressionStatement. The tree should change.
        Assert.False(ReferenceEquals(source, result),
            "When VisitMethodInvocation returns null, the containing " +
            "ExpressionStatement should not silently restore the original.");
    }

    private class NullifyBlockVisitor : CSharpVisitor<Core.ExecutionContext>
    {
        public override J VisitBlock(Block block, Core.ExecutionContext ctx)
        {
            return null!;
        }
    }

    private class NullifyMethodInvocationVisitor : CSharpVisitor<Core.ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, Core.ExecutionContext ctx)
        {
            return null!;
        }
    }
}
