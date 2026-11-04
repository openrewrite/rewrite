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

namespace OpenRewrite.Java.Search;

/// <summary>
/// Search-based precondition helpers that name a Java recipe to evaluate
/// the gate over the wire.
///
/// Each helper returns a <see cref="RecipeRef"/> placeholder that carries
/// the Java recipe class name + options. The framework introspects a
/// <c>Preconditions.Check(RecipeRef, editor)</c> wrapper at PrepareRecipe
/// time and emits the recipe identity directly in
/// <c>editPreconditions</c>; the Java host's <c>PreparedRecipeCache.instantiateVisitor</c>
/// constructs the recipe and uses its visitor — no extra RPC round-trip
/// needed at <c>GetVisitor()</c> construction time, so unit tests can call
/// <c>recipe.GetVisitor()</c> without an active RPC connection to a Java host.
/// </summary>
public static class Preconditions
{
    /// <summary>
    /// Match source files by path glob. Delegates to
    /// <c>org.openrewrite.FindSourceFiles</c> on the Java host. Bundles
    /// a native <see cref="IsSourceFile"/> visitor so unit tests
    /// without an active RPC connection still see real filtering.
    /// </summary>
    public static RecipeRef HasSourcePath(string filePattern)
    {
        return new RecipeRef(
            "org.openrewrite.FindSourceFiles",
            new Dictionary<string, object?>
            {
                ["filePattern"] = filePattern
            },
            new IsSourceFile(filePattern));
    }

    /// <summary>
    /// Match files using a specific type. Delegates to
    /// <c>org.openrewrite.java.search.HasType</c> on the Java host.
    /// Bundles a native <see cref="UsesType"/> visitor so unit tests
    /// without an active RPC connection still see real filtering.
    /// </summary>
    public static RecipeRef UsesType(string fullyQualifiedTypeName, bool checkAssignability = false)
    {
        return new RecipeRef(
            "org.openrewrite.java.search.HasType",
            new Dictionary<string, object?>
            {
                ["fullyQualifiedTypeName"] = fullyQualifiedTypeName,
                ["checkAssignability"] = checkAssignability
            },
            new UsesType(fullyQualifiedTypeName));
    }

    /// <summary>
    /// Match files using a specific method. <paramref name="methodPattern"/>
    /// follows the OpenRewrite method-pattern syntax
    /// <c>&lt;receiver-type&gt; &lt;method-name&gt;(&lt;args&gt;)</c>
    /// — e.g. <c>"*..* tostring(..)"</c>. Delegates to
    /// <c>org.openrewrite.java.search.HasMethod</c> on the Java host.
    /// Bundles a native <see cref="UsesMethod"/> visitor so unit tests
    /// without an active RPC connection still see real filtering.
    /// </summary>
    public static RecipeRef UsesMethod(string methodPattern, bool matchOverrides = false)
    {
        return new RecipeRef(
            "org.openrewrite.java.search.HasMethod",
            new Dictionary<string, object?>
            {
                ["methodPattern"] = methodPattern,
                ["matchOverrides"] = matchOverrides
            },
            new UsesMethod(methodPattern, matchOverrides));
    }

    /// <summary>
    /// Find and mark methods matching a pattern. Delegates to
    /// <c>org.openrewrite.java.search.FindMethods</c> on the Java host.
    /// Bundles a native <see cref="UsesMethod"/> visitor so unit tests
    /// without an active RPC connection still see real filtering.
    /// </summary>
    public static RecipeRef FindMethods(string methodPattern, bool matchOverrides = false)
    {
        return new RecipeRef(
            "org.openrewrite.java.search.FindMethods",
            new Dictionary<string, object?>
            {
                ["methodPattern"] = methodPattern,
                ["matchOverrides"] = matchOverrides
            },
            new UsesMethod(methodPattern, matchOverrides));
    }

    /// <summary>
    /// Find and mark usages of a type. Delegates to
    /// <c>org.openrewrite.java.search.FindTypes</c> on the Java host.
    /// Bundles a native <see cref="UsesType"/> visitor so unit tests
    /// without an active RPC connection still see real filtering.
    /// </summary>
    public static RecipeRef FindTypes(string fullyQualifiedTypeName)
    {
        return new RecipeRef(
            "org.openrewrite.java.search.FindTypes",
            new Dictionary<string, object?>
            {
                ["fullyQualifiedTypeName"] = fullyQualifiedTypeName
            },
            new UsesType(fullyQualifiedTypeName));
    }
}
