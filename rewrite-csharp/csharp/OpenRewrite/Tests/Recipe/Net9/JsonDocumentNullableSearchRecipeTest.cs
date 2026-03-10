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

public class JsonDocumentNullableSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsJsonSerializerDeserializeCall()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new JsonDocumentNullableSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Text.Json;
                class Test
                {
                    void M(string json)
                    {
                        var obj = JsonSerializer.Deserialize<object>(json);
                    }
                }
                """,
                """
                using System.Text.Json;
                class Test
                {
                    void M(string json)
                    {
                        var obj = /*~~(In .NET 9, nullable JsonDocument properties now deserialize to a JsonDocument with RootElement.ValueKind == JsonValueKind.Null instead of null. Null checks on deserialized JsonDocument properties may need updating.)~~>*/JsonSerializer.Deserialize<object>(json);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForOtherStaticMethods()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new JsonDocumentNullableSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Text.Json;
                class Test
                {
                    void M(string json)
                    {
                        var doc = JsonDocument.Parse(json);
                    }
                }
                """
            )
        );
    }
}
