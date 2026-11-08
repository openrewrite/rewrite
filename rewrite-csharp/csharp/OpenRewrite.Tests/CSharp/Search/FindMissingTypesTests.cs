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
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Search;
using OpenRewrite.Java;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.CSharp.Search;

public class FindMissingTypesTests : RewriteTest
{
    [Fact]
    public void MarksNodesParsedWithoutTypes()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()).SetReferenceAssemblies(null),
            CSharp(
                """
                class A
                {
                    A a;

                    void M()
                    {
                        M();
                        new A();
                        a.a = null;
                    }
                }
                """,
                """
                /*~~(ClassDeclaration type is missing)~~>*/class A
                {
                    /*~~(Identifier type is missing)~~>*/A /*~~(Identifier type is missing)~~>*/a;

                    /*~~(MethodDeclaration type is missing)~~>*/void M()
                    {
                        /*~~(MethodInvocation type is missing)~~>*/M();
                        /*~~(NewClass type is missing)~~>*/new A();
                        /*~~(Identifier type is missing)~~>*/a./*~~(Identifier type is missing)~~>*/a = null;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindListsEachNodeItWouldMarkByItsFirstLine()
    {
        var cu = new CSharpParser().Parse(
            """
            class A
            {
                void M()
                {
                    M();
                }
            }
            """);

        Assert.Equal(
            ["MethodInvocation type is missing: M()", "MethodDeclaration type is missing: void M() ...",
             "ClassDeclaration type is missing: class A ..."],
            FindMissingTypes.Find(cu));
    }

    [Fact]
    public async Task MarksAMemberAccessWithoutATypeWhoseNameHasOne()
    {
        const string source = """
            class A
            {
                int M(string s) => s.Length;
                System.Action G() => System.Console.WriteLine;
                string N() => nameof(A.G);
            }
            """;
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var tree = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("A", [tree], refs, new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(tree));

        Assert.Equal(["FieldAccess type is missing: s.Length", "FieldAccess type is missing: System.Console.WriteLine"],
            FindMissingTypes.Find((CompilationUnit)new MemberTypeRemover().Visit(cu, 0)!));
    }

    private sealed class MemberTypeRemover : CSharpVisitor<int>
    {
        public override J VisitFieldAccess(FieldAccess fieldAccess, int p) =>
            fieldAccess.Name.Element.SimpleName is "Length" or "WriteLine"
                ? fieldAccess.WithType(null)
                : base.VisitFieldAccess(fieldAccess, p);
    }

    [Fact]
    public async Task MarksOperatorDeclarationsWithoutAMethodType()
    {
        const string source = """
            class A
            {
                public static A operator +(A a, A b) => a;
                public static implicit operator A(int i) => new A();
            }
            """;
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var tree = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("A", [tree], refs, new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(tree));

        Assert.Empty(FindMissingTypes.Find(cu));
        Assert.Equal(
            ["OperatorDeclaration type is missing: public static A operator +(A a, A b) => a;",
             "ConversionOperatorDeclaration type is missing: public static implicit operator A(int i) => new A()"],
            FindMissingTypes.Find((CompilationUnit)new OperatorMethodTypeRemover().Visit(cu, 0)!));
    }

    private sealed class OperatorMethodTypeRemover : CSharpVisitor<int>
    {
        public override J VisitOperatorDeclaration(OperatorDeclaration operatorDeclaration, int p) =>
            ((OperatorDeclaration)base.VisitOperatorDeclaration(operatorDeclaration, p)).WithMethodType(null);

        public override J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration conversion, int p) =>
            ((ConversionOperatorDeclaration)base.VisitConversionOperatorDeclaration(conversion, p)).WithMethodType(null);
    }

    [Fact]
    public async Task MarksTypesWithAMissingPart()
    {
        const string source = """
            using System.Collections.Generic;
            class A
            {
                int M(string s, List<int> xs)
                {
                    var n = int.Parse(s);
                    return n + xs.Count;
                }
            }
            """;
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var tree = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("A", [tree], refs, new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(tree));

        Assert.Empty(FindMissingTypes.Find(cu));
        Assert.Equal(
            ["MethodInvocation type is malformed: int.Parse(s)", "Identifier field type is malformed: n",
             "Identifier type is malformed: xs"],
            FindMissingTypes.Find((CompilationUnit)new PartRemover().Visit(cu, 0)!));
    }

    private sealed class PartRemover : CSharpVisitor<int>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, int p)
        {
            var visited = (MethodInvocation)base.VisitMethodInvocation(mi, p);
            if (visited.MethodType is not { Name: "Parse" } m)
                return visited;
            var undeclared = new JavaType.Method(null, m.Name, m.FlagsBitMap, m.ReturnType, m.ParameterNames,
                m.ParameterTypes, m.ThrownExceptions, m.Annotations, m.DefaultValue, m.DeclaredFormalTypeNames);
            return visited.WithMethodType(undeclared).WithName(visited.Name.WithType(undeclared));
        }

        public override J VisitIdentifier(Identifier identifier, int p) =>
            Cursor.ParentTree.Value is NamedVariable ? identifier : identifier switch
            {
                { SimpleName: "n", FieldType: JavaType.Variable v } =>
                    identifier.WithFieldType(new JavaType.Variable(v.Name, null, v.Type, null)),
                { SimpleName: "xs", Type: JavaType.Parameterized list } =>
                    identifier.WithType(new JavaType.Parameterized(list.Type, [JavaType.Unknown.Instance])),
                _ => identifier
            };
    }

    [Fact]
    public async Task MarksTypesThatNameAnotherMember()
    {
        const string source = """
            class A
            {
                int x;
                void M()
                {
                    N();
                    x = 1;
                }
                void N() { }
            }
            """;
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var tree = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("A", [tree], refs, new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(tree));

        Assert.Empty(FindMissingTypes.Find(cu));
        Assert.Equal(
            ["MethodInvocation name type is not its method type: N()", "Identifier field type has a different name 'y': x",
             "MethodDeclaration type has a different name 'O': void M() ..."],
            FindMissingTypes.Find((CompilationUnit)new MemberRenamer().Visit(cu, 0)!));
    }

    private sealed class MemberRenamer : CSharpVisitor<int>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, int p)
        {
            var visited = (MethodInvocation)base.VisitMethodInvocation(mi, p);
            return visited.WithName(visited.Name.WithType(null));
        }

        public override J VisitMethodDeclaration(MethodDeclaration method, int p)
        {
            var visited = (MethodDeclaration)base.VisitMethodDeclaration(method, p);
            if (visited is not { Name.SimpleName: "M", MethodType: { } m })
                return visited;
            return visited.WithMethodType(new JavaType.Method(m.DeclaringType, "O", m.FlagsBitMap, m.ReturnType,
                m.ParameterNames, m.ParameterTypes, m.ThrownExceptions, m.Annotations, m.DefaultValue,
                m.DeclaredFormalTypeNames));
        }

        public override J VisitIdentifier(Identifier identifier, int p) =>
            identifier is { SimpleName: "x", FieldType: JavaType.Variable v } && Cursor.ParentTree.Value is not NamedVariable
                ? identifier.WithFieldType(new JavaType.Variable("y", v.Owner, v.Type, null))
                : identifier;
    }

    [Fact]
    public void LeavesNamesThatCSharpSpellsItsOwnWayUnmarked()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()),
            CSharp(
                """
                class A
                {
                    static A() { }
                    A() { }
                    ~A() { }
                    int @base, \u0068ex;
                    void @event() => @base = \u0068ex;
                }
                """
            )
        );
    }

    [Fact]
    public async Task LeavesNamesThatDoNotResolveUnmarked()
    {
        const string source = """
            class A
            {
                void M() => Undeclared = 1;
            }
            """;
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var tree = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("A", [tree], refs, new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(tree));

        Assert.Empty(FindMissingTypes.Find(cu));
    }

    [Fact]
    public void LeavesAttributedMethodsUnmarked()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()),
            CSharp(
                """
                class A
                {
                    void M()
                    {
                        System.Console.WriteLine();
                        M();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LeavesNamespacesUnmarked()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()),
            CSharp(
                """
                namespace Outer.Inner
                {
                    class B { }

                    class A
                    {
                        [global::System.Obsolete]
                        void M()
                        {
                            global::System.Console.WriteLine();
                            System.Int32 i = 0;
                            Inner.B b = null;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LeavesAttributeTargetsAndDiscardsUnmarked()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()),
            CSharp(
                """
                [assembly: System.Reflection.AssemblyVersion("1.0.0.0")]
                class A
                {
                    [return: System.Diagnostics.CodeAnalysis.NotNull]
                    object M()
                    {
                        var (_, x) = (1, 2);
                        return x;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LeavesCallsBoundAtRunTimeUnmarked()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()),
            CSharp(
                """
                class A
                {
                    static dynamic T;

                    void M(dynamic d)
                    {
                        d.Goo();
                        System.Console.WriteLine(d);
                        d["key"].ToString().Replace("a", "b");
                        T("key");
                        A.T("key").ToString();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LeavesCreationsOfATypeParameterUnmarked()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()),
            CSharp(
                """
                class A
                {
                    T M<T>() where T : new() => new T();
                }
                """
            )
        );
    }

    [Fact]
    public void LeavesNamesTheSourceOmitsUnmarked()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()),
            CSharp(
                """
                using System.Collections.Generic;

                class A
                {
                    void M()
                    {
                        try { } catch (System.Exception) when (true) { }
                        try { } catch when (true) { }
                        var t = typeof(Dictionary<,>);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LeavesOperatorsWrittenAsCallsUnmarked()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()),
            CSharp(
                """
                class A
                {
                    void M(int x)
                    {
                        var n = nameof(x);
                        System.TypedReference r = __makeref(x);
                        System.Type t = __reftype(r);
                        int v = __refvalue(r, int);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LeavesNamesWithoutATypeInNameofUnmarked()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()),
            CSharp(
                """
                class A
                {
                    string M() => nameof(System) + nameof(System.Enum.Parse);
                }
                """
            )
        );
    }

    [Fact]
    public void ChecksOnlyThePreprocessorBranchParsedWithTypes()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()),
            CSharp(
                """
                class A
                {
                    void M()
                    {
                #if DEBUG
                        M();
                #else
                        M();
                #endif
                    }
                }
                """
            )
        );
    }
}
