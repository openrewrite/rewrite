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
using OpenRewrite.Test;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Template;

public class TypedCaptureTests : RewriteTest
{
    [Fact]
    public void TypedCaptureStoresType()
    {
        var s = Capture.Of<Expression>("s", type: "string");
        Assert.Equal("string", s.Type);
    }

    [Fact]
    public void UntypedCaptureHasNullType()
    {
        var s = Capture.Of<Expression>("s");
        Assert.Null(s.Type);
    }

    [Fact]
    public void TypedCaptureProducesCorrectPlaceholder()
    {
        var s = Capture.Of<Expression>("s", type: "string");
        Assert.Equal("__plh_s__", s.ToString());
    }

    [Fact]
    public void TypedCaptureEnablesMemberAccess()
    {
        // With a typed capture, `{s}.Length` should parse correctly because
        // the parser knows __plh_s__ is a string and .Length is a valid member
        var s = Capture.Of<Expression>("s", type: "string");
        var pat = CSharpPattern.Create($"{s}.Length");
        var tree = pat.GetTree();
        Assert.IsType<FieldAccess>(tree);
    }

    [Fact]
    public void TypedCaptureEnablesMethodCall()
    {
        // With a typed capture, `{s}.ToUpper()` should parse correctly
        var s = Capture.Of<Expression>("s", type: "string");
        var pat = CSharpPattern.Create($"{s}.ToUpper()");
        var tree = pat.GetTree();
        Assert.IsType<MethodInvocation>(tree);
    }

    [Fact]
    public void TypedCapturePatternMatchesMemberAccess()
    {
        var s = Capture.Of<Expression>("s", type: "string");
        RewriteRun(
            spec => spec.SetRecipe(FindExpression($"{s}.Length")),
            CSharp(
                "class C { void M() { string x = \"hello\"; var n = x.Length; } }",
                "class C { void M() { string x = \"hello\"; var n = /*~~>*/x.Length; } }"
            )
        );
    }

    [Fact]
    public void TypedCapturePatternMatchesMethodCall()
    {
        var s = Capture.Of<Expression>("s", type: "string");
        RewriteRun(
            spec => spec.SetRecipe(FindExpression($"{s}.ToUpper()")),
            CSharp(
                "class C { void M() { string x = \"hello\"; var y = x.ToUpper(); } }",
                "class C { void M() { string x = \"hello\"; var y = /*~~>*/x.ToUpper(); } }"
            )
        );
    }

    [Fact]
    public void TypedCaptureWorksAlongsideUntypedCapture()
    {
        var s = Capture.Of<Expression>("s", type: "string");
        var n = Capture.Of<Expression>("n");
        RewriteRun(
            spec => spec.SetRecipe(FindExpression($"{s}.Substring({n})")),
            CSharp(
                "class C { void M() { string x = \"hello\"; var y = x.Substring(1); } }",
                "class C { void M() { string x = \"hello\"; var y = /*~~>*/x.Substring(1); } }"
            )
        );
    }

    [Fact]
    public void TypedCaptureWithIntType()
    {
        var n = Capture.Of<Expression>("n", type: "int");
        var pat = CSharpPattern.Create($"{n}.ToString()");
        var tree = pat.GetTree();
        Assert.IsType<MethodInvocation>(tree);
    }

    [Fact]
    public void MultipleTypedCaptures()
    {
        var s = Capture.Of<Expression>("s", type: "string");
        var n = Capture.Of<Expression>("n", type: "int");
        var pat = CSharpPattern.Create($"{s}.Substring({n})");
        var tree = pat.GetTree();
        Assert.IsType<MethodInvocation>(tree);
    }

    // ===============================================================
    // Recipe factory
    // ===============================================================

    private static Core.Recipe FindExpression(TemplateStringHandler handler)
        => new TypedPatternSearchRecipe(CSharpPattern.Create(handler));

    private static Core.Recipe FindExpression(string code)
        => new TypedPatternSearchRecipe(CSharpPattern.Create(code));
}

file class TypedPatternSearchRecipe(CSharpPattern pat) : Core.Recipe
{
    public override string DisplayName => "Find expression";
    public override string Description => "Searches for expressions matching the pattern.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new SearchVisitor(pat);

    private class SearchVisitor(CSharpPattern pat) : CSharpVisitor<ExecutionContext>
    {
        public override J? PreVisit(J tree, ExecutionContext ctx)
        {
            if (tree is Expression e)
            {
                return pat.Find(e, Cursor);
            }
            return tree;
        }
    }
}
