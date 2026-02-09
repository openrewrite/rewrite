using Newtonsoft.Json;
using Newtonsoft.Json.Converters;
using Newtonsoft.Json.Serialization;
using Rewrite.Core;
using Rewrite.Core.Rpc;
using Rewrite.Java;
using StreamJsonRpc;
using static Rewrite.Core.Rpc.RpcObjectData.ObjectState;

namespace Rewrite.CSharp.Rpc;

public class RewriteRpcServer
{
    private readonly CSharpParser _parser = new();

    /// <summary>
    /// Objects that have been parsed locally and are available for remote access.
    /// </summary>
    private readonly Dictionary<string, object?> _localObjects = new();

    /// <summary>
    /// Our understanding of the remote's state of objects.
    /// </summary>
    private readonly Dictionary<string, object?> _remoteObjects = new();

    /// <summary>
    /// Referentially deduplicated objects and their ref IDs.
    /// </summary>
    private readonly Dictionary<object, int> _localRefs = new(ReferenceEqualityComparer.Instance);

    public RewriteRpcServer()
    {
        // Register type name overrides for nagoya types that don't match Java names
        RpcSendQueue.RegisterJavaTypeName(typeof(NamespaceDeclaration),
            "org.openrewrite.csharp.tree.Cs$BlockScopeNamespaceDeclaration");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsLambda),
            "org.openrewrite.csharp.tree.Cs$Lambda");
        RpcSendQueue.RegisterJavaTypeName(typeof(YieldStatement),
            "org.openrewrite.csharp.tree.Cs$Yield");

        // Types in nagoya's Rewrite.Java namespace that don't follow nesting conventions
        RpcSendQueue.RegisterJavaTypeName(typeof(Java.NamedVariable),
            "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable");

        // Types in nagoya's Rewrite.Java namespace that are Cs types in Java
        RpcSendQueue.RegisterJavaTypeName(typeof(Java.ExpressionStatement),
            "org.openrewrite.csharp.tree.Cs$ExpressionStatement");

        // Marker type name overrides — markers live in marker packages, not tree packages
        RpcSendQueue.RegisterJavaTypeName(typeof(Java.Semicolon),
            "org.openrewrite.java.marker.Semicolon");
        RpcSendQueue.RegisterJavaTypeName(typeof(PrimaryConstructor),
            "org.openrewrite.csharp.marker.PrimaryConstructor");
        RpcSendQueue.RegisterJavaTypeName(typeof(Implicit),
            "org.openrewrite.csharp.marker.Implicit");
        RpcSendQueue.RegisterJavaTypeName(typeof(Struct),
            "org.openrewrite.csharp.marker.Struct");
        RpcSendQueue.RegisterJavaTypeName(typeof(RecordClass),
            "org.openrewrite.csharp.marker.RecordClass");
        RpcSendQueue.RegisterJavaTypeName(typeof(ExpressionBodied),
            "org.openrewrite.csharp.marker.ExpressionBodied");
    }

    [JsonRpcMethod("Parse", UseSingleObjectParameterDeserialization = true)]
    public Task<string[]> Parse(ParseRequest request)
    {
        var sourceFileIds = new List<string>();

        if (request.Inputs == null)
        {
            return Task.FromResult(sourceFileIds.ToArray());
        }

        foreach (var input in request.Inputs)
        {
            string content;
            if (input.Text != null)
            {
                content = input.Text;
            }
            else
            {
                content = File.ReadAllText(input.SourcePath);
            }

            var cu = _parser.Parse(content, input.SourcePath);
            var id = cu.Id.ToString();
            _localObjects[id] = cu;
            sourceFileIds.Add(id);
        }

        return Task.FromResult(sourceFileIds.ToArray());
    }

    [JsonRpcMethod("GetObject", UseSingleObjectParameterDeserialization = true)]
    public Task<List<RpcObjectData>> GetObject(GetObjectRequest request)
    {
        var after = _localObjects.GetValueOrDefault(request.Id);

        if (after == null)
        {
            return Task.FromResult(new List<RpcObjectData>
            {
                new() { State = DELETE },
                new() { State = END_OF_OBJECT }
            });
        }

        var before = _remoteObjects.GetValueOrDefault(request.Id);

        // Accumulate all RPC data into a single list
        var allData = new List<RpcObjectData>();
        var sendQueue = new RpcSendQueue(
            1024,
            batch => allData.AddRange(batch),
            _localRefs,
            request.SourceFileType,
            false,
            TreeCodec.Instance
        );

        sendQueue.Send(after, before, null);
        sendQueue.Put(new RpcObjectData { State = END_OF_OBJECT });
        sendQueue.Flush();

        // Update our understanding of remote's state
        _remoteObjects[request.Id] = after;

        return Task.FromResult(allData);
    }

    [JsonRpcMethod("Print", UseSingleObjectParameterDeserialization = true)]
    public Task<string> Print(PrintRequest request)
    {
        if (!_localObjects.TryGetValue(request.TreeId, out var obj) || obj is not Tree tree)
        {
            throw new InvalidOperationException($"Tree not found: {request.TreeId}");
        }

        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(tree);
        return Task.FromResult(printed);
    }

    public static async Task RunAsync(CancellationToken cancellationToken = default)
    {
        using var inputStream = Console.OpenStandardInput();
        using var outputStream = Console.OpenStandardOutput();

        // Configure JSON serialization to match Java expectations:
        // - camelCase property names
        // - string enum values (not integers)
        var formatter = new JsonMessageFormatter();
        formatter.JsonSerializer.ContractResolver = new CamelCasePropertyNamesContractResolver();
        formatter.JsonSerializer.Converters.Add(new StringEnumConverter());
        formatter.JsonSerializer.NullValueHandling = NullValueHandling.Ignore;

        var handler = new HeaderDelimitedMessageHandler(outputStream, inputStream, formatter);
        using var jsonRpc = new JsonRpc(handler);

        var server = new RewriteRpcServer();
        jsonRpc.AddLocalRpcTarget(server);
        jsonRpc.StartListening();

        await jsonRpc.Completion.WaitAsync(cancellationToken);
    }
    private class TreeCodec : IRpcCodec
    {
        public static readonly TreeCodec Instance = new();
        public void RpcSend(object after, RpcSendQueue q) => new CSharpSender().Visit((J)after, q);
        public object RpcReceive(object before, RpcReceiveQueue q) => throw new NotImplementedException();
    }
}

// --- Request DTOs ---

public class ParseRequest
{
    public List<ParseInput>? Inputs { get; set; }
}

public class ParseInput
{
    public string SourcePath { get; set; } = "";
    public string? Text { get; set; }
}

public class GetObjectRequest
{
    public string Id { get; set; } = "";
    public string? SourceFileType { get; set; }
}

public class PrintRequest
{
    public string TreeId { get; set; } = "";
    public string? SourcePath { get; set; }
    public string? SourceFileType { get; set; }
}
