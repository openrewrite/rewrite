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
using OpenRewrite.CSharp.Search;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.CSharp.Search;

public class FindMissingTypesTests : RewriteTest
{
    [Fact]
    public void MarksMethodsParsedWithoutTypes()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindMissingTypes()).SetReferenceAssemblies(null),
            CSharp(
                """
                class A
                {
                    void M()
                    {
                        M();
                    }
                }
                """,
                """
                class A
                {
                    /*~~(MethodDeclaration type is missing)~~>*/void M()
                    {
                        /*~~(MethodInvocation type is missing)~~>*/M();
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
            ["MethodInvocation type is missing: M()", "MethodDeclaration type is missing: void M() ..."],
            FindMissingTypes.Find(cu));
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
