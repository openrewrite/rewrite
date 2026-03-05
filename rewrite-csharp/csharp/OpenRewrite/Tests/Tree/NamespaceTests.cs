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

public class NamespaceTests : RewriteTest
{
    // File-scoped namespace tests (J.Package)

    [Fact]
    public void SimpleFileScopedNamespace()
    {
        RewriteRun(
            CSharp(
                """
                namespace Foo;
                """
            )
        );
    }

    [Fact]
    public void DottedFileScopedNamespace()
    {
        RewriteRun(
            CSharp(
                """
                namespace Foo.Bar.Baz;
                """
            )
        );
    }

    [Fact]
    public void FileScopedNamespaceWithClass()
    {
        RewriteRun(
            CSharp(
                """
                namespace Foo;

                class Bar {
                }
                """
            )
        );
    }

    [Fact]
    public void FileScopedNamespaceWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                namespace  Foo . Bar ;
                """
            )
        );
    }

    // Block-scoped namespace tests (Cs.NamespaceDeclaration)

    [Fact]
    public void SimpleBlockNamespace()
    {
        RewriteRun(
            CSharp(
                """
                namespace Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void DottedBlockNamespace()
    {
        RewriteRun(
            CSharp(
                """
                namespace Foo.Bar {
                }
                """
            )
        );
    }

    [Fact]
    public void BlockNamespaceWithClass()
    {
        RewriteRun(
            CSharp(
                """
                namespace Foo {
                    class Bar {
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedNamespaces()
    {
        RewriteRun(
            CSharp(
                """
                namespace Foo {
                    namespace Bar {
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleNamespaces()
    {
        RewriteRun(
            CSharp(
                """
                namespace Foo {
                }
                namespace Bar {
                }
                """
            )
        );
    }

    [Fact]
    public void BlockNamespaceWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                namespace  Foo . Bar  {
                }
                """
            )
        );
    }

    [Fact]
    public void BlockNamespaceWithMultipleClasses()
    {
        RewriteRun(
            CSharp(
                """
                namespace MyApp.Services {
                    class ServiceA {
                    }
                    class ServiceB {
                    }
                }
                """
            )
        );
    }
}
