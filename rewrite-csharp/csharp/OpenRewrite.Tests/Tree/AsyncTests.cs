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
