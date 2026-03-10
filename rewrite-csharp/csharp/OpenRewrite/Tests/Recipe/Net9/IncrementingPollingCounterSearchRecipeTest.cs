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
using OpenRewrite.Recipes.Net9;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Recipe.Net9;

public class IncrementingPollingCounterSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsIncrementingPollingCounterConstructor()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new IncrementingPollingCounterSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Diagnostics.Tracing;
                class Test : EventSource
                {
                    void M()
                    {
                        var counter = new IncrementingPollingCounter("test", this, () => 1.0);
                    }
                }
                """,
                """
                using System.Diagnostics.Tracing;
                class Test : EventSource
                {
                    void M()
                    {
                        var counter = /*~~(IncrementingPollingCounter initial callback is now asynchronous in .NET 9. Code that depends on the callback being invoked synchronously during construction may break.)~~>*/new IncrementingPollingCounter("test", this, () => 1.0);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForOtherCounters()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new IncrementingPollingCounterSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Diagnostics.Tracing;
                class Test : EventSource
                {
                    void M()
                    {
                        var counter = new PollingCounter("test", this, () => 1.0);
                    }
                }
                """
            )
        );
    }
}
