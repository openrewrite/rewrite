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
using OpenRewrite.CSharp;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.CSharp;

public class MSBuildProjectAttestationTests
{
    [Fact]
    public void FreshContextHasNoStaleAttestations()
    {
        var ctx = new ExecutionContext();
        Assert.False(MSBuildProjectHelper.IsAttestationStale(ctx, "src/A/A.csproj"));
    }

    [Fact]
    public void MarkStaleMakesIsStaleReturnTrue()
    {
        var ctx = new ExecutionContext();
        MSBuildProjectHelper.MarkAttestationStale(ctx, "src/A/A.csproj");
        Assert.True(MSBuildProjectHelper.IsAttestationStale(ctx, "src/A/A.csproj"));
    }

    [Fact]
    public void StaleFlagIsScopedPerSourcePath()
    {
        var ctx = new ExecutionContext();
        MSBuildProjectHelper.MarkAttestationStale(ctx, "src/A/A.csproj");
        Assert.True(MSBuildProjectHelper.IsAttestationStale(ctx, "src/A/A.csproj"));
        Assert.False(MSBuildProjectHelper.IsAttestationStale(ctx, "src/B/B.csproj"));
    }

    [Fact]
    public void StaleFlagComparisonIsCaseInsensitive()
    {
        // Source paths come through the LST verbatim, and Windows-origin paths can
        // differ in case from the mutation site to the EnsureCsprojAttestation step.
        var ctx = new ExecutionContext();
        MSBuildProjectHelper.MarkAttestationStale(ctx, "src/A/A.csproj");
        Assert.True(MSBuildProjectHelper.IsAttestationStale(ctx, "SRC/a/A.CSPROJ"));
    }

    [Fact]
    public void MarkStaleIsIdempotent()
    {
        var ctx = new ExecutionContext();
        MSBuildProjectHelper.MarkAttestationStale(ctx, "src/A/A.csproj");
        MSBuildProjectHelper.MarkAttestationStale(ctx, "src/A/A.csproj");
        Assert.True(MSBuildProjectHelper.IsAttestationStale(ctx, "src/A/A.csproj"));
    }

    [Fact]
    public void StaleFlagsAreIsolatedPerExecutionContext()
    {
        var ctx1 = new ExecutionContext();
        var ctx2 = new ExecutionContext();
        MSBuildProjectHelper.MarkAttestationStale(ctx1, "src/A/A.csproj");
        Assert.False(MSBuildProjectHelper.IsAttestationStale(ctx2, "src/A/A.csproj"));
    }
}
