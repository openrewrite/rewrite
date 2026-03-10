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

public class EnumConverterSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsEnumConverterConstructor()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new EnumConverterSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.ComponentModel;
                class Test
                {
                    void M()
                    {
                        var converter = new EnumConverter(typeof(DayOfWeek));
                    }
                }
                """,
                """
                using System.ComponentModel;
                class Test
                {
                    void M()
                    {
                        var converter = /*~~(EnumConverter now validates that the type is an enum in .NET 9. Passing a non-enum type will throw ArgumentException.)~~>*/new EnumConverter(typeof(DayOfWeek));
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForOtherConverters()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new EnumConverterSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.ComponentModel;
                class Test
                {
                    void M()
                    {
                        var converter = new BooleanConverter();
                    }
                }
                """
            )
        );
    }
}
