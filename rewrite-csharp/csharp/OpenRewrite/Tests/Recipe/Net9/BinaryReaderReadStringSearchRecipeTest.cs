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

public class BinaryReaderReadStringSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsReadStringCall()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new BinaryReaderReadStringSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.IO;
                class Test
                {
                    void M(BinaryReader reader)
                    {
                        var text = reader.ReadString();
                    }
                }
                """,
                """
                using System.IO;
                class Test
                {
                    void M(BinaryReader reader)
                    {
                        var text = /*~~(BinaryReader.ReadString now returns \uFFFD for malformed UTF-8 sequences in .NET 9. Verify your code handles the replacement character correctly.)~~>*/reader.ReadString();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForReadInt32()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new BinaryReaderReadStringSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.IO;
                class Test
                {
                    void M(BinaryReader reader)
                    {
                        var value = reader.ReadInt32();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForUnrelatedReadString()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new BinaryReaderReadStringSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                class MyReader
                {
                    public string ReadString() => "hello";
                }
                class Test
                {
                    void M(MyReader reader)
                    {
                        var text = reader.ReadString();
                    }
                }
                """
            )
        );
    }
}
