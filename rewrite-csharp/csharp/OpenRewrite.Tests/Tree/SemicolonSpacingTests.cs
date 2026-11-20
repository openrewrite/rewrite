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
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class SemicolonSpacingTests : RewriteTest
{
    [Fact]
    public void ExpressionStatementSimple()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        Console.WriteLine();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ExpressionStatementSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        Console.WriteLine() ;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ReturnSimple()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    int M() {
                        return 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ReturnSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    int M() {
                        return 1 ;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void VariableDeclarationLocal()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        int x = 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void VariableDeclarationSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        int x = 1 ;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FieldDeclaration()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    int x = 1;
                }
                """
            )
        );
    }

    [Fact]
    public void AbstractMethod()
    {
        RewriteRun(
            CSharp(
                """
                abstract class C {
                    abstract void M();
                }
                """
            )
        );
    }

    [Fact]
    public void UsingDirectiveSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                using System ;
                using static System.Math ;
                using Text = global::System.Text ;

                class C {
                }
                """
            )
        );
    }

    [Fact]
    public void ExternAliasSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                extern alias A ;
                namespace N;
                extern alias B ;
                class C {
                }
                """
            ),
            CSharp(
                """
                namespace N {
                    extern alias B ;
                    class C {
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LabeledStatementSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M(int x) {
                        L: x++ ;
                        A: B: return ;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void JumpStatementSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                using System;
                using System.Collections.Generic;

                class C {
                    IEnumerable<int> M(int x) {
                        while (true) {
                            if (x > 9) break ;
                            x++;
                            continue ;
                        }
                        switch (x) {
                            case 1: goto case 2 ;
                            case 2: goto default ;
                            default: goto L ;
                        }
                        L: x++;
                        yield return x ;
                        try { } catch { throw ; }
                        if (x < 0) throw new Exception() ;
                        yield break ;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void DoWhileSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M(int x) {
                        do { } while (x < 5) ;

                        do x++ ; while (x < 5) ;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MemberSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    event System.Action E ;
                    int this[int i] => i ;
                    public static implicit operator int(C c) => 1 ;
                }
                """
            )
        );
    }

    [Fact]
    public void EmptyStatementSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M(int x) {
                        while (x < 5) ;
                        for (;;) ;
                        if (x > 0) ; else ;
                        ;
                    }
                }
                """
            )
        );
    }
}
