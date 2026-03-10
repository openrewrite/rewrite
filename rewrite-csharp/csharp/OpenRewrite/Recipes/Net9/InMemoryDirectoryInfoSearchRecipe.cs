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
/// Finds new InMemoryDirectoryInfo() constructor calls. In .NET 9, rootDir is prepended
/// to file paths that don't start with the rootDir, which may change matching behavior.
/// </summary>
class InMemoryDirectoryInfoSearchRecipe : Recipe
{
    private const string InMemoryDirectoryInfoFqn = "Microsoft.Extensions.FileSystemGlobbing.InMemoryDirectoryInfo";

    public override string DisplayName => "Find `InMemoryDirectoryInfo` rootDir prepend change";

    public override string Description =>
        "Finds `new InMemoryDirectoryInfo()` constructor calls. In .NET 9, `rootDir` is prepended " +
        "to file paths that don't start with the `rootDir`, which may change file matching behavior.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "globbing" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        private const string TodoMessage =
            "InMemoryDirectoryInfo now prepends rootDir to file paths in .NET 9. " +
            "File paths that don't start with rootDir will be prefixed, which may change matching behavior.";

        public override J VisitNewClass(NewClass newClass, Core.ExecutionContext ctx)
        {
            newClass = (NewClass)base.VisitNewClass(newClass, ctx);

            var isMatch = newClass.ConstructorType?.DeclaringType is JavaType.Class cls &&
                          cls.FullyQualifiedName == InMemoryDirectoryInfoFqn;

            // Name-based fallback since Microsoft.Extensions.FileSystemGlobbing may not be in reference assemblies
            if (!isMatch && newClass.ConstructorType == null &&
                newClass.Clazz is Identifier clazzId &&
                clazzId.SimpleName == "InMemoryDirectoryInfo")
            {
                isMatch = true;
            }

            if (isMatch)
            {
                return AddWarnMarker(newClass, TodoMessage);
            }

            return newClass;
        }
    }
}
