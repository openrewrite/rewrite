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

public class PropertyDeclarationTests : RewriteTest
{
    [Fact]
    public void SimpleAutoProperty()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public int X { get; set; }
                }
                """
            )
        );
    }

    [Fact]
    public void GetOnlyAutoProperty()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public string Name { get; }
                }
                """
            )
        );
    }

    [Fact]
    public void PropertyWithPrivateSetter()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public int Value { get; private set; }
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleProperties()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public int X { get; set; }
                    public int Y { get; set; }
                }
                """
            )
        );
    }

    [Fact]
    public void PropertyExpressionBody()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public int X => 42;
                }
                """
            )
        );
    }

    [Fact]
    public void PropertyExpressionBodyWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public int X  =>  42;
                }
                """
            )
        );
    }

    [Fact]
    public void AccessorExpressionBody()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    private int _x;
                    public int X { get => _x; set => _x = value; }
                }
                """
            )
        );
    }

    [Fact]
    public void AccessorExpressionBodyGetOnly()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    private int _x;
                    public int X { get => _x; }
                }
                """
            )
        );
    }
}
