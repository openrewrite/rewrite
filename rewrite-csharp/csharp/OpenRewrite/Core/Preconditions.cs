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
    /// Wraps a visitor with a recipe-based precondition check. The recipe's visitor
    /// is used as the precondition gate.
    /// </summary>
    public static ITreeVisitor<ExecutionContext> Check(
        Recipe check,
        ITreeVisitor<ExecutionContext> visitor)
    {
        if (check is IScanningRecipe)
        {
            throw new ArgumentException("ScanningRecipe is not supported as a check");
        }
        return new RecipeCheck(check, visitor);
    }

    /// <summary>
    /// Negates a precondition visitor. Returns a search result when the inner visitor
    /// does NOT match (i.e., does not modify the tree).
    /// </summary>
    public static ITreeVisitor<ExecutionContext> Not(
        ITreeVisitor<ExecutionContext> visitor)
    {
        return new NotVisitor(visitor);
    }

    /// <summary>
    /// Combines multiple precondition visitors with AND semantics. All visitors must
    /// match (modify the tree) for the combined precondition to match.
    /// </summary>
    public static ITreeVisitor<ExecutionContext> And(
        params ITreeVisitor<ExecutionContext>[] visitors)
    {
        return new AndVisitor(visitors);
    }

    public static ITreeVisitor<ExecutionContext> And(this ITreeVisitor<ExecutionContext> first, params ITreeVisitor<ExecutionContext>[] others)
    {
        return And(others.Prepend(first).ToArray());
    }

    public static ITreeVisitor<ExecutionContext> And(
        params Recipe[] recipes)
    {
        return new AndVisitor(recipes.Select(x => x.GetVisitor()).ToArray());
    }
}
