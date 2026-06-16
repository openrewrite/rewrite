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

    private string WriteGlobalJson(string relDir, string content)
    {
        var dir = Path.Combine(_tempDir, relDir);
        Directory.CreateDirectory(dir);
        var path = Path.Combine(dir, "global.json");
        File.WriteAllText(path, content);
        return path;
    }

    [Fact]
    public void RemovesGlobalJsonWhileActiveAndRestoresVerbatimOnDispose()
    {
        // Include a BOM + trailing whitespace to prove byte-for-byte restoration.
        var original = "﻿" + PinnedGlobalJson + "\n";
        var path = Path.Combine(_tempDir, "global.json");
        File.WriteAllText(path, original, new System.Text.UTF8Encoding(encoderShouldEmitUTF8Identifier: true));
        var originalBytes = File.ReadAllBytes(path);

        GlobalJsonGuard guard = GlobalJsonGuard.Neutralize(_tempDir);
        Assert.False(File.Exists(path)); // removed while active

        guard.Dispose();
        Assert.True(File.Exists(path));
        Assert.Equal(originalBytes, File.ReadAllBytes(path)); // restored verbatim
    }

    [Fact]
    public void RemovesNestedGlobalJsonButSkipsBinAndObj()
    {
        var root = WriteGlobalJson(".", PinnedGlobalJson);
        var nested = WriteGlobalJson("src/Project", PinnedGlobalJson);
        var inObj = WriteGlobalJson("src/Project/obj", PinnedGlobalJson);
        var inBin = WriteGlobalJson("bin", PinnedGlobalJson);

        using (GlobalJsonGuard.Neutralize(_tempDir))
        {
            Assert.False(File.Exists(root));
            Assert.False(File.Exists(nested));
            Assert.True(File.Exists(inObj));  // bin/obj are build output, not honored by hostfxr — left alone
            Assert.True(File.Exists(inBin));
        }

        Assert.True(File.Exists(root));
        Assert.True(File.Exists(nested));
    }

    [Fact]
    public void IsReferenceCountedAcrossOverlappingGuards()
    {
        var path = Path.Combine(_tempDir, "global.json");
        File.WriteAllText(path, PinnedGlobalJson);

        var outer = GlobalJsonGuard.Neutralize(_tempDir);
        var inner = GlobalJsonGuard.Neutralize(_tempDir);
        Assert.False(File.Exists(path));

        inner.Dispose();
        Assert.False(File.Exists(path)); // outer still holds it removed

        outer.Dispose();
        Assert.True(File.Exists(path)); // restored only after the last guard releases
    }

    [Fact]
    public void RecoversTrackedButDeletedGlobalJsonFromGitOnDispose()
    {
        InitGitRepoWithCommittedGlobalJson();
        var path = Path.Combine(_tempDir, "global.json");

        // Simulate a prior crashed run that deleted global.json and never restored it.
        File.Delete(path);
        Assert.False(File.Exists(path));

        // A fresh guard over a repo with no global.json present still runs git recovery on dispose.
        GlobalJsonGuard.Neutralize(_tempDir).Dispose();

        Assert.True(File.Exists(path)); // recovered from git
        Assert.Equal(PinnedGlobalJson, File.ReadAllText(path));
    }

    [Fact]
    public void GitRecoveryDoesNotRestoreAFileAnActiveGuardIsHolding()
    {
        InitGitRepoWithCommittedGlobalJson();
        var path = Path.Combine(_tempDir, "global.json");

        var active = GlobalJsonGuard.Neutralize(_tempDir); // removes the tracked global.json
        Assert.False(File.Exists(path));

        // A second guard's dispose triggers git recovery, but must not resurrect the file the
        // active guard deliberately removed.
        GlobalJsonGuard.Neutralize(_tempDir).Dispose();
        Assert.False(File.Exists(path));

        active.Dispose();
        Assert.True(File.Exists(path)); // restored from memory by the holder
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
