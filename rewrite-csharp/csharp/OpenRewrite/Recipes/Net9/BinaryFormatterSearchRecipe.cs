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
/// Finds usages of BinaryFormatter which always throws in .NET 9.
/// The in-box BinaryFormatter implementation was removed and all methods throw
/// NotSupportedException at runtime.
/// </summary>
class BinaryFormatterSearchRecipe : Recipe
{
    private const string BinaryFormatterFqn =
        "System.Runtime.Serialization.Formatters.Binary.BinaryFormatter";

    private const string BinaryFormatterSimpleName = "BinaryFormatter";

    public override string DisplayName => "Find `BinaryFormatter` usage (removed in .NET 9)";

    public override string Description =>
        "Finds usages of `BinaryFormatter` which always throws `NotSupportedException` in .NET 9. " +
        "Migrate to a different serializer such as `System.Text.Json`, `XmlSerializer`, or `DataContractSerializer`.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "serialization" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        private const string TodoMessage =
            "BinaryFormatter always throws NotSupportedException in .NET 9. " +
            "Migrate to System.Text.Json, XmlSerializer, or DataContractSerializer.";

        public override J VisitNewClass(NewClass newClass, Core.ExecutionContext ctx)
        {
            newClass = (NewClass)base.VisitNewClass(newClass, ctx);

            var isMatch = newClass.ConstructorType?.DeclaringType is JavaType.Class cls &&
                          cls.FullyQualifiedName == BinaryFormatterFqn;

            // Fallback: match by name when type info unavailable (BinaryFormatter removed from ref assemblies)
            if (!isMatch && newClass.ConstructorType == null &&
                newClass.Clazz is Identifier clazzId &&
                clazzId.SimpleName == BinaryFormatterSimpleName)
            {
                isMatch = true;
            }

            if (isMatch)
            {
                return AddWarnMarker(newClass, TodoMessage);
            }

            return newClass;
        }

        public override J VisitIdentifier(Identifier ident, Core.ExecutionContext ctx)
        {
            ident = (Identifier)base.VisitIdentifier(ident, ctx);

            // Skip identifiers inside NewClass (handled by VisitNewClass)
            if (Cursor.Parent?.Value is NewClass)
            {
                return ident;
            }

            var isMatch = ident.Type is JavaType.Class cls &&
                          cls.FullyQualifiedName == BinaryFormatterFqn;

            // Fallback: match by simple name when type info unavailable
            if (!isMatch && ident.Type == null && ident.SimpleName == BinaryFormatterSimpleName)
            {
                isMatch = true;
            }

            if (isMatch)
            {
                return AddWarnMarker(ident, TodoMessage);
            }

            return ident;
        }
    }
}
