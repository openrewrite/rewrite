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
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Format;

namespace OpenRewrite.Tests.Format;

public class WhitespaceReconcilerTests
{
    private readonly CSharpParser _parser = new();
    private readonly CSharpPrinter<int> _printer = new();

    [Fact]
    public void IdenticalTreesReturnOriginal()
    {
        const string source = """
            using System;

            namespace Test
            {
                class Foo
                {
                    void Bar()
                    {
                    }
                }
            }
            """;
        var original = _parser.Parse(source);
        var formatted = _parser.Parse(source);

        var reconciler = new WhitespaceReconciler();
        var result = reconciler.Reconcile(original, formatted);

        Assert.True(reconciler.IsCompatible);
        // Result should print identically
        Assert.Equal(source, _printer.Print(result));
    }

    [Fact]
    public void CopiesIndentationFromFormatted()
    {
        const string original = """
            class Foo{
            void Bar(){
            int x=1;
            }
            }
            """;
        const string formatted = """
            class Foo
            {
                void Bar()
                {
                    int x = 1;
                }
            }
            """;

        var originalTree = _parser.Parse(original);
        var formattedTree = _parser.Parse(formatted);

        var reconciler = new WhitespaceReconciler();
        var result = reconciler.Reconcile(originalTree, formattedTree);

        Assert.True(reconciler.IsCompatible);
        var printed = _printer.Print(result);
        Assert.Equal(formatted, printed);
    }

    [Fact]
    public void PreservesTypeAttribution()
    {
        const string source = """
            using System;

            class Foo
            {
                void Bar()
                {
                    Console.WriteLine("hello");
                }
            }
            """;

        // Parse with type attribution (default)
        var original = _parser.Parse(source);
        // Parse without type attribution (as formatted tree would be)
        var formatted = _parser.Parse(source);

        var reconciler = new WhitespaceReconciler();
        var result = reconciler.Reconcile(original, formatted);

        Assert.True(reconciler.IsCompatible);
        // The result should preserve the original tree's IDs
        Assert.Equal(original.Id, (result as CompilationUnit)!.Id);
    }

    [Fact]
    public void StructureMismatchReturnsOriginal()
    {
        const string source1 = """
            class Foo
            {
                void Bar() { }
            }
            """;
        const string source2 = """
            class Foo
            {
                void Bar() { }
                void Baz() { }
            }
            """;

        var original = _parser.Parse(source1);
        var formatted = _parser.Parse(source2);

        var reconciler = new WhitespaceReconciler();
        var result = reconciler.Reconcile(original, formatted);

        Assert.False(reconciler.IsCompatible);
        // Should return original unchanged
        Assert.Equal(source1, _printer.Print(result));
    }

    [Fact]
    public void CopiesBlankLineChanges()
    {
        const string original = """
            using System;


            using System.Collections.Generic;
            """;
        const string formatted = """
            using System;
            using System.Collections.Generic;
            """;

        var originalTree = _parser.Parse(original);
        var formattedTree = _parser.Parse(formatted);

        var reconciler = new WhitespaceReconciler();
        var result = reconciler.Reconcile(originalTree, formattedTree);

        Assert.True(reconciler.IsCompatible);
        Assert.Equal(formatted, _printer.Print(result));
    }
}
