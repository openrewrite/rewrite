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

public class ZipArchiveEntryEncodingSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsZipArchiveEntryNameAccess()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ZipArchiveEntryEncodingSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.IO.Compression;
                class Test
                {
                    void M(ZipArchiveEntry entry)
                    {
                        var name = entry.Name;
                    }
                }
                """,
                """
                using System.IO.Compression;
                class Test
                {
                    void M(ZipArchiveEntry entry)
                    {
                        var name = /*~~(ZipArchiveEntry now respects the UTF-8 flag for Name, FullName, and Comment in .NET 9. Entry names and comments may be decoded differently if the ZIP uses non-UTF-8 encoding.)~~>*/entry.Name;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindsFullNameAccess()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ZipArchiveEntryEncodingSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.IO.Compression;
                class Test
                {
                    void M(ZipArchiveEntry entry)
                    {
                        var fullName = entry.FullName;
                    }
                }
                """,
                """
                using System.IO.Compression;
                class Test
                {
                    void M(ZipArchiveEntry entry)
                    {
                        var fullName = /*~~(ZipArchiveEntry now respects the UTF-8 flag for Name, FullName, and Comment in .NET 9. Entry names and comments may be decoded differently if the ZIP uses non-UTF-8 encoding.)~~>*/entry.FullName;
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
                .SetRecipe(new ZipArchiveEntryEncodingSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.IO.Compression;
                class Test
                {
                    void M(ZipArchiveEntry entry)
                    {
                        var len = entry.Length;
                    }
                }
                """
            )
        );
    }
}
