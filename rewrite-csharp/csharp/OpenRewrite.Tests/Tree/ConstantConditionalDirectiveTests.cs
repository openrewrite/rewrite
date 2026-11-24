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

public class ConstantConditionalDirectiveTests : RewriteTest
{
    [Fact]
    public void IfFalseAtTopLevel()
    {
        RewriteRun(
            CSharp(
                """
                #if false
                class Inactive { }
                #endif
                class Foo { }
                """
            )
        );
    }

    [Fact]
    public void IfFalseInMethodBody()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                #if false
                        int x = 1;
                #endif
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void IfFalseNextToSymbolDirective()
    {
        RewriteRun(
            CSharp(
                """
                #if DEBUG
                class Debug { }
                #endif
                #if false
                class Inactive { }
                #endif
                """
            )
        );
    }

    [Fact]
    public void IfFalseNestedInSymbolDirective()
    {
        RewriteRun(
            CSharp(
                """
                #if DEBUG
                #if false
                class Inactive { }
                #endif
                class Debug { }
                #endif
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedFalse()
    {
        RewriteRun(
            CSharp(
                """
                #if (false)
                class Inactive { }
                #endif
                """
            )
        );
    }

    [Fact]
    public void NegatedTrue()
    {
        RewriteRun(
            CSharp(
                """
                #if !true
                class Inactive { }
                #endif
                """
            )
        );
    }

    [Fact]
    public void IfTrueWithElse()
    {
        RewriteRun(
            CSharp(
                """
                #if true
                class Foo { }
                #else
                class Inactive { }
                #endif
                """
            )
        );
    }

    [Fact]
    public void IfTrueWithElifFalse()
    {
        RewriteRun(
            CSharp(
                """
                #if true
                class Foo { }
                #elif false
                class Inactive { }
                #endif
                """
            )
        );
    }

    [Fact]
    public void IfTrueOnly()
    {
        RewriteRun(
            CSharp(
                """
                #if true
                class Foo { }
                #endif
                """
            )
        );
    }
}
