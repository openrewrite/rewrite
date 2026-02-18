using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class InterfaceTests : RewriteTest
{
    [Fact]
    public void SimpleInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithMethod()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                    void Bar();
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithProperty()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                    int Value { get; set; }
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithModifiers()
    {
        RewriteRun(
            CSharp(
                """
                public interface IFoo {
                    void Bar();
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithBaseInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                }
                interface IBar : IFoo {
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithMultipleBaseInterfaces()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                }
                interface IBar {
                }
                interface IBaz : IFoo, IBar {
                }
                """
            )
        );
    }

    [Fact]
    public void GenericInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<T> {
                    T GetValue();
                }
                """
            )
        );
    }

    [Fact]
    public void GenericInterfaceWithConstraint()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<T> where T : class {
                    T GetValue();
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithDefaultImplementation()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                    void Bar() {
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                interface  IFoo  {
                }
                """
            )
        );
    }
}
