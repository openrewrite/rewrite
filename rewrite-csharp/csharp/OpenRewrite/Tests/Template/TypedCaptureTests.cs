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
        var pat = CSharpPattern.Expression($"{s}.Length");
        var tree = pat.GetTree();
        Assert.IsType<FieldAccess>(tree);
    }

    [Fact]
    public void TypedCaptureEnablesMethodCall()
    {
        // With a typed capture, `{s}.ToUpper()` should parse correctly
        var s = Capture.Of<Expression>("s", type: "string");
        var pat = CSharpPattern.Expression($"{s}.ToUpper()");
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
        var pat = CSharpPattern.Expression($"{n}.ToString()");
        var tree = pat.GetTree();
        Assert.IsType<MethodInvocation>(tree);
    }

    [Fact]
    public void MultipleTypedCaptures()
    {
        var s = Capture.Of<Expression>("s", type: "string");
        var n = Capture.Of<Expression>("n", type: "int");
        var pat = CSharpPattern.Expression($"{s}.Substring({n})");
        var tree = pat.GetTree();
        Assert.IsType<MethodInvocation>(tree);
    }

    [Fact]
    public void TypedCaptureRejectsIncompatibleType()
    {
        var target = Capture.Expression("target", type: "System.IO.BinaryReader");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"{target}.ReadString()", ["System.IO"]))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                // MyReader.ReadString() should NOT match — MyReader is not BinaryReader
                """
                class MyReader
                {
                    public string ReadString() => "hello";
                }
                class Test
                {
                    void M(MyReader reader)
                    {
                        var text = reader.ReadString();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TypedCaptureMatchesCompatibleType()
    {
        var target = Capture.Expression("target", type: "System.IO.BinaryReader");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"{target}.ReadString()", ["System.IO"]))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                "using System.IO; class Test { void M(BinaryReader r) { r.ReadString(); } }",
                "using System.IO; class Test { void M(BinaryReader r) { /*~~>*/r.ReadString(); } }"
            )
        );
    }

    [Fact]
    public void TypedCaptureMatchesGenericInterfaceImplementation()
    {
        var dict = Capture.Expression("dict",
            type: "IDictionary<TKey, TValue>",
            typeParameters: ["TKey", "TValue"]);
        var key = Capture.Expression("key");
        var pat = CSharpPattern.Expression($"{dict}.Keys.Contains({key})",
            usings: ["System.Collections.Generic"]);

        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation(pat))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Collections.Generic;
                class Test
                {
                    void M()
                    {
                        var dict = new Dictionary<string, int>();
                        bool has = dict.Keys.Contains("key");
                    }
                }
                """,
                """
                using System.Collections.Generic;
                class Test
                {
                    void M()
                    {
                        var dict = new Dictionary<string, int>();
                        bool has = /*~~>*/dict.Keys.Contains("key");
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TypedCaptureMatchesPrimitiveInt()
    {
        var expr = Capture.Expression("expr");
        var idx = Capture.Expression("idx", type: "int");
        var pat = CSharpPattern.Expression($"{expr}.ElementAt({idx})",
            usings: ["System.Linq"]);

        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation(pat))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Linq;
                using System.Collections.Generic;
                class Test
                {
                    void M()
                    {
                        var list = new List<string>();
                        var item = list.ElementAt(0);
                    }
                }
                """,
                """
                using System.Linq;
                using System.Collections.Generic;
                class Test
                {
                    void M()
                    {
                        var list = new List<string>();
                        var item = /*~~>*/list.ElementAt(0);
                    }
                }
                """
            )
        );
    }

    // ===============================================================
    // Open generic captures
    // ===============================================================

    [Fact]
    public void OpenGenericCapture_MatchesAnyDictionary()
    {
        var dict = Capture.Expression("dict",
            type: "IDictionary<TKey, TValue>",
            typeParameters: ["TKey", "TValue"]);
        var pat = CSharpPattern.Expression($"{dict}.Count",
            usings: ["System.Collections.Generic"]);

        RewriteRun(
            spec => spec.SetRecipe(FindExpression(pat))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Collections.Generic;
                class Test
                {
                    void M()
                    {
                        var d = new Dictionary<string, int>();
                        var n = d.Count;
                    }
                }
                """,
                """
                using System.Collections.Generic;
                class Test
                {
                    void M()
                    {
                        var d = new Dictionary<string, int>();
                        var n = /*~~>*/d.Count;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void OpenGenericCapture_PartialFixedFirstParam()
    {
        var dict = Capture.Expression("dict",
            type: "IDictionary<string, TValue>",
            typeParameters: ["TValue"]);
        var pat = CSharpPattern.Expression($"{dict}.Count",
            usings: ["System.Collections.Generic"]);

        RewriteRun(
            spec => spec.SetRecipe(FindExpression(pat))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                // Dictionary<string, int> — first param is string → should match
                """
                using System.Collections.Generic;
                class Test
                {
                    void M()
                    {
                        var d = new Dictionary<string, int>();
                        var n = d.Count;
                    }
                }
                """,
                """
                using System.Collections.Generic;
                class Test
                {
                    void M()
                    {
                        var d = new Dictionary<string, int>();
                        var n = /*~~>*/d.Count;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void OpenGenericCapture_PartialFixedFirstParam_Rejects()
    {
        var dict = Capture.Expression("dict",
            type: "IDictionary<string, TValue>",
            typeParameters: ["TValue"]);
        var pat = CSharpPattern.Expression($"{dict}.Count",
            usings: ["System.Collections.Generic"]);

        RewriteRun(
            spec => spec.SetRecipe(FindExpression(pat))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                // Dictionary<int, string> — first param is int, not string → no match
                """
                using System.Collections.Generic;
                class Test
                {
                    void M()
                    {
                        var d = new Dictionary<int, string>();
                        var n = d.Count;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void OpenGenericCapture_WithBound()
    {
        var list = Capture.Expression("list",
            type: "IEnumerable<T>",
            typeParameters: ["T : IComparable"]);
        var pat = CSharpPattern.Expression($"{list}.Count()",
            usings: ["System.Collections.Generic", "System.Linq"]);

        RewriteRun(
            spec => spec.SetRecipe(FindExpression(pat))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                // List<string> — string implements IComparable → should match
                """
                using System.Collections.Generic;
                using System.Linq;
                class Test
                {
                    void M()
                    {
                        var list = new List<string>();
                        var n = list.Count();
                    }
                }
                """,
                """
                using System.Collections.Generic;
                using System.Linq;
                class Test
                {
                    void M()
                    {
                        var list = new List<string>();
                        var n = /*~~>*/list.Count();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void OpenGenericCapture_StoresTypeParameters()
    {
        var cap = Capture.Expression("x",
            type: "IDictionary<TKey, TValue>",
            typeParameters: ["TKey", "TValue"]);
        Assert.NotNull(cap.TypeParameters);
        Assert.Equal(2, cap.TypeParameters!.Count);
        Assert.Equal("TKey", cap.TypeParameters[0]);
        Assert.Equal("TValue", cap.TypeParameters[1]);
    }

    [Fact]
    public void ConcreteGenericCapture_StillMatchesExact()
    {
        // No typeParameters → concrete generic, should only match exact type args
        var dict = Capture.Expression("dict", type: "Dictionary<string, string>");
        var pat = CSharpPattern.Expression($"{dict}.Count",
            usings: ["System.Collections.Generic"]);

        RewriteRun(
            spec => spec.SetRecipe(FindExpression(pat))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                // Dictionary<string, int> — type args don't match → no match
                """
                using System.Collections.Generic;
                class Test
                {
                    void M()
                    {
                        var d = new Dictionary<string, int>();
                        var n = d.Count;
                    }
                }
                """
            )
        );
    }

    // ===============================================================
    // Recipe factories
    // ===============================================================

    private static Core.Recipe FindExpression(TemplateStringHandler handler)
        => new TypedPatternSearchRecipe(CSharpPattern.Expression(handler));

    private static Core.Recipe FindExpression(string code)
        => new TypedPatternSearchRecipe(CSharpPattern.Expression(code));

    private static Core.Recipe FindExpression(CSharpPattern pat)
        => new TypedPatternSearchRecipe(pat);

    private static Core.Recipe FindMethodInvocation(TemplateStringHandler handler, IReadOnlyList<string> usings)
        => new MethodInvocationSearchRecipe(CSharpPattern.Expression(handler, usings: usings));

    private static Core.Recipe FindMethodInvocation(CSharpPattern pat)
        => new MethodInvocationSearchRecipe(pat);
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

file class MethodInvocationSearchRecipe(CSharpPattern pat) : Core.Recipe
{
    public override string DisplayName => "Find method invocation";
    public override string Description => "Searches for method invocations matching the pattern.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new SearchVisitor(pat);

    private class SearchVisitor(CSharpPattern pat) : CSharpVisitor<ExecutionContext>
    {
        public override J? PreVisit(J tree, ExecutionContext ctx)
        {
            if (tree is MethodInvocation m)
            {
                return pat.Find(m, Cursor);
            }
            return tree;
        }
    }
}
