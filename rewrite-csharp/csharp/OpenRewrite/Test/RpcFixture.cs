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
        var cpFile = Environment.GetEnvironmentVariable("RPC_TEST_SERVER_CLASSPATH")
                     ?? throw new InvalidOperationException(
                         "RPC_TEST_SERVER_CLASSPATH environment variable not set. " +
                         "Run './gradlew :rewrite-csharp:rpcTestClasspath' to generate the classpath file.");

        var classpath = File.ReadAllText(cpFile).Trim();
        return new ProcessStartInfo("java",
            $"-cp \"{classpath}\" org.openrewrite.maven.rpc.JavaRewriteRpc")
        {
            RedirectStandardInput = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };
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
