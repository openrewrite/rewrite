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

public class InterpolatedStringTests : RewriteTest
{
    [Fact]
    public void BasicInterpolation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var name = "World";
                        var greeting = $"Hello {name}";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void InterpolationWithExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = 5;
                        var result = $"Value: {x + 1}";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void InterpolationWithFormatSpecifier()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var value = 3.14159;
                        var formatted = $"Pi: {value:F2}";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void InterpolationWithAlignment()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = 42;
                        var aligned = $"Value: {x,10}";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void InterpolationWithAlignmentAndFormat()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var value = 3.14159;
                        var result = $"Pi: {value,10:F2}";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void VerbatimInterpolatedString()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var path = "Users";
                        var result = $@"C:\{path}\file.txt";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleInterpolations()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var first = "Hello";
                        var last = "World";
                        var result = $"{first}, {last}!";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void EscapedBraces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = 42;
                        var result = $"Value: {{x}} = {x}";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedMethodCallInInterpolation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var name = "HELLO";
                        var result = $"Name: {name.ToLower()}";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void InterpolationWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = 5;
                        var result = $"Value: { x }";
                    }
                }
                """
            )
        );
    }
}
