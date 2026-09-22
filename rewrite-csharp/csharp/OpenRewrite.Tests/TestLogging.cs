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
using System.Runtime.CompilerServices;
using Serilog;
using Serilog.Core;
using Serilog.Events;

namespace OpenRewrite.Tests;

/// <summary>
/// Routes Serilog warnings and errors to the console for the test run. Without a sink the engine's
/// diagnostics are silently dropped, which leaves a CI-only failure with nothing to go on.
/// </summary>
internal static class TestLogging
{
    [ModuleInitializer]
    internal static void Initialize()
    {
        Log.Logger = new LoggerConfiguration()
            .MinimumLevel.Warning()
            .WriteTo.Sink(new ConsoleSink())
            .CreateLogger();
    }

    private sealed class ConsoleSink : ILogEventSink
    {
        public void Emit(LogEvent logEvent)
        {
            Console.WriteLine($"[{logEvent.Level}] {logEvent.RenderMessage()}");
            if (logEvent.Exception != null)
                Console.WriteLine(logEvent.Exception);
        }
    }
}
