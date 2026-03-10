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
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Template;

public class TemplateParseTests
{
    [Fact]
    public void ParseLiteralExpression()
    {
        var tmpl = CSharpTemplate.Create("42");
        var tree = tmpl.GetTree();
        Assert.IsType<Literal>(tree);
        Assert.Equal(42, ((Literal)tree).Value);
    }

    [Fact]
    public void ParseStringLiteral()
    {
        var tmpl = CSharpTemplate.Create("\"hello\"");
        var tree = tmpl.GetTree();
        Assert.IsType<Literal>(tree);
        Assert.Equal("hello", ((Literal)tree).Value);
    }

    [Fact]
    public void ParseSimpleIdentifier()
    {
        var tmpl = CSharpTemplate.Create("x");
        var tree = tmpl.GetTree();
        Assert.IsType<Identifier>(tree);
        Assert.Equal("x", ((Identifier)tree).SimpleName);
    }

    [Fact]
    public void ParseMethodInvocation()
    {
        var tmpl = CSharpTemplate.Create("Console.WriteLine(\"hello\")");
        var tree = tmpl.GetTree();
        Assert.IsType<MethodInvocation>(tree);
        var mi = (MethodInvocation)tree;
        Assert.Equal("WriteLine", mi.Name.SimpleName);
    }

    [Fact]
    public void ParseBinaryExpression()
    {
        var tmpl = CSharpTemplate.Create("x + 1");
        var tree = tmpl.GetTree();
        Assert.IsType<Binary>(tree);
    }

    [Fact]
    public void ParseWithCapturePlaceholder()
    {
        var expr = Capture.Of<Expression>("expr");
        var tmpl = CSharpTemplate.Create($"Console.WriteLine({expr})");
        var tree = tmpl.GetTree();
        Assert.IsType<MethodInvocation>(tree);
        var mi = (MethodInvocation)tree;
        // The argument should be a placeholder identifier
        Assert.Single(mi.Arguments.Elements);
        var arg = mi.Arguments.Elements[0].Element;
        Assert.IsType<Identifier>(arg);
        Assert.Equal("__plh_expr__", ((Identifier)arg).SimpleName);
    }

    [Fact]
    public void ParseWithRawCodeSplice()
    {
        var tmpl = CSharpTemplate.Create($"logger.{Raw.Code("Info")}(\"message\")");
        var tree = tmpl.GetTree();
        Assert.IsType<MethodInvocation>(tree);
        var mi = (MethodInvocation)tree;
        Assert.Equal("Info", mi.Name.SimpleName);
    }

    [Fact]
    public void TemplateIsCached()
    {
        var tmpl = CSharpTemplate.Create("42");
        var tree1 = tmpl.GetTree();
        var tree2 = tmpl.GetTree();
        Assert.Same(tree1, tree2);
    }
}
