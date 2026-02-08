using Rewrite.CSharp;
using Rewrite.CSharp.Rpc;

// Pre-initialize the parser before starting the RPC server
// This prevents initialization delays during RPC handling
_ = new CSharpParser().Parse("class Warmup {}");

await RewriteRpcServer.RunAsync();
