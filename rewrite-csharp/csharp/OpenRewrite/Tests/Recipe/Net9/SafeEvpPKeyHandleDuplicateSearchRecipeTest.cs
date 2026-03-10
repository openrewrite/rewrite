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

public class SafeEvpPKeyHandleDuplicateSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsDuplicateHandleCall()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new SafeEvpPKeyHandleDuplicateSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Security.Cryptography;
                class Test
                {
                    void M(SafeEvpPKeyHandle handle)
                    {
                        var dup = handle.DuplicateHandle();
                    }
                }
                """,
                """
                using System.Security.Cryptography;
                class Test
                {
                    void M(SafeEvpPKeyHandle handle)
                    {
                        var dup = /*~~(SafeEvpPKeyHandle.DuplicateHandle now up-refs the handle in .NET 9 instead of deep copying. The original and duplicate now share the same underlying key. Disposing one may affect the other.)~~>*/handle.DuplicateHandle();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForOtherMethods()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new SafeEvpPKeyHandleDuplicateSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Security.Cryptography;
                class Test
                {
                    void M()
                    {
                        using var rsa = RSA.Create();
                    }
                }
                """
            )
        );
    }
}
