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
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;
using CompilationUnit = OpenRewrite.CSharp.CompilationUnit;

namespace OpenRewrite.Tests.Template;

public class TemplateApplyTests
{
    private static Cursor RootCursor() => new();

    [Fact]
    public void PatternMatchThenTemplateApply()
    {
        // Pattern: Console.Write({expr}) → Template: Console.WriteLine({expr})
        var expr = Capture.Of<Expression>("expr");
        var pat = CSharpPattern.Create($"Console.Write({expr})");
        var tmpl = CSharpTemplate.Create($"Console.WriteLine({expr})");

        var parser = new CSharpParser();
        var cu = parser.Parse("class C { void M() { Console.Write(42); } }");
        var mi = FindFirst<MethodInvocation>(cu);
        Assert.NotNull(mi);

        var match = pat.Match(mi, RootCursor());
        Assert.NotNull(match);

        var result = tmpl.Apply(RootCursor(), values: match);
        Assert.NotNull(result);
        Assert.IsType<MethodInvocation>(result);

        var resultMi = (MethodInvocation)result!;
        Assert.Equal("WriteLine", resultMi.Name.SimpleName);

        // The argument should be the captured literal 42
        Assert.Single(resultMi.Arguments.Elements);
        var arg = resultMi.Arguments.Elements[0].Element;
        Assert.IsType<Literal>(arg);
        Assert.Equal(42, ((Literal)arg).Value);
    }

    [Fact]
    public void TemplateWithoutCapturesProducesFixedAst()
    {
        var tmpl = CSharpTemplate.Create("Console.WriteLine(\"hello\")");
        var result = tmpl.Apply(RootCursor());
        Assert.NotNull(result);
        Assert.IsType<MethodInvocation>(result);
        Assert.Equal("WriteLine", ((MethodInvocation)result!).Name.SimpleName);
    }

    [Fact]
    public void TemplateWithRawCodeSplice()
    {
        var expr = Capture.Of<Expression>("expr");
        var methodName = "Info";
        var tmpl = CSharpTemplate.Create($"logger.{Raw.Code(methodName)}({expr})");

        // Create a simple match result with a literal
        var parser = new CSharpParser();
        var cu = parser.Parse("class C { void M() { logger.Debug(\"msg\"); } }");
        var mi = FindFirst<MethodInvocation>(cu);
        Assert.NotNull(mi);

        // Match a pattern to get the argument
        var debugExpr = Capture.Of<Expression>("expr");
        var pat = CSharpPattern.Create($"logger.Debug({debugExpr})");
        var match = pat.Match(mi, RootCursor());
        Assert.NotNull(match);

        var result = tmpl.Apply(RootCursor(), values: match);
        Assert.NotNull(result);
        Assert.IsType<MethodInvocation>(result);
        Assert.Equal("Info", ((MethodInvocation)result!).Name.SimpleName);
    }

    private static T? FindFirst<T>(CompilationUnit cu) where T : class, J
    {
        var finder = new FindFirstVisitor<T>();
        finder.Visit(cu, 0);
        return finder.Found;
    }

    private class FindFirstVisitor<T> : CSharpVisitor<int> where T : class, J
    {
        public T? Found { get; private set; }

        public override J? PreVisit(J tree, int p)
        {
            if (Found != null)
            {
                StopAfterPreVisit();
                return tree;
            }
            if (tree is T t)
            {
                Found = t;
                StopAfterPreVisit();
                return tree;
            }
            return tree;
        }
    }
}
