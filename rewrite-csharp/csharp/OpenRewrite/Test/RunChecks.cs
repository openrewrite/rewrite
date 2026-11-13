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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Test;

/// <summary>
/// The checks <see cref="RewriteTest"/> makes while the scheduler runs a test's recipe, with the execution context
/// that every cycle of the test shares.
/// </summary>
internal sealed class RunChecks : IRunObserver
{
    private readonly Validations _validations;
    private readonly AccumulatorContents? _accumulators;

    public RunChecks(Validations validations)
    {
        _validations = validations;
        _accumulators = validations.LightweightAccumulators ? new AccumulatorContents() : null;
        Ctx = validations.ExecutionContextUnchanged ? new UnchangedExecutionContext(this) : new ExecutionContext();
    }

    public ExecutionContext Ctx { get; }

    /// <summary>The recipe the scheduler last called into.</summary>
    public Recipe? Recipe { get; private set; }

    /// <summary>Runs one cycle of the recipe on the sources.</summary>
    public List<Result> Run(Recipe recipe, List<SourceFile> sources)
    {
        var results = RecipeScheduler.Run(recipe, sources, Ctx, this);
        (Ctx as UnchangedExecutionContext)?.AssertUnchanged();
        return results;
    }

    public void Running(Recipe recipe) => Recipe = recipe;

    public void Scanned(Recipe recipe, SourceFile source, Tree? scanned)
    {
        if (_validations.NoEditsInScanningPhase && !ReferenceEquals(source, scanned))
        {
            Assert.Fail(
                "Record what the scanner finds in the accumulator, and make the edits in GetVisitor(acc).\n" +
                $"{recipe.Name}'s scanner {(scanned == null ? "deleted" : "changed")} {source.SourcePath}, and the " +
                "scanning phase discards edits. A scanner that reuses an editing visitor opts out with " +
                "spec.SetValidations(new Validations { NoEditsInScanningPhase = false }).");
        }
    }

    public void ScanningEnded(IReadOnlyDictionary<IScanningRecipe, object> accumulators) =>
        _accumulators?.Check(accumulators, "after scanning");

    public void EditingEnded(IReadOnlyDictionary<IScanningRecipe, object> accumulators) =>
        _accumulators?.Check(accumulators, "after editing");

    /// <summary>Fails for what the accumulators kept in the cycles so far that is too large to keep.</summary>
    public void AssertLightweightAccumulators() => _accumulators?.AssertNoneFound();
}
