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
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using Serilog;

var logFile = args.FirstOrDefault(a => a.StartsWith("--log-file="))
    ?.Substring("--log-file=".Length);

var loggerConfig = new LoggerConfiguration();
if (logFile != null)
{
    loggerConfig.MinimumLevel.Debug()
        .WriteTo.File(logFile,
            outputTemplate: "[{Timestamp:HH:mm:ss.fff} {Level:u3}] {Message:lj}{NewLine}{Exception}",
            flushToDiskInterval: TimeSpan.FromSeconds(1));
}

Log.Logger = loggerConfig.CreateLogger();

Log.Information("Process starting");

// Pre-initialize the parser before starting the RPC server
// This prevents initialization delays during RPC handling
var sw = Stopwatch.StartNew();
Log.Debug(">> Parser warmup");
_ = new CSharpParser().Parse("class Warmup {}");
sw.Stop();
Log.Debug("<< Parser warmup ({Elapsed})", sw.Elapsed);

Log.Information("Starting RPC server");
await RewriteRpcServer.RunAsync();
Log.Information("RPC server exited");
