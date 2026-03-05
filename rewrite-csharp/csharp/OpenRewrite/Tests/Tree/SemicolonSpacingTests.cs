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
}
