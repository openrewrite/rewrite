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

public class CsHelpersTests
{
    // =============================================================
    // GetSimpleName
    // =============================================================

    [Fact]
    public void GetSimpleName_Identifier()
    {
        var id = MakeId("Obsolete");
        Assert.Equal("Obsolete", Cs.GetSimpleName(id));
    }

    [Fact]
    public void GetSimpleName_FieldAccess()
    {
        var fa = MakeFieldAccess("System", "Obsolete");
        Assert.Equal("Obsolete", Cs.GetSimpleName(fa));
    }

    [Fact]
    public void GetSimpleName_UnsupportedNameTree()
    {
        var parameterized = new ParameterizedType(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            MakeId("List"), null, null);
        Assert.Null(Cs.GetSimpleName(parameterized));
    }

    // =============================================================
    // HasNoArguments
    // =============================================================

    [Fact]
    public void HasNoArguments_EmptyList()
    {
        var mi = new MethodInvocation(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, MakeId("Foo"), null,
            new JContainer<Expression>(Space.Empty, [], Markers.Empty),
            null);
        Assert.True(Cs.HasNoArguments(mi));
    }

    [Fact]
    public void HasNoArguments_EmptySentinel()
    {
        var empty = new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty);
        var mi = new MethodInvocation(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, MakeId("Foo"), null,
            new JContainer<Expression>(Space.Empty, [Pad<Expression>(empty)], Markers.Empty),
            null);
        Assert.True(Cs.HasNoArguments(mi));
    }

    [Fact]
    public void HasNoArguments_WithArgument()
    {
        var arg = MakeId("x");
        var mi = new MethodInvocation(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, MakeId("Foo"), null,
            new JContainer<Expression>(Space.Empty, [Pad<Expression>(arg)], Markers.Empty),
            null);
        Assert.False(Cs.HasNoArguments(mi));
    }

    [Fact]
    public void HasNoArguments_MultipleArguments()
    {
        var mi = new MethodInvocation(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, MakeId("Foo"), null,
            new JContainer<Expression>(Space.Empty,
                [Pad<Expression>(MakeId("x")), Pad<Expression>(MakeId("y"))], Markers.Empty),
            null);
        Assert.False(Cs.HasNoArguments(mi));
    }

    // =============================================================
    // GetValueText
    // =============================================================

    [Theory]
    [InlineData("Foo", "Foo")]
    [InlineData("@class", "class")]
    [InlineData("@Foo", "Foo")]
    [InlineData("\\u0061b", "ab")]
    [InlineData("@\\u0061", "a")]
    [InlineData("\\U00000061", "a")]
    [InlineData("ä", "ä")]
    [InlineData("~Foo", "~Foo")]
    [InlineData("", "")]
    public void GetValueText_SimpleName(string simpleName, string expected)
    {
        Assert.Equal(expected, Cs.GetValueText(simpleName));
    }

    [Fact]
    public void GetValueText_ParsedIdentifiersCompareEqual()
    {
        var cu = new CSharpParser().Parse("class @class { void \\u0061() { } void a(int x) { } }");
        var cd = (ClassDeclaration)cu.Members[0].Element;
        var first = (MethodDeclaration)cd.Body.Statements[0].Element;
        var second = (MethodDeclaration)cd.Body.Statements[1].Element;

        Assert.Equal("@class", cd.Name.SimpleName);
        Assert.Equal("class", Cs.GetValueText(cd.Name));
        Assert.NotEqual(first.Name.SimpleName, second.Name.SimpleName);
        Assert.Equal(Cs.GetValueText(first.Name), Cs.GetValueText(second.Name));
    }

    // =============================================================
    // HasComments
    // =============================================================

    private static ClassDeclaration ParseClass(string code) =>
        (ClassDeclaration)new CSharpParser().Parse(code).Members[0].Element;

    private static MethodDeclaration FirstMethod(string code) =>
        (MethodDeclaration)ParseClass(code).Body.Statements[0].Element;

    [Fact]
    public void HasComments_NoneIgnoresSlashesInStringsAndUrls()
    {
        var method = FirstMethod("""
            class A {
                void M() {
                    var url = "https://example.com/*x*/";
                }
            }
            """);
        Assert.False(Cs.HasComments(method));
    }

    [Theory]
    [InlineData("var x = 1; // trailing")]
    [InlineData("/* lead */ var x = 1;")]
    [InlineData("N(1 /* after */, 2);")]
    [InlineData("N(1, /* before */ 2);")]
    [InlineData("int x /* before = */ = 1;")]
    [InlineData("N /* before ( */ (1, 2);")]
    [InlineData("var y = new[] { 1, 2 /* trailing element */ };")]
    public void HasComments_FindsCommentInEveryPadding(string statement)
    {
        var method = FirstMethod($$"""
            class A {
                void M() {
                    {{statement}}
                }
                void N(int a, int b) { }
            }
            """);
        Assert.True(Cs.HasComments(method));
    }

    [Fact]
    public void HasComments_FindsDocComment()
    {
        var method = FirstMethod("""
            class A {
                /// <summary>Does nothing.</summary>
                void M() { }
            }
            """);
        Assert.Contains(method.Prefix.Comments, c => c is CsDocComment.DocComment);
        Assert.True(Cs.HasComments(method));
    }

    [Fact]
    public void HasComments_ExcludingPrefix()
    {
        var leading = FirstMethod("""
            class A {
                // leading
                void M() { }
            }
            """);
        var interior = FirstMethod("""
            class A {
                // leading
                void M() { /* interior */ }
            }
            """);
        Assert.True(Cs.HasComments(leading));
        Assert.False(Cs.HasComments(leading, includePrefix: false));
        Assert.True(Cs.HasComments(interior, includePrefix: false));
    }

    [Fact]
    public void HasComments_PreprocessorDirectivesOnlyWhenRequested()
    {
        var method = FirstMethod("""
            class A {
                void M() {
            #pragma warning disable CS0168
                    int x;
            #pragma warning restore CS0168
                }
            }
            """);
        Assert.False(Cs.HasComments(method));
        Assert.True(Cs.HasComments(method, includeDirectives: true));
    }

    [Fact]
    public void HasComments_StatementList()
    {
        var method = FirstMethod("""
            class A {
                void M() {
                    var x = 1;
                    var y = 2 /* end */;
                }
            }
            """);
        var statements = method.Body!.Statements;
        Assert.False(Cs.HasComments(statements.Take(1)));
        Assert.True(Cs.HasComments(statements));
    }

    [Fact]
    public void HasComments_Container()
    {
        var withComment = FirstMethod("class A { void M(int a /* x */) { } }");
        var withoutComment = FirstMethod("class A { void M(int a) { } }");
        var beforeParens = FirstMethod("class A { void M /* x */ (int a) { } }");
        Assert.True(Cs.HasComments(withComment.Parameters));
        Assert.False(Cs.HasComments(withoutComment.Parameters));
        Assert.True(Cs.HasComments(beforeParens.Parameters));
    }
}
