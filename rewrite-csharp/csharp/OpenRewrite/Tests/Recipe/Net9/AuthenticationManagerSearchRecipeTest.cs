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

public class AuthenticationManagerSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsAuthenticationManagerPropertyAccess()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new AuthenticationManagerSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Net;
                class Test
                {
                    void M()
                    {
                        var modules = AuthenticationManager.RegisteredModules;
                    }
                }
                """,
                """
                using System.Net;
                class Test
                {
                    void M()
                    {
                        var modules = /*~~(AuthenticationManager is not supported in .NET 9 (SYSLIB0009). Methods will no-op or throw PlatformNotSupportedException. Use HttpClientHandler or CredentialCache for authentication.)~~>*/AuthenticationManager.RegisteredModules;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForUnrelatedClass()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new AuthenticationManagerSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Net;
                class Test
                {
                    void M()
                    {
                        var credential = new NetworkCredential("user", "pass");
                    }
                }
                """
            )
        );
    }
}
