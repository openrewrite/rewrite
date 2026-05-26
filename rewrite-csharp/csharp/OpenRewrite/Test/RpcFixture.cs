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
using System.Diagnostics;
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;
using Newtonsoft.Json.Serialization;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Rpc;
using StreamJsonRpc;

namespace OpenRewrite.Test;

/// <summary>
/// xUnit collection fixture that manages a Java RPC test server process.
/// Shared across all tests via ICollectionFixture; call Reset() between
/// tests to clear accumulated state without JVM restart.
/// </summary>
public class RpcFixture : IDisposable
{
    private readonly Process _javaProcess;
    private readonly JsonRpc _jsonRpc;
    private readonly RewriteRpcServer _server;

    public RpcFixture()
    {
        var psi = CreateJavaProcessStartInfo();

        _javaProcess = Process.Start(psi)
            ?? throw new InvalidOperationException("Failed to start Java RPC test server process");

        // Log stderr from Java process asynchronously
        _javaProcess.ErrorDataReceived += (_, e) =>
        {
            if (e.Data != null)
                Console.Error.WriteLine($"[Java RPC] {e.Data}");
        };
        _javaProcess.BeginErrorReadLine();

        // Configure JSON serialization to match Java expectations
        var formatter = new JsonMessageFormatter();
        formatter.JsonSerializer.ContractResolver = new CamelCasePropertyNamesContractResolver();
        formatter.JsonSerializer.Converters.Add(new StringEnumConverter());
        formatter.JsonSerializer.NullValueHandling = NullValueHandling.Ignore;

        var handler = new HeaderDelimitedMessageHandler(
            _javaProcess.StandardInput.BaseStream,
            _javaProcess.StandardOutput.BaseStream,
            formatter);

        _jsonRpc = new JsonRpc(handler);

        _server = new RewriteRpcServer(new RecipeMarketplace());
        _server.Connect(_jsonRpc);
        RewriteRpcServer.SetCurrent(_server);
    }

    public void Reset()
    {
        _server.ResetAll();
    }

    private static ProcessStartInfo CreateJavaProcessStartInfo()
    {
        var cpFile = ResolveClasspathFile();
        var classpath = File.ReadAllText(cpFile).Trim();
        var psi = new ProcessStartInfo("java", "org.openrewrite.maven.rpc.JavaRewriteRpc")
        {
            RedirectStandardInput = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };
        psi.Environment["CLASSPATH"] = classpath;
        return psi;
    }

    /// <summary>
    /// Resolves the rewrite-csharp RPC test server classpath file.
    /// <p>
    /// Order of resolution:
    ///   1. <c>RPC_TEST_SERVER_CLASSPATH</c> env var, if it points at an existing file
    ///      whose mtime isn't older than the SDK assembly that's currently loaded.
    ///      A stale env var (pointing at an unrelated rewrite checkout's leftover
    ///      classpath) silently linking the test process to outdated Java JARs is
    ///      a recurring source of cryptic "recipe didn't fire" failures.
    ///   2. The classpath file at a deterministic location relative to the loaded
    ///      OpenRewrite SDK assembly: walk up from the assembly until we hit
    ///      <c>rewrite-csharp/csharp/OpenRewrite/{bin,obj}</c>, then the classpath
    ///      file is at <c>rewrite-csharp/build/rpc-test-server-classpath.txt</c>.
    /// </summary>
    private static string ResolveClasspathFile()
    {
        var sdkRelativeFile = AutoLocateClasspathFile();
        var envFile = Environment.GetEnvironmentVariable("RPC_TEST_SERVER_CLASSPATH");

        // Prefer the SDK-relative file when it exists and is at least as fresh as
        // any env-pointed file — guards against a stale env var inherited from a
        // different rewrite checkout.
        if (sdkRelativeFile != null && File.Exists(sdkRelativeFile))
        {
            if (string.IsNullOrEmpty(envFile) || !File.Exists(envFile) ||
                File.GetLastWriteTimeUtc(sdkRelativeFile) >=
                File.GetLastWriteTimeUtc(envFile))
            {
                return sdkRelativeFile;
            }
        }

        if (!string.IsNullOrEmpty(envFile) && File.Exists(envFile))
            return envFile;

        if (sdkRelativeFile != null && File.Exists(sdkRelativeFile))
            return sdkRelativeFile;

        throw new InvalidOperationException(
            "Cannot locate the Java RPC test server classpath. Either set " +
            "RPC_TEST_SERVER_CLASSPATH, or run " +
            "`./gradlew :rewrite-csharp:rpcTestClasspath` from the rewrite SDK " +
            "checkout. Searched SDK-relative path: " +
            (sdkRelativeFile ?? "(could not derive from loaded SDK assembly)"));
    }

    private static string? AutoLocateClasspathFile()
    {
        // Two layout cases to handle:
        //   (a) Test runs INSIDE the rewrite SDK repo: walk up from the SDK assembly
        //       until we find `<root>/rewrite-csharp/csharp/OpenRewrite/`.
        //   (b) Test runs in a CONSUMING project (e.g. recipes-csharp) that pulls in
        //       the SDK via ProjectReference. The DLL is copied to the test project's
        //       bin/, so the assembly path doesn't reach the SDK source. Instead, walk
        //       up from the assembly looking for an `external/openrewrite/rewrite`
        //       symlink (the convention for source-linked SDK in Conductor / consumer
        //       repos), then derive the classpath from the symlink target.
        var sdkAssembly = typeof(OpenRewrite.CSharp.CSharpParser).Assembly.Location;
        if (string.IsNullOrEmpty(sdkAssembly))
            return null;

        var dir = Path.GetDirectoryName(sdkAssembly);
        while (dir != null)
        {
            // Case (a): inside the SDK repo.
            if (Path.GetFileName(dir) == "OpenRewrite")
            {
                var parent = Path.GetDirectoryName(dir);
                var grandparent = parent != null ? Path.GetDirectoryName(parent) : null;
                if (parent != null && grandparent != null &&
                    Path.GetFileName(parent) == "csharp" &&
                    Path.GetFileName(grandparent) == "rewrite-csharp")
                {
                    return Path.Combine(grandparent, "build", "rpc-test-server-classpath.txt");
                }
            }

            // Case (b): consuming repo with `external/openrewrite/rewrite` symlink.
            var symlink = Path.Combine(dir, "external", "openrewrite", "rewrite");
            if (Directory.Exists(symlink))
            {
                var sdkMarker = Path.Combine(symlink,
                    "rewrite-csharp", "csharp", "OpenRewrite", "OpenRewrite.csproj");
                if (File.Exists(sdkMarker))
                {
                    return Path.Combine(symlink,
                        "rewrite-csharp", "build", "rpc-test-server-classpath.txt");
                }
            }
            dir = Path.GetDirectoryName(dir);
        }
        return null;
    }

    public void Dispose()
    {
        RewriteRpcServer.SetCurrent(null);

        try
        {
            _jsonRpc.Dispose();
        }
        catch
        {
            // Ignore errors during shutdown
        }

        if (!_javaProcess.HasExited)
        {
            _javaProcess.Kill();
            _javaProcess.WaitForExit(5000);
        }

        _javaProcess.Dispose();
    }
}
