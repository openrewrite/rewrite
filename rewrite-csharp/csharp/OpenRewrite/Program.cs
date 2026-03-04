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
