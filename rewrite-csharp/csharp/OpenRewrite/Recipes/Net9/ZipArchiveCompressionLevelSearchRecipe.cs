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
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using static OpenRewrite.Java.J;
using static OpenRewrite.Recipes.Net9.Net9RecipeHelpers;

namespace OpenRewrite.Recipes.Net9;

/// <summary>
/// Finds ZipArchive.CreateEntry() and ZipFileExtensions.CreateEntryFromFile() calls
/// with a CompressionLevel parameter. In .NET 9, the CompressionLevel value now sets
/// general-purpose bit flags in the ZIP central directory header, which may affect
/// ZIP interoperability with other tools.
/// </summary>
class ZipArchiveCompressionLevelSearchRecipe : Recipe
{
    public override string DisplayName => "Find `ZipArchive.CreateEntry` with `CompressionLevel` (bit flag change)";

    public override string Description =>
        "Finds `ZipArchive.CreateEntry()` and `ZipFileExtensions.CreateEntryFromFile()` calls " +
        "with a `CompressionLevel` parameter. In .NET 9, the `CompressionLevel` value now sets " +
        "general-purpose bit flags in the ZIP central directory header, which may affect interoperability.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "compression" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        private const string TodoMessage =
            "In .NET 9, ZipArchive.CreateEntry with CompressionLevel now sets general-purpose bit flags " +
            "in the ZIP central directory header. This may affect interoperability with other ZIP tools.";

        private static readonly HashSet<string> AffectedTypes =
        [
            "System.IO.Compression.ZipArchive",
            "System.IO.Compression.ZipFileExtensions"
        ];

        private static readonly HashSet<string> AffectedMethods =
        [
            "CreateEntry",
            "CreateEntryFromFile"
        ];

        public override J VisitMethodInvocation(MethodInvocation mi, Core.ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);

            if (!AffectedMethods.Contains(mi.Name.SimpleName))
                return mi;

            // Only flag if there's a CompressionLevel argument (2+ args for CreateEntry, 3+ for CreateEntryFromFile)
            var argCount = 0;
            foreach (var a in mi.Arguments.Elements)
                if (a.Element is not Empty) argCount++;
            if (mi.Name.SimpleName == "CreateEntry" && argCount < 2)
                return mi;
            if (mi.Name.SimpleName == "CreateEntryFromFile" && argCount < 3)
                return mi;

            var isMatch = mi.MethodType?.DeclaringType is JavaType.Class cls &&
                          AffectedTypes.Contains(cls.FullyQualifiedName);

            if (!isMatch && mi.MethodType == null && mi.Select != null)
            {
                var targetFqn = GetExpressionTypeFqn(mi.Select.Element);
                isMatch = targetFqn != null && AffectedTypes.Contains(targetFqn);
            }

            if (isMatch)
            {
                return AddWarnMarker(mi, TodoMessage);
            }

            return mi;
        }
    }
}
