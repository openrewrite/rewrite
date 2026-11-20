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
using System.Text.Json.Nodes;
using LibGit2Sharp;
using OpenRewrite.CSharp;

namespace OpenRewrite.Tests;

public class GlobalJsonGuardTests : IDisposable
{
    private readonly string _tempDir;

    public GlobalJsonGuardTests()
    {
        _tempDir = Path.Combine(Path.GetTempPath(), "GlobalJsonGuard_" + Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(_tempDir);
    }

    public void Dispose()
    {
        try { Directory.Delete(_tempDir, true); } catch { /* best effort */ }
    }

    private const string PinnedGlobalJson = """{"sdk":{"version":"8.0.100","rollForward":"disable"}}""";

    private const string PinnedWithMsBuildSdks = """
        {
          // Arcade-style repo pin
          "sdk": { "version": "8.0.100", "rollForward": "disable" },
          "tools": { "dotnet": "8.0.100" },
          "msbuild-sdks": { "MSTest.Sdk": "3.6.4" },
        }
        """;

    private string WriteGlobalJson(string relDir, string content)
    {
        var dir = Path.Combine(_tempDir, relDir);
        Directory.CreateDirectory(dir);
        var path = Path.Combine(dir, "global.json");
        File.WriteAllText(path, content);
        return path;
    }

    private static JsonObject ReadJson(string path) => (JsonObject)JsonNode.Parse(File.ReadAllText(path))!;

    [Fact]
    public void StripsSdkWhileActiveAndRestoresVerbatimOnDispose()
    {
        var original = PinnedGlobalJson + "\n";
        var path = Path.Combine(_tempDir, "global.json");
        File.WriteAllText(path, original, new System.Text.UTF8Encoding(encoderShouldEmitUTF8Identifier: true));
        var originalBytes = File.ReadAllBytes(path);

        GlobalJsonGuard guard = GlobalJsonGuard.Neutralize(_tempDir);
        var neutralized = ReadJson(path);
        Assert.False(neutralized.ContainsKey("sdk"));

        guard.Dispose();
        Assert.Equal(originalBytes, File.ReadAllBytes(path));
    }

    [Fact]
    public void KeepsMsBuildSdksAndOtherSectionsWhileActive()
    {
        var path = WriteGlobalJson(".", PinnedWithMsBuildSdks);

        using (GlobalJsonGuard.Neutralize(_tempDir))
        {
            var neutralized = ReadJson(path);
            Assert.False(neutralized.ContainsKey("sdk"));
            Assert.Equal("3.6.4", neutralized["msbuild-sdks"]!["MSTest.Sdk"]!.GetValue<string>());
            Assert.Equal("8.0.100", neutralized["tools"]!["dotnet"]!.GetValue<string>());
        }

        Assert.Equal(PinnedWithMsBuildSdks, File.ReadAllText(path));
    }

    [Fact]
    public void LeavesGlobalJsonWithoutSdkSectionUntouched()
    {
        const string msBuildSdksOnly = """{ "msbuild-sdks": { "Microsoft.Build.NoTargets": "3.7.56" } }""";
        var path = WriteGlobalJson(".", msBuildSdksOnly);
        var lastWrite = File.GetLastWriteTimeUtc(path);

        using (GlobalJsonGuard.Neutralize(_tempDir))
        {
            Assert.Equal(msBuildSdksOnly, File.ReadAllText(path));
        }

        Assert.Equal(msBuildSdksOnly, File.ReadAllText(path));
        Assert.Equal(lastWrite, File.GetLastWriteTimeUtc(path));
    }

    [Theory]
    [InlineData("""{ "sdk": { "version": """)]
    [InlineData("""{ "sdk": { "version": "8.0.100" }, "sdk": { "version": "9.0.100" } }""")]
    [InlineData("""[ "not", "an", "object" ]""")]
    public void RemovesGlobalJsonItCannotRewriteWhileActive(string content)
    {
        var path = WriteGlobalJson(".", content);

        using (GlobalJsonGuard.Neutralize(_tempDir))
        {
            Assert.False(File.Exists(path));
        }

        Assert.Equal(content, File.ReadAllText(path));
    }

    [Fact]
    public void NeutralizesNestedGlobalJsonButSkipsBinAndObj()
    {
        var root = WriteGlobalJson(".", PinnedGlobalJson);
        var nested = WriteGlobalJson("src/Project", PinnedGlobalJson);
        var inObj = WriteGlobalJson("src/Project/obj", PinnedGlobalJson);
        var inBin = WriteGlobalJson("bin", PinnedGlobalJson);

        using (GlobalJsonGuard.Neutralize(_tempDir))
        {
            Assert.False(ReadJson(root).ContainsKey("sdk"));
            Assert.False(ReadJson(nested).ContainsKey("sdk"));
            Assert.Equal(PinnedGlobalJson, File.ReadAllText(inObj));
            Assert.Equal(PinnedGlobalJson, File.ReadAllText(inBin));
        }

        Assert.Equal(PinnedGlobalJson, File.ReadAllText(root));
        Assert.Equal(PinnedGlobalJson, File.ReadAllText(nested));
    }

    [Fact]
    public void DoesNotClobberAGlobalJsonChangedWhileActive()
    {
        var path = WriteGlobalJson(".", PinnedGlobalJson);
        const string edited = """{"sdk":{"version":"10.0.100"}}""";

        using (GlobalJsonGuard.Neutralize(_tempDir))
        {
            File.WriteAllText(path, edited);
        }

        Assert.Equal(edited, File.ReadAllText(path));
    }

    [Fact]
    public void IsReferenceCountedAcrossOverlappingGuards()
    {
        var path = WriteGlobalJson(".", PinnedGlobalJson);

        var outer = GlobalJsonGuard.Neutralize(_tempDir);
        var inner = GlobalJsonGuard.Neutralize(_tempDir);
        Assert.False(ReadJson(path).ContainsKey("sdk"));

        inner.Dispose();
        Assert.False(ReadJson(path).ContainsKey("sdk"));

        outer.Dispose();
        Assert.Equal(PinnedGlobalJson, File.ReadAllText(path));
    }

    [Fact]
    public void RecoversTrackedButDeletedGlobalJsonFromGitOnDispose()
    {
        InitGitRepoWithCommittedGlobalJson();
        var path = Path.Combine(_tempDir, "global.json");
        File.Delete(path);

        GlobalJsonGuard.Neutralize(_tempDir).Dispose();

        Assert.Equal(PinnedGlobalJson, File.ReadAllText(path));
    }

    [Fact]
    public void RecoversTrackedGlobalJsonLeftNeutralizedFromGitOnDispose()
    {
        InitGitRepoWithCommittedGlobalJson();
        var path = Path.Combine(_tempDir, "global.json");
        Assert.True(GlobalJsonGuard.TryNeutralize(File.ReadAllBytes(path), out var neutralized));
        File.WriteAllBytes(path, neutralized!);

        GlobalJsonGuard.Neutralize(_tempDir).Dispose();

        Assert.Equal(PinnedGlobalJson, File.ReadAllText(path));
    }

    [Fact]
    public void GitRecoveryDoesNotRestoreAFileAnActiveGuardIsHolding()
    {
        InitGitRepoWithCommittedGlobalJson();
        var path = Path.Combine(_tempDir, "global.json");

        var active = GlobalJsonGuard.Neutralize(_tempDir);
        Assert.False(ReadJson(path).ContainsKey("sdk"));

        GlobalJsonGuard.Neutralize(_tempDir).Dispose();
        Assert.False(ReadJson(path).ContainsKey("sdk"));

        active.Dispose();
        Assert.Equal(PinnedGlobalJson, File.ReadAllText(path));
    }

    [Fact]
    public async Task GuardedSolutionLoadSucceedsDespitePinnedGlobalJson()
    {
        // A project pinning an uninstalled SDK band with rollForward disabled — the worst case.
        File.WriteAllText(Path.Combine(_tempDir, "Test.csproj"), """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup><TargetFramework>net8.0</TargetFramework></PropertyGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(_tempDir, "HelloWorld.cs"),
            "namespace Test { public class HelloWorld { } }\n");
        var gj = Path.Combine(_tempDir, "global.json");
        File.WriteAllText(gj, PinnedGlobalJson);
        var proj = Path.Combine(_tempDir, "Test.csproj");

        var parser = new SolutionParser();
        using (GlobalJsonGuard.Neutralize(_tempDir))
        {
            var solution = await parser.LoadAsync(proj);
            var results = parser.ParseProject(solution, proj, _tempDir);
            Assert.Single(results);
            Assert.IsType<CompilationUnit>(results[0]);
        }

        // The pin is restored verbatim once parsing completes.
        Assert.True(File.Exists(gj));
        Assert.Equal(PinnedGlobalJson, File.ReadAllText(gj));
    }

    [Fact]
    public async Task GuardedSolutionLoadResolvesVersionlessMsBuildSdkPinnedInGlobalJson()
    {
        File.WriteAllText(Path.Combine(_tempDir, "Test.csproj"), """
            <Project Sdk="MSTest.Sdk">
              <PropertyGroup><TargetFramework>net10.0</TargetFramework></PropertyGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(_tempDir, "Tests.cs"),
            "namespace Test { public class Tests { } }\n");
        var gj = WriteGlobalJson(".", PinnedWithMsBuildSdks);
        var proj = Path.Combine(_tempDir, "Test.csproj");

        var parser = new SolutionParser();
        using (GlobalJsonGuard.Neutralize(_tempDir))
        {
            var solution = await parser.LoadAsync(proj);
            Assert.Empty(parser.UnevaluatedProjects);
            var results = parser.ParseProject(solution, proj, _tempDir);
            Assert.IsType<CompilationUnit>(Assert.Single(results));
        }

        Assert.Equal(PinnedWithMsBuildSdks, File.ReadAllText(gj));
    }

    [Fact]
    public void NoGlobalJsonIsANoOp()
    {
        // No global.json anywhere, not a git repo: Neutralize/Dispose must not throw.
        using (GlobalJsonGuard.Neutralize(_tempDir)) { }
        Assert.Empty(Directory.EnumerateFiles(_tempDir));
    }

    private void InitGitRepoWithCommittedGlobalJson()
    {
        File.WriteAllText(Path.Combine(_tempDir, "global.json"), PinnedGlobalJson);
        Repository.Init(_tempDir);
        using var repo = new Repository(_tempDir);
        Commands.Stage(repo, "global.json");
        var sig = new Signature("Test", "test@example.com", DateTimeOffset.UnixEpoch);
        repo.Commit("initial", sig, sig);
    }
}
