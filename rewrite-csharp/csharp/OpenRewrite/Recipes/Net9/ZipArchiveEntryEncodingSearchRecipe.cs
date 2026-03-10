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
/// Finds access to ZipArchiveEntry.Name, FullName, or Comment properties.
/// In .NET 9, these properties now respect the UTF8 flag in ZIP entries,
/// which may cause names/comments to be decoded differently.
/// </summary>
class ZipArchiveEntryEncodingSearchRecipe : Recipe
{
    private const string ZipArchiveEntryFqn = "System.IO.Compression.ZipArchiveEntry";

    public override string DisplayName => "Find `ZipArchiveEntry` name/comment UTF-8 encoding change";

    public override string Description =>
        "Finds access to `ZipArchiveEntry.Name`, `FullName`, or `Comment` properties. " +
        "In .NET 9, these now respect the UTF-8 flag in ZIP entries, which may change decoded values.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "compression" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        private const string TodoMessage =
            "ZipArchiveEntry now respects the UTF-8 flag for Name, FullName, and Comment in .NET 9. " +
            "Entry names and comments may be decoded differently if the ZIP uses non-UTF-8 encoding.";

        private static readonly HashSet<string> AffectedProperties =
        [
            "Name",
            "FullName",
            "Comment"
        ];

        public override J VisitFieldAccess(FieldAccess fa, Core.ExecutionContext ctx)
        {
            fa = (FieldAccess)base.VisitFieldAccess(fa, ctx);

            if (!AffectedProperties.Contains(fa.Name.Element.SimpleName))
                return fa;

            if (fa.Target is Identifier id &&
                id.Type is JavaType.Class cls &&
                cls.FullyQualifiedName == ZipArchiveEntryFqn)
            {
                return AddWarnMarker(fa, TodoMessage);
            }

            // Also check via expression type for chained access
            var targetFqn = GetExpressionTypeFqn(fa.Target);
            if (targetFqn == ZipArchiveEntryFqn)
            {
                return AddWarnMarker(fa, TodoMessage);
            }

            return fa;
        }
    }
}
