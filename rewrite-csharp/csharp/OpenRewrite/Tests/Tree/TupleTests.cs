using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class TupleTests : RewriteTest
{
    // Tuple type tests

    [Fact]
    public void BasicTupleType()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    (int, int) M() {
                        return (1, 2);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NamedTupleType()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    (int x, int y) M() {
                        return (1, 2);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MixedNamedTupleType()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    (int, string name) M() {
                        return (1, "test");
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TupleTypeVariable()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        (int, int) myTuple;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NamedTupleTypeVariable()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        (int x, int y) point;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TupleTypeParameter()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void Process((int, string) data) {
                    }
                }
                """
            )
        );
    }

    // Tuple expression tests

    [Fact]
    public void SimpleTupleExpression()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        var t = (1, 2);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NamedTupleExpression()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        var p = (name: "John", age: 25);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedTupleExpression()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        var n = (1, (2, 3));
                    }
                }
                """
            )
        );
    }

    // TODO: Tuple deconstruction requires handling DeclarationExpressionSyntax
    // with ParenthesizedVariableDesignationSyntax
    // [Fact]
    // public void TupleDeconstruction()
    // {
    //     RewriteRun(
    //         CSharp(
    //             """
    //             class C {
    //                 void M() {
    //                     var (x, y) = (1, 2);
    //                 }
    //             }
    //             """
    //         )
    //     );
    // }

    // [Fact]
    // public void TupleDeconstructionWithVar()
    // {
    //     RewriteRun(
    //         CSharp(
    //             """
    //             class C {
    //                 void M() {
    //                     (var x, var y) = (1, 2);
    //                 }
    //             }
    //             """
    //         )
    //     );
    // }

    [Fact]
    public void TupleWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        var t = ( 1 , 2 );
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TupleTypeWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    ( int , string ) M() {
                        return (1, "test");
                    }
                }
                """
            )
        );
    }
}
