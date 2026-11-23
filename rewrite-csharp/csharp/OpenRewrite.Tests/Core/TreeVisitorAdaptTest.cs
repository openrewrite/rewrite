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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// Verifies that <see cref="TreeVisitor{T,P}.Adapt"/> wraps a generic visitor as the right
/// language-specific visitor before dispatching Accept. Without adaptation a bare
/// <c>TreeVisitor&lt;J, P&gt;</c> would silently no-op (default Accept is identity) and a
/// plain <c>JavaVisitor&lt;P&gt;</c> would throw on the first Cs node its switch doesn't
/// recognize. Mirrors the regression fix made on the Python side in commit 96c08d1eaf.
/// </summary>
public class TreeVisitorAdaptTest
{
    private static readonly string CSharpSource = """
        using System;

        namespace MyApp
        {
            public class Foo
            {
                public int Bar(int x) => x + 1;
            }
        }
        """;

    /// <summary>
    /// A bare TreeVisitor that only overrides PreVisit must descend into every node of a Cs
    /// source file. Before the fix, the default Accept (identity) caused traversal to stop
    /// at the root.
    /// </summary>
    [Fact]
    public void BareTreeVisitorTraversesCSharpSource()
    {
        var cu = new CSharpParser().Parse(CSharpSource);

        var collector = new CountingPreVisitor();
        collector.Visit(cu, new ExecutionContext());

        // Must descend into the namespace, class, method, parameter, body, etc.
        // Not asserting an exact count (parser-version-dependent), but it must be > 1
        // (more than just the CompilationUnit itself).
        Assert.True(collector.Count > 5,
            $"Expected bare TreeVisitor to descend into Cs nodes, only saw {collector.Count} preVisit calls");

        // Must include both Cs-specific types and shared J types.
        Assert.Contains(collector.SeenTypes, t => typeof(Cs).IsAssignableFrom(t));
        Assert.Contains(collector.SeenTypes, t => typeof(J).IsAssignableFrom(t) && !typeof(Cs).IsAssignableFrom(t));
    }

    /// <summary>
    /// A plain JavaVisitor visiting a Cs source file must not throw on Cs-specific nodes.
    /// Before the fix, JavaVisitor's switch had no arm for Cs.UsingDirective /
    /// Cs.CompilationUnit / etc. and threw "Unknown J tree type".
    /// </summary>
    [Fact]
    public void JavaVisitorTraversesCSharpSourceWithoutThrowing()
    {
        var cu = new CSharpParser().Parse(CSharpSource);

        var visitor = new MethodCountingJavaVisitor();
        var ex = Record.Exception(() => visitor.Visit(cu, new ExecutionContext()));

        Assert.Null(ex);
        Assert.Equal(1, visitor.MethodCount);
    }
}

file class CountingPreVisitor : TreeVisitor<J, ExecutionContext>
{
    public int Count { get; private set; }
    public HashSet<System.Type> SeenTypes { get; } = new();

    public override J? PreVisit(J tree, ExecutionContext p)
    {
        Count++;
        SeenTypes.Add(tree.GetType());
        return tree;
    }
}

file class MethodCountingJavaVisitor : JavaVisitor<ExecutionContext>
{
    public int MethodCount { get; private set; }

    public override J VisitMethodDeclaration(MethodDeclaration method, ExecutionContext p)
    {
        MethodCount++;
        return base.VisitMethodDeclaration(method, p);
    }
}
