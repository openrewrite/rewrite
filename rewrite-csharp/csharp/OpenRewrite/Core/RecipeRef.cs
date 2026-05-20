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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Core;

/// <summary>
/// Recipe-identity placeholder for use as a precondition.
///
/// Captures a Java recipe class name + options without instantiating the
/// recipe or firing an RPC. The framework introspects a
/// <c>Preconditions.Check(RecipeRef, editor)</c> wrapper at PrepareRecipe
/// time and emits the recipe identity directly in
/// <c>PrepareRecipeResponse.editPreconditions</c>. The Java host's
/// <c>PreparedRecipeCache.instantiateVisitor</c> constructs the recipe via
/// Jackson and uses its visitor — no extra RPC round-trip needed at
/// editor() construction time.
///
/// This avoids requiring the recipe author to do an RPC at <c>GetVisitor()</c>
/// construction time, which would otherwise block in-process unit tests
/// that don't have an active RPC connection.
///
/// Implements <see cref="ITreeVisitor{ExecutionContext}"/> so the existing
/// <c>Preconditions.Check(ITreeVisitor, ITreeVisitor)</c> overload accepts
/// it directly. The in-process <c>Visit</c> short-circuits to
/// "always matches" by returning a marked tree, so unit tests exercising
/// a recipe without an active RPC connection still run the wrapped editor.
///
/// Helpers in <see cref="OpenRewrite.Java.Search.Preconditions"/>
/// (<c>UsesMethod</c>, <c>UsesType</c>, <c>HasSourcePath</c>,
/// <c>FindMethods</c>, <c>FindTypes</c>) return <see cref="RecipeRef"/> instances.
/// </summary>
public class RecipeRef : TreeVisitor<Tree, ExecutionContext>
{
    public string RecipeName { get; }
    public IReadOnlyDictionary<string, object?> Options { get; }

    /// <summary>
    /// Optional native visitor for in-process gate evaluation. Helpers
    /// like <c>UsesType</c> / <c>UsesMethod</c> populate this so unit
    /// tests without an active RPC connection still see real filtering
    /// instead of unconditionally short-circuiting to "always matches".
    /// </summary>
    public ITreeVisitor<ExecutionContext>? LocalVisitor { get; }

    public RecipeRef(
        string recipeName,
        IReadOnlyDictionary<string, object?>? options = null,
        ITreeVisitor<ExecutionContext>? localVisitor = null)
    {
        RecipeName = recipeName;
        Options = options ?? new Dictionary<string, object?>();
        LocalVisitor = localVisitor;
    }

    /// <summary>
    /// In-process fallback. When a <see cref="LocalVisitor"/> was
    /// provided, delegate to it for real filtering; otherwise return
    /// a marked tree so the wrapping <see cref="Check"/> takes the
    /// "matches" branch and runs the wrapped editor (the host
    /// evaluates the gate over the wire when an RPC connection is
    /// available).
    /// </summary>
    public override Tree? Visit(Tree? tree, ExecutionContext ctx)
    {
        if (tree == null) return null;
        if (LocalVisitor != null)
        {
            return LocalVisitor.Visit(tree, ctx);
        }
        return SearchResult.Found(tree);
    }
}
