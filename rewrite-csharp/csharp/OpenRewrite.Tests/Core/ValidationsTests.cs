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
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// <see cref="Validations.None"/> is for input the parser can't round-trip, which says nothing about what the recipe
/// does, so it leaves the checks of the recipe on.
/// </summary>
public class ValidationsTests
{
    [Fact]
    public void AllTurnsEveryCheckOn()
    {
        var all = Validations.All;
        Assert.True(all.WhitespaceInSpaces && all.PrintEqualsInput && all.PrintIdempotence &&
                    all.FormattingReconciliation && all.ReportingMarkersOnly && all.ExecutionContextUnchanged &&
                    all.NoEditsInScanningPhase && all.LightweightAccumulators && all.ParserShapedTrees &&
                    all.MinimalFormatting);
    }

    [Fact]
    public void NoneLeavesTheChecksOfWhatTheRecipeDoesOn()
    {
        var none = Validations.None;
        Assert.False(none.WhitespaceInSpaces || none.PrintEqualsInput || none.PrintIdempotence ||
                     none.FormattingReconciliation);
        Assert.True(none.ReportingMarkersOnly && none.ExecutionContextUnchanged && none.NoEditsInScanningPhase &&
                    none.LightweightAccumulators && none.ParserShapedTrees && none.MinimalFormatting);
    }
}
