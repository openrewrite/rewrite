using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

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
