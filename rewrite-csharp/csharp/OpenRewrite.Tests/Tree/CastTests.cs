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

public class CastTests : RewriteTest
{
    [Fact]
    public void SimpleCast()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        object o = 42;
                        int x = (int)o;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CastToString()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        object o = "hello";
                        string s = (string)o;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CastWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        object o = 42;
                        int x = ( int )o;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CastInExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        object o = 42;
                        int x = (int)o + 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedCast()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        object o = 42;
                        long x = (long)(int)o;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CastMethodResult()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    object GetValue() { return 42; }
                    void Bar() {
                        int x = (int)GetValue();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CastToDouble()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int i = 42;
                        double d = (double)i;
                    }
                }
                """
            )
        );
    }
}
