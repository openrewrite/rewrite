using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class PointerTests : RewriteTest
{
    [Fact]
    public void PointerDereference()
    {
        RewriteRun(
            CSharp(
                """
                unsafe class C {
                    void M(int* p) {
                        var x = *p;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void PointerMemberAccessField()
    {
        RewriteRun(
            CSharp(
                """
                unsafe struct S {
                    public int X;
                }
                unsafe class C {
                    void M(S* p) {
                        var x = p->X;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void PointerMemberAccessMethod()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    unsafe void M(int* p) {
                        p->ToString();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void PointerDereferenceExplicit()
    {
        RewriteRun(
            CSharp(
                """
                unsafe struct S {
                    public int X;
                }
                unsafe class C {
                    void M(S* p) {
                        var x = (*p).X;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AddressOf()
    {
        RewriteRun(
            CSharp(
                """
                unsafe class C {
                    void M() {
                        int x = 0;
                        int* p = &x;
                    }
                }
                """
            )
        );
    }
}
