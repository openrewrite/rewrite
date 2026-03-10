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

public class ZipArchiveCompressionLevelSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsCreateEntryWithCompressionLevel()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ZipArchiveCompressionLevelSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.IO.Compression;
                class Test
                {
                    void M(ZipArchive archive)
                    {
                        archive.CreateEntry("file.txt", CompressionLevel.Fastest);
                    }
                }
                """,
                """
                using System.IO.Compression;
                class Test
                {
                    void M(ZipArchive archive)
                    {
                        /*~~(In .NET 9, ZipArchive.CreateEntry with CompressionLevel now sets general-purpose bit flags in the ZIP central directory header. This may affect interoperability with other ZIP tools.)~~>*/archive.CreateEntry("file.txt", CompressionLevel.Fastest);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForCreateEntryWithoutCompressionLevel()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ZipArchiveCompressionLevelSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.IO.Compression;
                class Test
                {
                    void M(ZipArchive archive)
                    {
                        archive.CreateEntry("file.txt");
                    }
                }
                """
            )
        );
    }
}
