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

public class UsingStatementTests : RewriteTest
{
    [Fact]
    public void UsingWithBlock()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        using (var stream = new System.IO.MemoryStream()) {
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void UsingWithSingleEmbeddedStatement()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(System.IO.Stream content) {
                        using (var stream = new System.IO.MemoryStream())
                            content.CopyTo(stream);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void UsingWithSingleReturnStatement()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int Bar() {
                        using (var stream = new System.IO.MemoryStream())
                            return 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedUsingWithoutBraces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        using (var a = new System.IO.MemoryStream())
                        using (var b = new System.IO.MemoryStream())
                            a.CopyTo(b);
                    }
                }
                """
            )
        );
    }
}
