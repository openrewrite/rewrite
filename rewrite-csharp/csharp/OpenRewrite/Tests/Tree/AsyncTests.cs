using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class AsyncTests : RewriteTest
{
    [Fact]
    public void SimpleAwait()
    {
        RewriteRun(
            CSharp(
                """
                using System.Threading.Tasks;
                class Foo {
                    async Task Bar() {
                        await Task.CompletedTask;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AwaitMethodCall()
    {
        RewriteRun(
            CSharp(
                """
                using System.Threading.Tasks;
                class Foo {
                    async Task Bar() {
                        await DoSomethingAsync();
                    }
                    Task DoSomethingAsync() { return Task.CompletedTask; }
                }
                """
            )
        );
    }

    [Fact]
    public void AwaitWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                using System.Threading.Tasks;
                class Foo {
                    async Task Bar() {
                        await  DoSomethingAsync();
                    }
                    Task DoSomethingAsync() { return Task.CompletedTask; }
                }
                """
            )
        );
    }

    [Fact]
    public void AwaitInAsyncMethod()
    {
        RewriteRun(
            CSharp(
                """
                using System.Threading.Tasks;
                class Foo {
                    async Task<int> GetValueAsync() {
                        await Task.Delay(100);
                        return 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AwaitInAsyncLambda()
    {
        RewriteRun(
            CSharp(
                """
                using System;
                using System.Threading.Tasks;
                class Foo {
                    void Bar() {
                        Func<Task> f = async () => await Task.CompletedTask;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AwaitTaskDelay()
    {
        RewriteRun(
            CSharp(
                """
                using System.Threading.Tasks;
                class Foo {
                    async Task Bar() {
                        await Task.Delay(1000);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AwaitChainedCalls()
    {
        RewriteRun(
            CSharp(
                """
                using System.Threading.Tasks;
                class Foo {
                    async Task Bar() {
                        await Task.CompletedTask.ConfigureAwait(false);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleAwaits()
    {
        RewriteRun(
            CSharp(
                """
                using System.Threading.Tasks;
                class Foo {
                    async Task Bar() {
                        await Task.Delay(100);
                        await Task.Delay(200);
                        await Task.Delay(300);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AwaitInExpression()
    {
        RewriteRun(
            CSharp(
                """
                using System.Threading.Tasks;
                class Foo {
                    async Task<int> Bar() {
                        var result = await GetValueAsync() + 1;
                        return result;
                    }
                    Task<int> GetValueAsync() { return Task.FromResult(41); }
                }
                """
            )
        );
    }

    [Fact]
    public void AwaitWithParentheses()
    {
        RewriteRun(
            CSharp(
                """
                using System.Threading.Tasks;
                class Foo {
                    async Task Bar() {
                        await (Task.CompletedTask);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedAwait()
    {
        RewriteRun(
            CSharp(
                """
                using System.Threading.Tasks;
                class Foo {
                    async Task Bar() {
                        await GetTaskAsync();
                    }
                    async Task<Task> GetTaskAsync() {
                        return Task.CompletedTask;
                    }
                }
                """
            )
        );
    }
}
