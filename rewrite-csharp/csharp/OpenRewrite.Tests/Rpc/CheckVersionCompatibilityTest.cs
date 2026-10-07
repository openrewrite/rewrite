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
using System.Reflection;
using System.Runtime.Loader;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.CSharp.Rpc;
using Serilog;
using Serilog.Core;
using Serilog.Events;

namespace OpenRewrite.Tests.Rpc;

public class CheckVersionCompatibilityTest
{
    [Fact]
    public void WarnsWhenPluginReferencesADifferentMajorVersion()
    {
        var plugin = CompilePluginAgainstOpenRewrite("99.0.0.0");

        var warnings = new List<string>();
        var previousLogger = Log.Logger;
        Log.Logger = new LoggerConfiguration()
            .MinimumLevel.Warning()
            .WriteTo.Sink(new CollectingSink(warnings))
            .CreateLogger();
        try
        {
            typeof(RewriteRpcServer)
                .GetMethod("CheckVersionCompatibility", BindingFlags.NonPublic | BindingFlags.Static)!
                .Invoke(null, [plugin]);
        }
        finally
        {
            Log.Logger = previousLogger;
        }

        Assert.Contains(warnings, w => w.Contains("VersionProbePlugin") && w.Contains("99.0.0.0"));
    }

    /// <summary>
    /// Builds a plugin whose metadata references an assembly named like the host's at the given version.
    /// Only the reference metadata is inspected, so the fake dependency never has to load.
    /// </summary>
    private static Assembly CompilePluginAgainstOpenRewrite(string version)
    {
        var hostName = typeof(RewriteRpcServer).Assembly.GetName().Name!;
        var runtimeRefs = ((string)AppContext.GetData("TRUSTED_PLATFORM_ASSEMBLIES")!)
            .Split(Path.PathSeparator)
            .Where(p => Path.GetFileName(p) is "System.Runtime.dll" or "System.Private.CoreLib.dll")
            .Select(p => MetadataReference.CreateFromFile(p))
            .ToList();

        var fakeHost = Emit(hostName,
            $"[assembly: System.Reflection.AssemblyVersion(\"{version}\")] public class Marker {{ }}", runtimeRefs);
        var plugin = Emit("VersionProbePlugin", "public class Plugin : Marker { }",
            [..runtimeRefs, MetadataReference.CreateFromImage(fakeHost)]);

        return new AssemblyLoadContext(null, isCollectible: true).LoadFromStream(new MemoryStream(plugin));
    }

    private static byte[] Emit(string assemblyName, string source, IEnumerable<MetadataReference> refs)
    {
        var compilation = CSharpCompilation.Create(assemblyName, [CSharpSyntaxTree.ParseText(source)], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        using var stream = new MemoryStream();
        var result = compilation.Emit(stream);
        Assert.True(result.Success, string.Join("\n", result.Diagnostics));
        return stream.ToArray();
    }

    private sealed class CollectingSink(List<string> messages) : ILogEventSink
    {
        public void Emit(LogEvent logEvent)
        {
            lock (messages)
                messages.Add(logEvent.RenderMessage());
        }
    }
}
