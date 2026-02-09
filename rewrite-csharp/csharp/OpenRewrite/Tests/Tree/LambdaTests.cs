using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class LambdaTests : RewriteTest
{
    // Simple lambdas (single parameter without parentheses)

    [Fact]
    public void SimpleLambdaExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = x => x * 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SimpleLambdaWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = x  =>  x * 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SimpleLambdaWithStringBody()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = x => "hello";
                    }
                }
                """
            )
        );
    }

    // Parenthesized lambdas (explicit parentheses around parameter(s))

    [Fact]
    public void ParenthesizedSingleParameter()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (x) => x * 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedNoParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = () => 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedNoParametersWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (  ) => 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedTwoParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (x, y) => x + y;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedTwoParametersWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = ( x , y ) => x + y;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedThreeParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (x, y, z) => x + y + z;
                    }
                }
                """
            )
        );
    }

    // Typed parameters

    [Fact]
    public void TypedSingleParameter()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (int x) => x * 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TypedTwoParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (int x, int y) => x + y;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TypedMixedParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (string s, int n) => s + n;
                    }
                }
                """
            )
        );
    }

    // Block body lambdas

    [Fact]
    public void SimpleLambdaWithBlock()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = x => { return x * 2; };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedLambdaWithBlock()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (x, y) => { return x + y; };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoParametersWithBlock()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = () => { return 42; };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void BlockWithMultipleStatements()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (x) => {
                            var y = x * 2;
                            return y;
                        };
                    }
                }
                """
            )
        );
    }

    // Lambda as argument

    [Fact]
    public void LambdaAsMethodArgument()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(x => x * 2);
                    }
                    void DoSomething(object f) { }
                }
                """
            )
        );
    }

    [Fact]
    public void LambdaAsSecondArgument()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(1, x => x * 2);
                    }
                    void DoSomething(int a, object f) { }
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleLambdaArguments()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(x => x * 2, y => y + 1);
                    }
                    void DoSomething(object f1, object f2) { }
                }
                """
            )
        );
    }

    // Lambda in various contexts

    [Fact]
    public void LambdaInReturn()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    object Bar() {
                        return x => x * 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LambdaInTernary()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(bool b) {
                        var f = b ? x => x * 2 : x => x * 3;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LambdaInParentheses()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (x => x * 2);
                    }
                }
                """
            )
        );
    }

    // Nested lambdas

    [Fact]
    public void NestedLambda()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = x => y => x + y;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedLambdaWithBlocks()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = x => {
                            return y => {
                                return x + y;
                            };
                        };
                    }
                }
                """
            )
        );
    }

    // Complex expressions in body

    [Fact]
    public void LambdaWithMethodCall()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = x => x.ToString();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LambdaWithNewExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = () => new Foo();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LambdaWithNullCoalescing()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (x) => x ?? "default";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LambdaWithBinaryExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (a, b) => a > b && a < 100;
                    }
                }
                """
            )
        );
    }

    // Async lambdas (C#-specific, uses CsLambda wrapper)

    [Fact]
    public void AsyncLambdaSimple()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = async x => x;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AsyncLambdaParenthesized()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = async (x) => x;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AsyncLambdaNoParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = async () => 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AsyncLambdaWithBlock()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = async () => { return 42; };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AsyncLambdaWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = async  x  =>  x;
                    }
                }
                """
            )
        );
    }

    // Static lambdas (C# 9+)

    [Fact]
    public void StaticLambdaSimple()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = static x => x * 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void StaticLambdaParenthesized()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = static (x, y) => x + y;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void StaticLambdaNoParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = static () => 42;
                    }
                }
                """
            )
        );
    }

    // Async static lambdas

    [Fact]
    public void AsyncStaticLambda()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = async static () => 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void StaticAsyncLambda()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = static async () => 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AsyncStaticLambdaWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = async  static  (x)  =>  x;
                    }
                }
                """
            )
        );
    }

    // Explicit return type (C# 10+)

    [Fact]
    public void ExplicitReturnTypeInt()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = int (x) => x * 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ExplicitReturnTypeString()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = string () => "hello";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ExplicitReturnTypeWithTypedParams()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = int (int x, int y) => x + y;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ExplicitReturnTypeAsync()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = async int () => 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ExplicitReturnTypeAsyncStatic()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = async static int () => 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ExplicitReturnTypeWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = int  (int x)  =>  x;
                    }
                }
                """
            )
        );
    }

    // Edge cases with modifiers

    [Fact]
    public void AsyncLambdaAsArgument()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(async x => x);
                    }
                    void DoSomething(object f) { }
                }
                """
            )
        );
    }

    [Fact]
    public void StaticLambdaInTernary()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(bool b) {
                        var f = b ? static x => x * 2 : static x => x * 3;
                    }
                }
                """
            )
        );
    }

    // Default parameter values (C# 12+)

    [Fact]
    public void DefaultParameterValue()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (int x = 10) => x * 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void DefaultParameterValueWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (int x  =  10) => x * 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void DefaultParameterValueString()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (string s = "default") => s.ToUpper();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleParametersWithDefaults()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (int x = 1, int y = 2) => x + y;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MixedDefaultAndNonDefault()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (int x, int y = 10) => x + y;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void DefaultParameterWithExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = (int x = 1 + 2) => x * 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void DefaultParameterWithAsyncStatic()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var f = async static (int x = 42) => x;
                    }
                }
                """
            )
        );
    }
}
