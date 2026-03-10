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

public class BinaryFormatterSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsNewBinaryFormatter()
    {
        RewriteRun(
            spec => spec.SetRecipe(new BinaryFormatterSearchRecipe()),
            CSharp(
                """
                class Test
                {
                    void M()
                    {
                        var formatter = new BinaryFormatter();
                    }
                }
                """,
                """
                class Test
                {
                    void M()
                    {
                        var formatter = /*~~(BinaryFormatter always throws NotSupportedException in .NET 9. Migrate to System.Text.Json, XmlSerializer, or DataContractSerializer.)~~>*/new BinaryFormatter();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindsBinaryFormatterAsFieldType()
    {
        RewriteRun(
            spec => spec.SetRecipe(new BinaryFormatterSearchRecipe()),
            CSharp(
                """
                class Test
                {
                    private BinaryFormatter _formatter;
                }
                """,
                """
                class Test
                {
                    private /*~~(BinaryFormatter always throws NotSupportedException in .NET 9. Migrate to System.Text.Json, XmlSerializer, or DataContractSerializer.)~~>*/BinaryFormatter _formatter;
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForUnrelatedClass()
    {
        RewriteRun(
            spec => spec.SetRecipe(new BinaryFormatterSearchRecipe()),
            CSharp(
                """
                class Test
                {
                    void M()
                    {
                        var list = new List<string>();
                    }
                }
                """
            )
        );
    }
}
