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

public class AwaitStatementTests : RewriteTest
{
    [Fact]
    public void AwaitForeach()
    {
        RewriteRun(
            CSharp(
                """
                using System.Collections.Generic;
                using System.Threading.Tasks;
                class C {
                    async Task M(IAsyncEnumerable<int> items) {
                        await foreach (var x in items) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AwaitUsing()
    {
        RewriteRun(
            CSharp(
                """
                using System.IO;
                using System.Threading.Tasks;
                class C {
                    async Task M() {
                        await using (var x = new MemoryStream()) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void RegularForeach()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M(int[] items) {
                        foreach (var x in items) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void RegularUsing()
    {
        RewriteRun(
            CSharp(
                """
                using System.IO;
                class C {
                    void M() {
                        using (var x = new MemoryStream()) { }
                    }
                }
                """
            )
        );
    }
}
