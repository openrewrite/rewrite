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

public class RuntimeHelpersGetSubArraySearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsGetSubArrayCall()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RuntimeHelpersGetSubArraySearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Runtime.CompilerServices;
                class Test
                {
                    void M(int[] arr)
                    {
                        var sub = RuntimeHelpers.GetSubArray(arr, 1..3);
                    }
                }
                """,
                """
                using System.Runtime.CompilerServices;
                class Test
                {
                    void M(int[] arr)
                    {
                        var sub = /*~~(RuntimeHelpers.GetSubArray may return a different array type in .NET 9. Code that checks the runtime type of the returned array or casts it may break.)~~>*/RuntimeHelpers.GetSubArray(arr, 1..3);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForOtherRuntimeHelpersMethods()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RuntimeHelpersGetSubArraySearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Runtime.CompilerServices;
                class Test
                {
                    void M()
                    {
                        RuntimeHelpers.RunClassConstructor(typeof(string).TypeHandle);
                    }
                }
                """
            )
        );
    }
}
