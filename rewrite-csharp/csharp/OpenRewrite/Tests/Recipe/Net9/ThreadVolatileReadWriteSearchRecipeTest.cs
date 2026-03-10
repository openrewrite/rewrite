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

public class ThreadVolatileReadWriteSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsVolatileRead()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ThreadVolatileReadWriteSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Threading;
                class Test
                {
                    private int _value;
                    int M()
                    {
                        return Thread.VolatileRead(ref _value);
                    }
                }
                """,
                """
                using System.Threading;
                class Test
                {
                    private int _value;
                    int M()
                    {
                        return /*~~(Thread.VolatileRead is obsolete in .NET 9 (SYSLIB0054). Use Volatile.Read instead.)~~>*/Thread.VolatileRead(ref _value);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindsVolatileWrite()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ThreadVolatileReadWriteSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Threading;
                class Test
                {
                    private int _value;
                    void M()
                    {
                        Thread.VolatileWrite(ref _value, 42);
                    }
                }
                """,
                """
                using System.Threading;
                class Test
                {
                    private int _value;
                    void M()
                    {
                        /*~~(Thread.VolatileWrite is obsolete in .NET 9 (SYSLIB0054). Use Volatile.Write instead.)~~>*/Thread.VolatileWrite(ref _value, 42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForThreadSleep()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ThreadVolatileReadWriteSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Threading;
                class Test
                {
                    void M()
                    {
                        Thread.Sleep(100);
                    }
                }
                """
            )
        );
    }
}
