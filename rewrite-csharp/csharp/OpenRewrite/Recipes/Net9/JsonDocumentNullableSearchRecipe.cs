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
/// Finds JsonSerializer.Deserialize() calls that may return JsonDocument or types
/// containing JsonDocument properties. In .NET 9, nullable JsonDocument properties
/// now deserialize to a JsonDocument with RootElement.ValueKind == JsonValueKind.Null
/// instead of being null.
/// </summary>
class JsonDocumentNullableSearchRecipe : Recipe
{
    private const string JsonSerializerFqn = "System.Text.Json.JsonSerializer";

    public override string DisplayName => "Find `JsonSerializer.Deserialize` nullable `JsonDocument` change";

    public override string Description =>
        "Finds `JsonSerializer.Deserialize()` calls. In .NET 9, nullable `JsonDocument` properties " +
        "now deserialize to a `JsonDocument` with `RootElement.ValueKind == JsonValueKind.Null` " +
        "instead of being `null`.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "serialization" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        private const string TodoMessage =
            "In .NET 9, nullable JsonDocument properties now deserialize to a JsonDocument with " +
            "RootElement.ValueKind == JsonValueKind.Null instead of null. " +
            "Null checks on deserialized JsonDocument properties may need updating.";

        public override J VisitMethodInvocation(MethodInvocation mi, Core.ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);

            if (mi.Name.SimpleName != "Deserialize")
                return mi;

            var isMatch = mi.MethodType?.DeclaringType is JavaType.Class cls &&
                          cls.FullyQualifiedName == JsonSerializerFqn;

            if (!isMatch && mi.MethodType == null && mi.Select != null)
            {
                isMatch = GetExpressionTypeFqn(mi.Select.Element) == JsonSerializerFqn;
            }

            // Also match static calls like JsonSerializer.Deserialize(...)
            if (!isMatch && mi.Select?.Element is Identifier id &&
                id.SimpleName == "JsonSerializer")
            {
                isMatch = true;
            }

            if (isMatch)
            {
                return AddWarnMarker(mi, TodoMessage);
            }

            return mi;
        }
    }
}
