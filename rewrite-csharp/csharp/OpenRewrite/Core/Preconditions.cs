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
/// Language-agnostic precondition utilities.
/// </summary>
public static class Preconditions
{
    /// <summary>
    /// Wraps a visitor with a precondition check. The inner visitor only runs
    /// on files where the precondition matches.
    /// </summary>
    public static ITreeVisitor<ExecutionContext> Check(
        ITreeVisitor<ExecutionContext> precondition,
        ITreeVisitor<ExecutionContext> visitor)
    {
        return new Check(precondition, visitor);
    }

    /// <summary>
    /// Wraps a recipe so its visitor only runs on files where the precondition matches.
    /// Useful for applying a precondition to sub-recipes within a composite recipe.
    /// </summary>
    public static Recipe Check(ITreeVisitor<ExecutionContext> precondition, Recipe recipe)
    {
        return new PreconditionedRecipe(precondition, recipe);
    }
}

/// <summary>
/// A recipe wrapper that gates execution on a precondition visitor.
/// </summary>
internal class PreconditionedRecipe : Recipe
{
    private readonly ITreeVisitor<ExecutionContext> _precondition;
    private readonly Recipe _delegate;

    public PreconditionedRecipe(ITreeVisitor<ExecutionContext> precondition, Recipe @delegate)
    {
        _precondition = precondition;
        _delegate = @delegate;
    }

    public override string DisplayName => _delegate.DisplayName;
    public override string Description => _delegate.Description;

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        return Preconditions.Check(_precondition, _delegate.GetVisitor());
    }
}
