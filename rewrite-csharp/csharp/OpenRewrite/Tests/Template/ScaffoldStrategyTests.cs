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
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Template;

public class ScaffoldStrategyTests
{
    // === Expression ===

    [Fact]
    public void ExpressionLiteral()
    {
        var tmpl = CSharpTemplate.Expression("42");
        var tree = tmpl.GetTree();
        Assert.IsType<Literal>(tree);
        Assert.Equal(42, ((Literal)tree).Value);
    }

    [Fact]
    public void ExpressionBinary()
    {
        var tmpl = CSharpTemplate.Expression("1 + 2");
        var tree = tmpl.GetTree();
        Assert.IsType<Binary>(tree);
    }

    [Fact]
    public void ExpressionMethodInvocation()
    {
        var tmpl = CSharpTemplate.Expression("Console.WriteLine(\"hello\")");
        var tree = tmpl.GetTree();
        Assert.IsType<MethodInvocation>(tree);
    }

    [Fact]
    public void ExpressionWithCapture()
    {
        var expr = Capture.Of<Expression>("expr");
        var tmpl = CSharpTemplate.Expression($"1 + {expr}");
        var tree = tmpl.GetTree();
        Assert.IsType<Binary>(tree);
    }

    // === Statement ===

    [Fact]
    public void StatementVariableDeclaration()
    {
        var tmpl = CSharpTemplate.Statement("var x = 42");
        var tree = tmpl.GetTree();
        Assert.IsType<VariableDeclarations>(tree);
    }

    [Fact]
    public void StatementThrow()
    {
        var tmpl = CSharpTemplate.Statement("throw new Exception()");
        var tree = tmpl.GetTree();
        Assert.IsType<Throw>(tree);
    }

    [Fact]
    public void StatementIfBlock()
    {
        var tmpl = CSharpTemplate.Statement("if (true) { }");
        var tree = tmpl.GetTree();
        Assert.IsType<If>(tree);
    }

    // === Attribute ===

    [Fact]
    public void AttributeSimple()
    {
        var tmpl = CSharpTemplate.Attribute("Obsolete");
        var tree = tmpl.GetTree();
        Assert.IsType<Annotation>(tree);
        var ann = (Annotation)tree;
        Assert.Equal("Obsolete", ((Identifier)ann.AnnotationType).SimpleName);
    }

    [Fact]
    public void AttributeWithArguments()
    {
        var tmpl = CSharpTemplate.Attribute("Obsolete(\"Use new API\")");
        var tree = tmpl.GetTree();
        Assert.IsType<Annotation>(tree);
        var ann = (Annotation)tree;
        Assert.NotNull(ann.Arguments);
        Assert.Single(ann.Arguments!.Elements);
    }

    [Fact]
    public void AttributeWithUsings()
    {
        var tmpl = CSharpTemplate.Attribute("Serializable",
            usings: ["System"]);
        var tree = tmpl.GetTree();
        Assert.IsType<Annotation>(tree);
    }

    // === ClassMember ===

    [Fact]
    public void ClassMemberMethod()
    {
        var tmpl = CSharpTemplate.ClassMember("public void Foo() { }");
        var tree = tmpl.GetTree();
        Assert.IsType<MethodDeclaration>(tree);
        var md = (MethodDeclaration)tree;
        Assert.Equal("Foo", md.Name.SimpleName);
    }

    [Fact]
    public void ClassMemberField()
    {
        var tmpl = CSharpTemplate.ClassMember("private int _count");
        var tree = tmpl.GetTree();
        Assert.IsType<VariableDeclarations>(tree);
    }

    [Fact]
    public void ClassMemberProperty()
    {
        var tmpl = CSharpTemplate.ClassMember("public string Name { get; set; }");
        var tree = tmpl.GetTree();
        Assert.IsType<PropertyDeclaration>(tree);
    }

    [Fact]
    public void ClassMemberAnnotatedMethod()
    {
        var tmpl = CSharpTemplate.ClassMember("[Obsolete] public void Foo() { }");
        var tree = tmpl.GetTree();
        Assert.IsType<AnnotatedStatement>(tree);
    }

    [Fact]
    public void ClassMemberWithCapture()
    {
        var name = Capture.Name("name");
        var tmpl = CSharpTemplate.ClassMember($"public void {name}() {{ }}");
        var tree = tmpl.GetTree();
        Assert.IsType<MethodDeclaration>(tree);
        var md = (MethodDeclaration)tree;
        Assert.Equal("__plh_name__", md.Name.SimpleName);
    }

    // === Pattern variants ===

    [Fact]
    public void PatternExpression()
    {
        var pat = CSharpPattern.Expression("42");
        var tree = pat.GetTree();
        Assert.IsType<Literal>(tree);
    }

    [Fact]
    public void PatternStatement()
    {
        var pat = CSharpPattern.Statement("var x = 42");
        var tree = pat.GetTree();
        Assert.IsType<VariableDeclarations>(tree);
    }

    [Fact]
    public void PatternAttribute()
    {
        var pat = CSharpPattern.Attribute("Obsolete");
        var tree = pat.GetTree();
        Assert.IsType<Annotation>(tree);
    }

    [Fact]
    public void PatternClassMember()
    {
        var pat = CSharpPattern.ClassMember("public void Foo() { }");
        var tree = pat.GetTree();
        Assert.IsType<MethodDeclaration>(tree);
    }
}
