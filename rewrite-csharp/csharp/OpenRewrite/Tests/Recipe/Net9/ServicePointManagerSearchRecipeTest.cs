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

public class ServicePointManagerSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsServicePointManagerPropertyAccess()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ServicePointManagerSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Net;
                class Test
                {
                    void M()
                    {
                        ServicePointManager.DefaultConnectionLimit = 100;
                    }
                }
                """,
                """
                using System.Net;
                class Test
                {
                    void M()
                    {
                        /*~~(ServicePointManager is fully obsolete in .NET 9 (SYSLIB0014). Its settings don't affect SslStream or HttpClient. Configure TLS settings directly on HttpClientHandler or SslStream instead.)~~>*/ServicePointManager.DefaultConnectionLimit = 100;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForUnrelatedNetworkingCode()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ServicePointManagerSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Net.Http;
                class Test
                {
                    void M()
                    {
                        var client = new HttpClient();
                    }
                }
                """
            )
        );
    }
}
