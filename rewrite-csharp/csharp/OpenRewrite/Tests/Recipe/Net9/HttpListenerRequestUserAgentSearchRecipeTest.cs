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

public class HttpListenerRequestUserAgentSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsUserAgentAccess()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new HttpListenerRequestUserAgentSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Net;
                class Test
                {
                    void M(HttpListenerRequest request)
                    {
                        var ua = request.UserAgent;
                    }
                }
                """,
                """
                using System.Net;
                class Test
                {
                    void M(HttpListenerRequest request)
                    {
                        var ua = /*~~(HttpListenerRequest.UserAgent is now nullable (string?) in .NET 9. Add null checks before using this property.)~~>*/request.UserAgent;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForOtherProperties()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new HttpListenerRequestUserAgentSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Net;
                class Test
                {
                    void M(HttpListenerRequest request)
                    {
                        var url = request.Url;
                    }
                }
                """
            )
        );
    }
}
