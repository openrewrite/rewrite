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
using NuGet.Common;
using OpenRewrite.CSharp.NuGet;

namespace OpenRewrite.Tests;

public class NuGetSourceFailuresTests
{
    private const string Feed = "https://pkgs.dev.azure.com/contoso/_packaging/tools/nuget/v3/index.json";
    private const string OtherFeed = "https://pkgs.dev.azure.com/contoso/_packaging/legacy/nuget/v3/index.json";
    private const string Public = "https://api.nuget.org/v3/index.json";

    private static readonly string[] Sources = [Feed, OtherFeed, Public];

    private sealed class RestoreMessage : IRestoreLogMessage
    {
        public LogLevel Level { get; set; } = LogLevel.Warning;
        public WarningLevel WarningLevel { get; set; } = WarningLevel.Severe;
        public NuGetLogCode Code { get; set; } = NuGetLogCode.Undefined;
        public string Message { get; set; } = string.Empty;
        public string? ProjectPath { get; set; }
        public DateTimeOffset Time { get; set; } = DateTimeOffset.UnixEpoch;
        public string? FilePath { get; set; }
        public int StartLineNumber { get; set; }
        public int StartColumnNumber { get; set; }
        public int EndLineNumber { get; set; }
        public int EndColumnNumber { get; set; }
        public string? LibraryId { get; set; }
        public IReadOnlyList<string> TargetGraphs { get; set; } = [];
        public bool ShouldDisplay { get; set; } = true;
    }

    private static RestoreMessage ServiceIndexFailure(string feed, string package, string project) => new()
    {
        Level = LogLevel.Warning,
        Code = NuGetLogCode.NU1301,
        Message = $"Unable to load the service index for source {feed}.",
        LibraryId = package,
        ProjectPath = $"/repo/src/{project}/{project}.csproj",
    };

    [Fact]
    public void GroupsRepeatedServiceIndexFailuresIntoOneEndpoint()
    {
        var failures = new NuGetSourceFailures("Contoso.sln", Sources);
        foreach (var project in new[] { "Api", "Web", "Worker" })
        foreach (var package in new[] { "Serilog", "Newtonsoft.Json", "Polly" })
            Assert.True(failures.Record(ServiceIndexFailure(Feed, package, project)));

        var summary = failures.BuildSummary();

        Assert.Equal($"NuGet: 9 package source diagnostic(s) from 1 source(s) while restoring " +
                     "Contoso.sln; grouped by endpoint below", summary[0]);
        Assert.Equal($"  source {Feed}: 9 diagnostic(s), unused - every package resolved from another source",
            summary[1]);
        Assert.Equal($"    [Warning NU1301] x9: Unable to load the service index for source {Feed}.", summary[2]);
        Assert.Equal("    affected packages (3): Newtonsoft.Json, Polly, Serilog", summary[3]);
        Assert.Equal("    affected projects (3): Api.csproj, Web.csproj, Worker.csproj", summary[4]);
        Assert.Equal(5, summary.Count);
    }

    [Fact]
    public void AttributesResourceUrlsToTheConfiguredFeedTheyBelongTo()
    {
        var failures = new NuGetSourceFailures("Contoso.sln", Sources);
        failures.Record(new RestoreMessage
        {
            Code = NuGetLogCode.Undefined,
            Message = "Retrying 'https://pkgs.dev.azure.com/contoso/_packaging/tools/nuget/v3/flat2/serilog/index.json' " +
                       $"for source '{Feed}'.",
            LibraryId = "Serilog",
        });
        failures.Record(ServiceIndexFailure(Feed, "Serilog", "Api"));

        var summary = failures.BuildSummary();

        Assert.Contains($"  source {Feed}: 2 diagnostic(s)", summary[1]);
        Assert.Equal(2, summary.Count(l => l.TrimStart().StartsWith('[')));
    }

    [Fact]
    public void KeepsSeparateFeedsOnTheSameHostApart()
    {
        var failures = new NuGetSourceFailures("Contoso.sln", Sources);
        failures.Record(ServiceIndexFailure(Feed, "Serilog", "Api"));
        failures.Record(ServiceIndexFailure(OtherFeed, "Serilog", "Api"));

        var summary = failures.BuildSummary();

        Assert.StartsWith("NuGet: 2 package source diagnostic(s) from 2 source(s)", summary[0]);
        Assert.Contains(summary, l => l.StartsWith($"  source {Feed}:"));
        Assert.Contains(summary, l => l.StartsWith($"  source {OtherFeed}:"));
    }

    [Fact]
    public void ReportsAFeedAsRequiredWhenItBlockedAPackage()
    {
        var failures = new NuGetSourceFailures("Contoso.sln", Sources);
        failures.Record(ServiceIndexFailure(Feed, "Contoso.Internal.Core", "Api"));
        failures.Record(new RestoreMessage
        {
            Level = LogLevel.Error,
            Code = NuGetLogCode.NU1101,
            Message = "Unable to find package Contoso.Internal.Core. No packages exist with this id in " +
                       $"source(s): {Feed}",
            LibraryId = "Contoso.Internal.Core",
        });

        var summary = failures.BuildSummary();

        Assert.Contains("REQUIRED - blocked 1 package(s): Contoso.Internal.Core", summary[1]);
    }

    [Fact]
    public void ReportsAFeedAsNotRequiredWhenSomethingElseFailedToResolve()
    {
        var failures = new NuGetSourceFailures("Contoso.sln", Sources);
        failures.Record(ServiceIndexFailure(Feed, "Serilog", "Api"));
        failures.MarkUnresolved("Contoso.Internal.Core");

        var summary = failures.BuildSummary();

        Assert.Contains("not required - none of the 1 unresolved package(s) were looked up here", summary[1]);
    }

    [Fact]
    public void FlagsAFeedAsPossiblyRequiredWhenItNamedNoPackageAtAll()
    {
        var failures = new NuGetSourceFailures("Contoso.sln", Sources);
        failures.Record(new RestoreMessage
        {
            Code = NuGetLogCode.NU1801,
            Message = $"Unable to load the service index for source {Feed}.",
            ProjectPath = "/repo/src/Api/Api.csproj",
        });
        failures.MarkUnresolved("Contoso.Internal.Core");

        var summary = failures.BuildSummary();

        Assert.Contains("possibly required - 1 package(s) resolved from no source: Contoso.Internal.Core",
            summary[1]);
    }

    [Fact]
    public void LeavesInformationalHttpTraceAlone()
    {
        var failures = new NuGetSourceFailures("Contoso.sln", Sources);

        Assert.False(failures.Record(new RestoreMessage
        {
            Level = LogLevel.Information,
            Message = $"  OK {Public} 108ms",
        }));
        Assert.Empty(failures.BuildSummary());
    }

    [Fact]
    public void LeavesMessagesThatNameNoSourceAlone()
    {
        var failures = new NuGetSourceFailures("Contoso.sln", Sources);

        Assert.False(failures.Record(new RestoreMessage
        {
            Code = NuGetLogCode.NU1603,
            Message = "Serilog 2.0.0 was not found. An approximate best match of Serilog 2.1.0 was resolved.",
            LibraryId = "Serilog",
        }));
        Assert.Empty(failures.BuildSummary());
    }

    [Fact]
    public void GroupsFailuresRaisedOutsideNuGetsLogger()
    {
        var failures = new NuGetSourceFailures("packages.config", Sources);

        Assert.True(failures.RecordSourceFailure(Feed, "Serilog", "The remote name could not be resolved"));
        Assert.True(failures.RecordSourceFailure(Feed, "Polly", "The remote name could not be resolved"));
        Assert.False(failures.RecordSourceFailure(string.Empty, "Polly", "boom"));

        var summary = failures.BuildSummary();

        Assert.Contains($"  source {Feed}: 2 diagnostic(s)", summary[1]);
        Assert.Equal(1, summary.Count(l => l.TrimStart().StartsWith('[')));
        Assert.Contains("    affected packages (2): Polly, Serilog", summary);
    }

    [Fact]
    public void CollapseElidesUrlsPathsAndThePackageUnderResolution()
    {
        var collapsed = NuGetSourceFailures.Collapse(
            $"Unable to load the service index for source {Feed}\n  while restoring " +
            "/repo/src/Api/Api.csproj for Serilog", "Serilog", Sources, null);

        Assert.Equal("Unable to load the service index for source {source} while restoring {path} for {package}",
            collapsed);
    }

    [Fact]
    public void CollapseFallsBackToTheAuthorityForUnconfiguredSources()
    {
        var endpoints = new List<string>();
        NuGetSourceFailures.Collapse("Unable to load the service index for source https://nuget.example.com/a/b.json.",
            null, Sources, endpoints);

        Assert.Equal(["https://nuget.example.com"], endpoints);
    }

    [Fact]
    public void CollapseDoesNotMistakeOrdinaryProseForAPath()
    {
        Assert.Equal("either and/or both", NuGetSourceFailures.Collapse("either and/or both", null, null, null));
    }
}
