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
            }
            """;
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var tree = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("A", [tree], refs, new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(tree));

        Assert.Equal(["FieldAccess type is missing: s.Length"],
            FindMissingTypes.Find((CompilationUnit)new LengthTypeRemover().Visit(cu, 0)!));
    }

    private sealed class LengthTypeRemover : CSharpVisitor<int>
    {
        public override J VisitFieldAccess(FieldAccess fieldAccess, int p) =>
            fieldAccess.Name.Element.SimpleName == "Length" ? fieldAccess.WithType(null) : base.VisitFieldAccess(fieldAccess, p);
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
