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
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Reflection;
using System.Runtime.Loader;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Xml.Linq;
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;
using Serilog;
using StreamJsonRpc;
using StreamJsonRpc.Protocol;
using static OpenRewrite.Core.Rpc.RpcObjectData.ObjectState;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp.Rpc;

public class RewriteRpcServer
{
    private static RewriteRpcServer? _current;

    /// <summary>
    /// The current RPC server instance, or null if not running.
    /// Used by RpcVisitor and Preconditions to delegate to the Java peer.
    /// </summary>
    public static RewriteRpcServer? Current => _current;

    /// <summary>
    /// Sets the current RPC server instance. Used by test infrastructure to wire up
    /// an RPC connection to a Java process without going through RunAsync().
    /// Matches the JavaScript pattern: RewriteRpc.set(value) / RewriteRpc.get().
    /// </summary>
    public static void SetCurrent(RewriteRpcServer? server) => _current = server;

    private readonly RecipeMarketplace _marketplace;

    // Recipe name -> the package that contributed it, recorded at install time and persisted for the
    // process lifetime so GetMarketplace can attribute each row even when a later install is a no-op.
    private readonly ConcurrentDictionary<string, string> _recipeOrigin = new();

    private readonly ConcurrentDictionary<string, Recipe> _preparedRecipes = new();
    private readonly ConcurrentDictionary<string, object?> _recipeAccumulators = new();
    private readonly ConcurrentDictionary<string, ExecutionContext> _executionContexts = new();

    private volatile IDataTableStore? _configuredDataTableStore;

    private string? _recipesProjectDir;
    private readonly string? _recipeInstallDir;
    private JsonRpc? _jsonRpc;
    private DotNetBuildContext? _buildContext;

    /// <summary>
    /// Objects that have been parsed locally and are available for remote access.
    /// </summary>
    private readonly ConcurrentDictionary<string, object?> _localObjects = new();

    /// <summary>
    /// Our understanding of the remote's state of objects.
    /// </summary>
    private readonly ConcurrentDictionary<string, object?> _remoteObjects = new();

    /// <summary>
    /// Referentially deduplicated objects and their ref IDs.
    /// </summary>
    private readonly ConcurrentDictionary<object, int> _localRefs = new(ReferenceEqualityComparer.Instance);

    /// <summary>
    /// Refs received from the remote process (Java) for deduplication.
    /// </summary>
    private readonly ConcurrentDictionary<int, object> _remoteRefs = new();

    /// <summary>
    /// Ref high-water per source file (send-side _localRefs count, receive-side max _remoteRefs
    /// key), captured before first visit so <see cref="Evict"/> rolls back exactly its refs.
    /// </summary>
    private readonly ConcurrentDictionary<string, (int LocalRefs, int RemoteRefsMax)> _refCheckpoints = new();

    /// <summary>
    /// Connects this server to a remote JSON-RPC peer. Used by test infrastructure
    /// to wire up an RPC connection to a Java process.
    /// </summary>
    public void Connect(JsonRpc jsonRpc)
    {
        _jsonRpc = jsonRpc;
        jsonRpc.SynchronizationContext = null;
        jsonRpc.AddLocalRpcTarget(this);
        jsonRpc.StartListening();
    }

    public RewriteRpcServer(RecipeMarketplace marketplace, string? recipeInstallDir = null)
    {
        _marketplace = marketplace;
        _recipeInstallDir = recipeInstallDir;

        // Register type name overrides for nagoya types that don't match Java names
        RpcSendQueue.RegisterJavaTypeName(typeof(CsLambda),
            "org.openrewrite.csharp.tree.Cs$Lambda");
        // Cs-prefixed types in C# that correspond to unprefixed Java names
        RpcSendQueue.RegisterJavaTypeName(typeof(CsBinary),
            "org.openrewrite.csharp.tree.Cs$Binary");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsUnary),
            "org.openrewrite.csharp.tree.Cs$Unary");

        // Structured XML doc-comment tree — nested C# types map to the Java CsDocComment model.
        RpcSendQueue.RegisterJavaTypeName(typeof(CsDocComment.DocComment),
            "org.openrewrite.csharp.tree.CsDocComment$DocComment");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsDocComment.XmlElement),
            "org.openrewrite.csharp.tree.CsDocComment$XmlElement");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsDocComment.XmlEmptyElement),
            "org.openrewrite.csharp.tree.CsDocComment$XmlEmptyElement");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsDocComment.XmlText),
            "org.openrewrite.csharp.tree.CsDocComment$XmlText");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsDocComment.XmlAttribute),
            "org.openrewrite.csharp.tree.CsDocComment$XmlAttribute");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsDocComment.XmlCrefAttribute),
            "org.openrewrite.csharp.tree.CsDocComment$XmlCrefAttribute");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsDocComment.XmlNameAttribute),
            "org.openrewrite.csharp.tree.CsDocComment$XmlNameAttribute");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsDocComment.LineBreak),
            "org.openrewrite.csharp.tree.CsDocComment$LineBreak");

        // Types in nagoya's Rewrite.Java namespace that don't follow nesting conventions
        RpcSendQueue.RegisterJavaTypeName(typeof(Java.NamedVariable),
            "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable");

        // Marker type name overrides — markers live in marker packages, not tree packages
        RpcSendQueue.RegisterJavaTypeName(typeof(Java.Semicolon),
            "org.openrewrite.java.marker.Semicolon");
        RpcSendQueue.RegisterJavaTypeName(typeof(Java.NullSafe),
            "org.openrewrite.java.marker.NullSafe");
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
        RpcSendQueue.RegisterJavaTypeName(typeof(OmitParentheses),
            "org.openrewrite.java.marker.OmitParentheses");
        RpcSendQueue.RegisterJavaTypeName(typeof(AnonymousMethod),
            "org.openrewrite.csharp.marker.AnonymousMethod");
        RpcSendQueue.RegisterJavaTypeName(typeof(CSharpFormatStyle),
            "org.openrewrite.csharp.style.CSharpFormatStyle");
        RpcSendQueue.RegisterJavaTypeName(typeof(ConditionalBranchMarker),
            "org.openrewrite.csharp.marker.ConditionalBranchMarker");
        RpcSendQueue.RegisterJavaTypeName(typeof(DirectiveBoundaryMarker),
            "org.openrewrite.csharp.marker.DirectiveBoundaryMarker");
        RpcSendQueue.RegisterJavaTypeName(typeof(PatternCombinator),
            "org.openrewrite.csharp.marker.PatternCombinator");
        RpcSendQueue.RegisterJavaTypeName(typeof(WhereClauseOrder),
            "org.openrewrite.csharp.marker.WhereClauseOrder");
        RpcSendQueue.RegisterJavaTypeName(typeof(MultiDimensionContinuation),
            "org.openrewrite.csharp.marker.MultiDimensionContinuation");
        RpcSendQueue.RegisterJavaTypeName(typeof(TrailingComma),
            "org.openrewrite.java.marker.TrailingComma");
        RpcSendQueue.RegisterJavaTypeName(typeof(OmitBraces),
            "org.openrewrite.java.marker.OmitBraces");
        RpcSendQueue.RegisterJavaTypeName(typeof(NullSafe),
            "org.openrewrite.java.marker.NullSafe");
        RpcSendQueue.RegisterJavaTypeName(typeof(PointerMemberAccess),
            "org.openrewrite.csharp.marker.PointerMemberAccess");
        RpcSendQueue.RegisterJavaTypeName(typeof(ForEachVariableLoopControl),
            "org.openrewrite.csharp.tree.Cs$ForEachVariableLoop$Control");

        // Marker type overrides for markers that live in Cs.java but map to marker package
        RpcSendQueue.RegisterJavaTypeName(typeof(ImplicitTypeParameters),
            "org.openrewrite.csharp.marker.ImplicitTypeParameters");

        // DotNetProject marker
        RpcSendQueue.RegisterJavaTypeName(typeof(DotNetProject),
            "org.openrewrite.csharp.marker.DotNetProject");

        // MSBuildProject marker and nested types
        RpcSendQueue.RegisterJavaTypeName(typeof(MSBuildProject),
            "org.openrewrite.csharp.marker.MSBuildProject");
        RpcSendQueue.RegisterJavaTypeName(typeof(TargetFramework),
            "org.openrewrite.csharp.marker.MSBuildProject$TargetFramework");
        RpcSendQueue.RegisterJavaTypeName(typeof(PackageReference),
            "org.openrewrite.csharp.marker.MSBuildProject$PackageReference");
        RpcSendQueue.RegisterJavaTypeName(typeof(ResolvedPackage),
            "org.openrewrite.csharp.marker.MSBuildProject$ResolvedPackage");
        RpcSendQueue.RegisterJavaTypeName(typeof(ProjectReference),
            "org.openrewrite.csharp.marker.MSBuildProject$ProjectReference");
        RpcSendQueue.RegisterJavaTypeName(typeof(PropertyValue),
            "org.openrewrite.csharp.marker.MSBuildProject$PropertyValue");
        RpcSendQueue.RegisterJavaTypeName(typeof(PackageSource),
            "org.openrewrite.csharp.marker.MSBuildProject$PackageSource");

        // LINQ types live in Linq$ not Cs$ on the Java side
        RpcSendQueue.RegisterJavaTypeName(typeof(QueryExpression),
            "org.openrewrite.csharp.tree.Linq$QueryExpression");
        RpcSendQueue.RegisterJavaTypeName(typeof(QueryBody),
            "org.openrewrite.csharp.tree.Linq$QueryBody");
        RpcSendQueue.RegisterJavaTypeName(typeof(FromClause),
            "org.openrewrite.csharp.tree.Linq$FromClause");
        RpcSendQueue.RegisterJavaTypeName(typeof(LetClause),
            "org.openrewrite.csharp.tree.Linq$LetClause");
        RpcSendQueue.RegisterJavaTypeName(typeof(JoinClause),
            "org.openrewrite.csharp.tree.Linq$JoinClause");
        RpcSendQueue.RegisterJavaTypeName(typeof(JoinIntoClause),
            "org.openrewrite.csharp.tree.Linq$JoinIntoClause");
        RpcSendQueue.RegisterJavaTypeName(typeof(WhereClause),
            "org.openrewrite.csharp.tree.Linq$WhereClause");
        RpcSendQueue.RegisterJavaTypeName(typeof(OrderByClause),
            "org.openrewrite.csharp.tree.Linq$OrderByClause");
        RpcSendQueue.RegisterJavaTypeName(typeof(Ordering),
            "org.openrewrite.csharp.tree.Linq$Ordering");
        RpcSendQueue.RegisterJavaTypeName(typeof(SelectClause),
            "org.openrewrite.csharp.tree.Linq$SelectClause");
        RpcSendQueue.RegisterJavaTypeName(typeof(GroupClause),
            "org.openrewrite.csharp.tree.Linq$GroupClause");
        RpcSendQueue.RegisterJavaTypeName(typeof(QueryContinuation),
            "org.openrewrite.csharp.tree.Linq$QueryContinuation");

        RpcSendQueue.RegisterJavaTypeName(typeof(ParseError),
            "org.openrewrite.tree.ParseError");
        RpcSendQueue.RegisterJavaTypeName(typeof(ParseExceptionResult),
            "org.openrewrite.ParseExceptionResult");
    }

    [JsonRpcMethod("ParseSolution", UseSingleObjectParameterDeserialization = true)]
    public async Task<ParseSolutionResponse> ParseSolution(ParseSolutionRequest request)
    {
        Log.Debug("RPC ParseSolution: received request path={Path} rootDir={RootDir}", request.Path, request.RootDir);
        var solutionParser = new SolutionParser();
        var path = ResolvePath(request.Path);
        var rootDir = ResolvePath(request.RootDir);

        // Remove any global.json SDK pin before running dotnet restore / loading the workspace,
        // so the latest installed SDK is used regardless of the version the repo pins. The
        // file(s) are restored verbatim when the guard is disposed at the end of this method
        // (after all projects are parsed), and any global.json a prior crashed run left deleted
        // is recovered from git at that point too.
        using var globalJsonGuard = GlobalJsonGuard.Neutralize(rootDir);

        var requirePrintEqualsInput = true;
        if (request.Options?.TryGetValue("org.openrewrite.requirePrintEqualsInput", out var val) == true)
        {
            // SystemTextJsonFormatter delivers object-typed option values as JsonElement.
            requirePrintEqualsInput = val switch
            {
                JsonElement { ValueKind: JsonValueKind.True } => true,
                JsonElement { ValueKind: JsonValueKind.False } => false,
                JsonElement { ValueKind: JsonValueKind.String } je => bool.Parse(je.GetString()!),
                JsonElement je => Convert.ToBoolean(je.ToString()),
                _ => Convert.ToBoolean(val),
            };
        }

        var solution = await solutionParser.LoadAsync(path, CancellationToken.None);

        var response = new ParseSolutionResponse();
        var seenProjects = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var projectList = solution.Projects.Where(p => p.FilePath != null).ToList();
        Log.Debug("RPC ParseSolution: {ProjectCount} projects to parse", projectList.Count);

        var projectIndex = 0;
        foreach (var project in projectList)
        {
            if (!seenProjects.Add(project.FilePath!))
                continue;

            projectIndex++;
            Log.Debug("RPC ParseSolution: parsing project [{ProjectIndex}/{ProjectCount}] {ProjectName}",
                projectIndex, projectList.Count, Path.GetFileNameWithoutExtension(project.FilePath));

            List<SourceFile> sourceFiles;
            try
            {
                sourceFiles = solutionParser.ParseProject(solution, project.FilePath!, rootDir,
                    requirePrintEqualsInput);
            }
            catch (Exception ex)
            {
                Log.Debug("RPC ParseSolution: EXCEPTION parsing project {ProjectPath}: {ExType}: {ExMessage}",
                    project.FilePath, ex.GetType().Name, ex.Message);
                throw;
            }

            foreach (var sourceFile in sourceFiles)
            {
                var id = sourceFile.Id.ToString();
                var sourceFileType = sourceFile is ParseError
                    ? "org.openrewrite.tree.ParseError"
                    : "org.openrewrite.csharp.tree.Cs$CompilationUnit";

                _localObjects[id] = sourceFile;
                response.Items.Add(new ParseSolutionResponseItem
                {
                    Id = id,
                    SourceFileType = sourceFileType
                });
            }

            // Oversize source files skipped during parsing are emitted as Quarks; the Java
            // side builds each Quark from SourcePath locally, so they carry no content and
            // are deliberately not registered in _localObjects (no GetObject round-trip).
            foreach (var relPath in solutionParser.LastOversizePaths)
            {
                response.Items.Add(new ParseSolutionResponseItem
                {
                    Id = Guid.NewGuid().ToString(),
                    SourceFileType = "org.openrewrite.quark.Quark",
                    SourcePath = relPath
                });
            }

            // Parse the .csproj file itself as an Xml.Document LST with MSBuildProject marker.
            // The in-process restore during solution loading produced the in-memory LockFile
            // for each project; fall back to a fresh in-process resolve when absent.
            try
            {
                var content = ReadFilePreservingBom(project.FilePath!);
                var relativePath = Path.GetRelativePath(rootDir, project.FilePath!);
                var xmlParser = new OpenRewrite.Xml.XmlParser();
                var csprojDoc = xmlParser.Parse(content, relativePath);
                var projectFullPath = Path.GetFullPath(project.FilePath!);
                var marker = solutionParser.RestoredLockFiles.TryGetValue(projectFullPath, out var lockFile)
                    ? MSBuildProjectHelper.CreateMarker(csprojDoc, lockFile, Path.GetDirectoryName(projectFullPath)!)
                    : MSBuildProjectHelper.CreateMarker(csprojDoc, rootDir);
                if (marker != null)
                    csprojDoc = csprojDoc.WithMarkers(csprojDoc.Markers.Add(marker));
                _localObjects[csprojDoc.Id.ToString()] = csprojDoc;
                response.Items.Add(new ParseSolutionResponseItem
                {
                    Id = csprojDoc.Id.ToString(),
                    SourceFileType = "org.openrewrite.xml.tree.Xml$Document"
                });
            }
            catch (Exception ex)
            {
                Log.Debug("RPC ParseSolution: failed to parse csproj for {ProjectPath}: {ExType}: {ExMessage}",
                    project.FilePath, ex.GetType().Name, ex.Message);
            }
        }

        // Capture build context files from disk for reattestation
        _buildContext = new DotNetBuildContext();
        _buildContext.CaptureFromDisk(rootDir);

        Log.Debug("RPC ParseSolution: completed, {ItemCount} source files", response.Items.Count);
        return response;
    }

    /// <summary>
    /// Reads a text file while preserving a leading UTF-8 BOM as a `\uFEFF` character in
    /// the returned string. File.ReadAllText silently strips BOMs, which defeats the
    /// XmlParser's BOM detection and causes csproj files to round-trip without their BOM.
    /// </summary>
    private static string ReadFilePreservingBom(string filePath)
    {
        var bytes = File.ReadAllBytes(filePath);
        if (bytes.Length >= 3 && bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF)
        {
            return "\uFEFF" + System.Text.Encoding.UTF8.GetString(bytes, 3, bytes.Length - 3);
        }
        return System.Text.Encoding.UTF8.GetString(bytes);
    }

    /// <summary>
    /// Resolves a path to its canonical form.
    /// On macOS, /var and /tmp are "firmlinks" to /private/var and /private/tmp,
    /// which are not detected as regular symlinks by .NET. Java sends paths through
    /// /var/ while .NET file enumeration resolves through /private/var/, causing
    /// Path.GetRelativePath to fail. This method normalizes both forms.
    /// </summary>
    private static string ResolvePath(string path)
    {
        var fullPath = Path.GetFullPath(path);
        if (OperatingSystem.IsMacOS())
        {
            if (fullPath.StartsWith("/var/"))
                fullPath = "/private" + fullPath;
            else if (fullPath.StartsWith("/tmp/"))
                fullPath = "/private" + fullPath;
        }
        return fullPath;
    }

    [JsonRpcMethod("GetObject", UseSingleObjectParameterDeserialization = true)]
    public Task<List<RpcObjectData>> GetObject(GetObjectRequest request)
    {
        var after = _localObjects.GetValueOrDefault(request.Id);

        if (after == null)
        {
            Log.Debug("RPC GetObject: {Id} not found, returning DELETE", request.Id);
            return Task.FromResult(new List<RpcObjectData>
            {
                new() { State = DELETE },
                new() { State = END_OF_OBJECT }
            });
        }

        // ExecutionContext is sent as a typed shell with no data,
        // matching the JavaScript pattern (empty codec).
        if (after is ExecutionContext)
        {
            return Task.FromResult(new List<RpcObjectData>
            {
                new() { State = ADD, ValueType = "org.openrewrite.InMemoryExecutionContext" },
                new() { State = END_OF_OBJECT }
            });
        }

        var before = _remoteObjects.GetValueOrDefault(request.Id);
        var sw = Stopwatch.StartNew();

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

        try
        {
            sendQueue.Send(after, before, null);
        }
        catch (Exception ex)
        {
            Log.Debug("RPC GetObject: EXCEPTION sending {Id} ({ObjType}): {ExType}: {ExMessage}",
                request.Id, after.GetType().Name, ex.GetType().Name, ex.Message);
            throw new InvalidOperationException(
                $"Failed to send object {request.Id} (type: {after.GetType().Name}): {ex.Message}\n{ex.StackTrace}", ex);
        }
        sendQueue.Put(new RpcObjectData { State = END_OF_OBJECT });
        sendQueue.Flush();

        // Update our understanding of remote's state
        _remoteObjects[request.Id] = after;

        sw.Stop();
        Log.Debug("RPC GetObject: {Id} sent {ItemCount} items ({ElapsedMs}ms)",
            request.Id, allData.Count, sw.Elapsed.TotalMilliseconds.ToString("F0"));

        return Task.FromResult(allData);
    }

    [JsonRpcMethod("Print", UseSingleObjectParameterDeserialization = true)]
    public async Task<string> Print(PrintRequest request)
    {
        Tree tree;
        try
        {
            tree = await GetObjectFromRemoteAsync(request.TreeId, request.SourceFileType);
        }
        catch (Exception ex)
        {
            throw new InvalidOperationException(
                $"Print: Failed to receive tree {request.TreeId} (type: {request.SourceFileType}): {ex.Message}\n{ex.StackTrace}", ex);
        }
        try
        {
            var markerPrinter = request.MarkerPrinter switch
            {
                "SANITIZED" => Core.MarkerPrinter.Sanitized,
                "FENCED" => Core.MarkerPrinter.Fenced,
                "SEARCH_MARKERS_ONLY" => Core.MarkerPrinter.SearchMarkersOnly,
                _ => Core.MarkerPrinter.Default
            };
            var capture = new PrintOutputCapture<int>(0, markerPrinter);
            if (tree is OpenRewrite.Xml.Xml)
                new OpenRewrite.Xml.XmlPrinter<int>().Visit((OpenRewrite.Xml.Xml)tree, capture);
            else
                new CSharpPrinter<int>().Visit(tree, capture);
            return capture.ToString();
        }
        catch (Exception ex)
        {
            throw new InvalidOperationException(
                $"Print: Failed to print tree {request.TreeId} (type: {request.SourceFileType}, treeType: {tree.GetType().Name}): {ex.Message}\n{ex.StackTrace}", ex);
        }
    }

    /// <summary>
    /// Fetches an object from the remote (Java) process by calling GetObject back.
    /// This is the reverse of the local GetObject handler — instead of serializing
    /// our local state, we ask Java to serialize its local state to us.
    /// </summary>
    private async Task<Tree> GetObjectFromRemoteAsync(string id, string? sourceFileType)
    {
        var localObject = _localObjects.GetValueOrDefault(id);

        var q = new RpcReceiveQueue(
            _remoteRefs,
            () => _jsonRpc!.InvokeWithParameterObjectAsync<List<RpcObjectData>>(
                "GetObject",
                new GetObjectRequest { Id = id, SourceFileType = sourceFileType })
                .GetAwaiter().GetResult(),
            sourceFileType,
            TreeCodec.Instance
        );

        object? remoteObject;
        try
        {
            remoteObject = q.Receive(localObject, (Func<object, object>?)null);
        }
        catch (Exception ex)
        {
            throw new InvalidOperationException(
                $"Failed to receive object {id} (type: {sourceFileType}): {ex.Message}\n{ex.StackTrace}", ex);
        }
        var endMarker = q.Take();
        if (endMarker.State != END_OF_OBJECT)
        {
            // Collect remaining items for debugging
            var remaining = new System.Text.StringBuilder();
            remaining.Append($"[0] State={endMarker.State}, Value={endMarker.Value}, ValueType={endMarker.ValueType}");
            for (int i = 1; i < 20; i++)
            {
                try
                {
                    var next = q.Take();
                    remaining.Append($" | [{i}] State={next.State}, Value={next.Value}, ValueType={next.ValueType}");
                    if (next.State == END_OF_OBJECT) break;
                }
                catch { break; }
            }
            throw new InvalidOperationException($"Expected END_OF_OBJECT. Remaining: {remaining}");
        }

        if (remoteObject != null)
        {
            _remoteObjects[id] = remoteObject;
            _localObjects[id] = remoteObject;
        }

        return (Tree)remoteObject!;
    }

    [JsonRpcMethod("GetMarketplace")]
    public Task<List<GetMarketplaceResponseRow>> GetMarketplace()
    {
        var rowByRecipeId = new Dictionary<string, GetMarketplaceResponseRow>();

        foreach (var category in _marketplace.Categories)
        {
            CollectRecipes(rowByRecipeId, category, []);
        }

        return Task.FromResult(rowByRecipeId.Values.ToList());
    }

    private void CollectRecipes(
        Dictionary<string, GetMarketplaceResponseRow> rowByRecipeId,
        RecipeMarketplace.Category category,
        List<CategoryDescriptorDto> parentPath)
    {
        var currentPath = new List<CategoryDescriptorDto>(parentPath)
        {
            new() { DisplayName = category.Descriptor.DisplayName, Description = category.Descriptor.Description }
        };

        foreach (var (descriptor, _) in category.Recipes)
        {
            if (!rowByRecipeId.TryGetValue(descriptor.Name, out var row))
            {
                row = new GetMarketplaceResponseRow
                {
                    Descriptor = RecipeDescriptorDto.FromDescriptor(descriptor),
                    CategoryPaths = [],
                    PackageName = _recipeOrigin.GetValueOrDefault(descriptor.Name)
                };
                rowByRecipeId[descriptor.Name] = row;
            }
            row.CategoryPaths.Add(new List<CategoryDescriptorDto>(currentPath));
        }

        foreach (var child in category.SubCategories)
        {
            CollectRecipes(rowByRecipeId, child, currentPath);
        }
    }

    [JsonRpcMethod("InstallRecipes", UseSingleObjectParameterDeserialization = true)]
    public Task<InstallRecipesResponse> InstallRecipes(InstallRecipesRequest request)
    {
        var beforeCount = _marketplace.AllRecipes().Count;
        string? version = null;          // requested version, as supplied by the caller (never mutated)
        string? resolvedVersion = null;  // concrete version NuGet resolved it to (null off the NuGet path)

        // SystemTextJsonFormatter deserializes the object-typed Recipes payload to a JsonElement:
        // a JSON string is a local assembly path; a JSON object describes a NuGet package.
        var recipesString = request.Recipes switch
        {
            string s => s,
            JsonElement { ValueKind: JsonValueKind.String } je => je.GetString(),
            _ => null,
        };
        var recipesObject = request.Recipes is JsonElement { ValueKind: JsonValueKind.Object } obj
            ? (JsonElement?)obj
            : null;

        if (recipesString != null)
        {
            // Local assembly path
            var absolutePath = Path.GetFullPath(recipesString);
            var context = new PluginLoadContext(absolutePath);
            var assembly = context.LoadFromAssemblyPath(absolutePath);
            CheckVersionCompatibility(assembly);
            ActivateAssembly(assembly, recipesString);
        }
        else if (recipesObject is { } packageObj)
        {
            var packageName = (packageObj.TryGetProperty("packageName", out var pn) ? pn.GetString() : null)
                              ?? throw new ArgumentException("Missing packageName in recipes object");
            version = packageObj.TryGetProperty("version", out var v) ? v.GetString() : null;

            if (File.Exists(packageName))
            {
                var absolutePath = Path.GetFullPath(packageName);
                var context = new PluginLoadContext(absolutePath);
                var assembly = context.LoadFromAssemblyPath(absolutePath);
                CheckVersionCompatibility(assembly);
                ActivateAssembly(assembly, packageName);
            }
            else
            {
                // NuGet install reads as a three-stage pipeline: restore the bundle (which also
                // resolves the concrete version), publish it into the resolved-version dir, then
                // load the plugin assemblies. The requested version may be a concrete pin or a
                // floating/unspecified spec — restore resolves either uniformly.
                var (stagingCsproj, resolved) = RestoreBundle(packageName, version);
                resolvedVersion = resolved;
                var publishDir = PublishBundle(stagingCsproj, resolved);
                var assemblies = LoadPlugin(publishDir);
                var ownAssemblyNames = OwnAssemblyNames(packageName, resolved);
                if (ownAssemblyNames.Count == 0)
                {
                    Log.Warning("No own assemblies found for {Package} {Version} (lib folder missing/empty); no recipes activated", packageName, resolved);
                }
                foreach (var assembly in assemblies)
                {
                    CheckVersionCompatibility(assembly);
                    if (ownAssemblyNames.Contains(assembly.GetName().Name))
                    {
                        ActivateAssembly(assembly, packageName);
                    }
                }
            }
        }
        else
        {
            throw new ArgumentException($"Unexpected recipes type: {request.Recipes?.GetType().Name ?? "null"}");
        }

        return Task.FromResult(new InstallRecipesResponse
        {
            RecipesInstalled = _marketplace.AllRecipes().Count - beforeCount,
            Version = resolvedVersion ?? version
        });
    }

    private void ActivateAssembly(Assembly assembly, string? packageName = null)
    {
        var before = packageName == null ? null
            : new HashSet<string>(_marketplace.AllRecipes().Select(r => r.Name));

        Type[] exportedTypes;
        try
        {
            exportedTypes = assembly.GetExportedTypes();
        }
        catch (ReflectionTypeLoadException ex)
        {
            Log.Warning("Could not load all types from {Assembly}: {Errors}",
                assembly.GetName().Name,
                string.Join("; ", ex.LoaderExceptions
                    .Where(e => e != null)
                    .Select(e => e!.Message)));
            exportedTypes = ex.Types.Where(t => t != null).ToArray()!;
        }

        foreach (var type in exportedTypes)
        {
            if (typeof(IRecipeActivator).IsAssignableFrom(type) && !type.IsAbstract && !type.IsInterface)
            {
                var activator = (IRecipeActivator)Activator.CreateInstance(type)!;
                activator.Activate(_marketplace);
            }
        }

        if (packageName != null)
        {
            foreach (var name in _marketplace.AllRecipes().Select(r => r.Name))
            {
                if (!before!.Contains(name))
                {
                    _recipeOrigin[name] = packageName;
                }
            }
        }
    }

    // Sanitize a path segment (package id or version) for use as a directory name.
    private static string Sanitize(string s) => string.Join("_", s.Split(Path.GetInvalidFileNameChars()));

    private string EnsureRecipesProject(string packageName, string? version)
    {
        // Use the caller-supplied recipe install directory when provided (so a co-located
        // NuGet.config is found by dotnet's project-directory config walk); otherwise fall
        // back to a temp directory.
        var root = _recipeInstallDir
            ?? Path.Combine(Path.GetTempPath(), "rewrite-recipes");
        // Mirror NuGet's global-packages layout: <root>/<id>/<version>/, versions as
        // immutable siblings. Never deleted — old versions accumulate (no cleanup),
        // matching ~/.nuget and existing Moderne/OpenRewrite practice.
        var bundleDir = Path.Combine(root, Sanitize(packageName), Sanitize(version ?? "unversioned"));
        var csprojPath = Path.Combine(bundleDir, "Recipes.csproj");
        _recipesProjectDir = bundleDir;

        // Reuse an already-published version dir (idempotent, like NuGet reusing an
        // already-extracted package). Snapshots carry timestamps in their version string,
        // so each publish lands in a fresh dir.
        if (File.Exists(csprojPath))
        {
            return csprojPath;
        }
        Directory.CreateDirectory(bundleDir);

        File.WriteAllText(csprojPath, """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
                <EnableDefaultCompileItems>false</EnableDefaultCompileItems>
              </PropertyGroup>
            </Project>
            """);

        // For local cross-repo development, make the local NuGet feed additive to
        // whatever config already lives in the project dir. A caller (e.g. the Moderne
        // CLI) may have written its own nuget.config there — possibly an exclusive
        // configured feed — so we must not clobber it: append only the local feed when
        // a config is present, and create a standalone dev config that adds only the
        // local feed (never nuget.org, so it merges with the user/machine config's
        // default source) when none exists. No-ops in production, where local-feed is absent.
        var localFeed = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            ".nuget", "local-feed");
        if (Directory.Exists(localFeed))
        {
            var nugetConfig = Path.Combine(bundleDir, "nuget.config");
            var existing = File.Exists(nugetConfig) ? File.ReadAllText(nugetConfig) : null;
            File.WriteAllText(nugetConfig, BuildRecipesNuGetConfig(existing, localFeed));
        }

        return csprojPath;
    }

    private static HashSet<string> OwnAssemblyNames(string packageName, string? version, string? packagesRoot = null)
    {
        // The package's own runtime assemblies are those shipped in its lib/<tfm> folder
        // in the global packages cache (the directly-contributed boundary). All other
        // published DLLs are transitive dependencies and must not be activated.
        packagesRoot ??= Environment.GetEnvironmentVariable("NUGET_PACKAGES")
            ?? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".nuget", "packages");
        // NuGet's v3 global-packages layout lowercases both the package id and the (normalized)
        // version in the folder path, so match that or the lib dir won't be found on a
        // case-sensitive filesystem (Linux).
        var libDir = Path.Combine(packagesRoot, packageName.ToLowerInvariant(), (version ?? "").ToLowerInvariant(), "lib");
        var names = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        if (Directory.Exists(libDir))
        {
            foreach (var dll in Directory.GetFiles(libDir, "*.dll", SearchOption.AllDirectories))
            {
                names.Add(Path.GetFileNameWithoutExtension(dll));
            }
        }
        return names;
    }

    /// <summary>
    /// Test seam for <see cref="OwnAssemblyNames"/>: exposes the private helper
    /// with an explicit packages root so unit tests can use a synthetic temp directory
    /// without a real NuGet install.
    /// </summary>
    internal static HashSet<string> OwnAssemblyNamesForTest(string packageName, string? version, string packagesRoot)
        => OwnAssemblyNames(packageName, version, packagesRoot);

    /// <summary>
    /// Produces the recipe project's <c>nuget.config</c> with the local development
    /// feed present. When <paramref name="existingConfigXml"/> is null/empty, creates a
    /// standalone config that adds only the local feed — never nuget.org — so it merges
    /// with (rather than overrides) the user/machine NuGet configuration that supplies the
    /// environment's default source. Otherwise the caller already wrote a config (possibly
    /// an exclusive configured feed): only the local feed is appended to
    /// <c>&lt;packageSources&gt;</c>, preserving the caller's sources and any
    /// <c>&lt;clear/&gt;</c>, and idempotently (no duplicate if already present).
    /// </summary>
    internal static string BuildRecipesNuGetConfig(string? existingConfigXml, string localFeedPath)
    {
        if (string.IsNullOrWhiteSpace(existingConfigXml))
        {
            return $"""
                <?xml version="1.0" encoding="utf-8"?>
                <configuration>
                  <packageSources>
                    <add key="local-feed" value="{localFeedPath}" />
                  </packageSources>
                </configuration>
                """;
        }

        var doc = XDocument.Parse(existingConfigXml);
        var configuration = doc.Element("configuration")
                            ?? throw new InvalidOperationException("nuget.config is missing its <configuration> root");
        var packageSources = configuration.Element("packageSources");
        if (packageSources == null)
        {
            packageSources = new XElement("packageSources");
            configuration.Add(packageSources);
        }

        bool alreadyPresent = packageSources.Elements("add").Any(e =>
            string.Equals((string?)e.Attribute("value"), localFeedPath, StringComparison.OrdinalIgnoreCase));
        if (!alreadyPresent)
        {
            packageSources.Add(new XElement("add",
                new XAttribute("key", "local-feed"),
                new XAttribute("value", localFeedPath)));
        }

        var declaration = doc.Declaration ?? new XDeclaration("1.0", "utf-8", null);
        return declaration + Environment.NewLine + doc.Root!.ToString();
    }

    private static void RunDotnet(string arguments)
    {
        var psi = new ProcessStartInfo("dotnet", arguments)
        {
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };

        using var process = System.Diagnostics.Process.Start(psi)
                            ?? throw new InvalidOperationException("Failed to start dotnet process");

        var stdout = process.StandardOutput.ReadToEnd();
        var stderr = process.StandardError.ReadToEnd();
        process.WaitForExit();

        if (process.ExitCode != 0)
        {
            throw new InvalidOperationException(
                $"dotnet {arguments} failed (exit code {process.ExitCode}):\n{stderr}\n{stdout}");
        }
    }

    /// <summary>
    /// Restore stage: restore a NuGet recipe bundle in a staging project under the install root
    /// (so the caller's nuget.config is found by dotnet's upward project-directory config walk),
    /// and return the staging project plus the resolved concrete version.
    /// </summary>
    private (string Csproj, string ResolvedVersion) RestoreBundle(string packageName, string? requestedVersion)
    {
        var stagingCsproj = EnsureRecipesProject(packageName, ".staging");
        var addArgs = $"add \"{stagingCsproj}\" package {packageName}";
        if (requestedVersion != null)
            addArgs += $" --version {requestedVersion}";
        RunDotnet(addArgs);

        // dotnet add package preserves the requested constraint verbatim in the csproj;
        // the resolved concrete version lives in obj/project.assets.json.
        var resolvedVersion = MSBuildProjectHelper.GetResolvedPackageVersion(
            Path.GetDirectoryName(stagingCsproj)!, packageName);
        return (stagingCsproj, resolvedVersion);
    }

    /// <summary>
    /// Publish stage: publish a restored staging project into its permanent, resolved-version-keyed
    /// sibling dir (<c>&lt;root&gt;/&lt;id&gt;/&lt;resolvedVersion&gt;</c>), reusing it if already
    /// present. Returns the publish output directory.
    /// </summary>
    private static string PublishBundle(string stagingCsproj, string resolvedVersion)
    {
        var bundleRoot = Directory.GetParent(Path.GetDirectoryName(stagingCsproj)!)!.FullName; // <root>/<id>
        var publishDir = Path.Combine(bundleRoot, Sanitize(resolvedVersion));
        // The publish output is an immutable sibling: publish only when this resolved version
        // isn't there yet. --no-restore reuses the staging project's restore.
        if (!File.Exists(Path.Combine(publishDir, "Recipes.dll")))
        {
            RunDotnet($"publish \"{stagingCsproj}\" --no-restore -c Release -o \"{publishDir}\"");
        }
        return publishDir;
    }

    /// <summary>
    /// Load stage: load the recipe-bearing assemblies from a publish output directory into an
    /// isolated <see cref="PluginLoadContext"/>. Because the NuGet package name may not match the
    /// assembly name, we scan all non-host DLLs in the publish output for
    /// <see cref="IRecipeActivator"/> implementations.
    /// </summary>
    private List<Assembly> LoadPlugin(string publishDir)
    {
        // Use the Recipes.deps.json (from the staging project) for the dependency resolver
        var depsJson = Path.Combine(publishDir, "Recipes.deps.json");
        if (!File.Exists(depsJson))
        {
            Log.Warning("No .deps.json found in publish output at {PublishDir}", publishDir);
        }

        // The staging project's main DLL is the anchor for AssemblyDependencyResolver
        var anchorDll = Path.Combine(publishDir, "Recipes.dll");
        if (!File.Exists(anchorDll))
        {
            // Fallback: pick any DLL that has a matching .deps.json
            anchorDll = Directory.GetFiles(publishDir, "*.dll").FirstOrDefault()
                        ?? throw new InvalidOperationException(
                            $"No DLLs found in publish output at {publishDir}");
        }

        var context = new PluginLoadContext(anchorDll);

        // Only load DLLs that are not already loaded in the host and not well-known
        // framework/SDK assemblies. The PluginLoadContext handles lazy resolution of
        // transitive dependencies via AssemblyDependencyResolver.
        var hostAssemblyNames = new HashSet<string>(
            AssemblyLoadContext.Default.Assemblies
                .Select(a => a.GetName().Name!)
                .Where(n => n != null),
            StringComparer.OrdinalIgnoreCase);

        var loadedAssemblies = new List<Assembly>();
        foreach (var dll in Directory.GetFiles(publishDir, "*.dll"))
        {
            var assemblyFileName = Path.GetFileNameWithoutExtension(dll);

            // Skip assemblies already loaded in the host
            if (hostAssemblyNames.Contains(assemblyFileName))
                continue;

            // Skip well-known framework/SDK assemblies that don't contain recipes
            if (IsFrameworkAssembly(assemblyFileName))
                continue;

            try
            {
                var assembly = context.LoadFromAssemblyPath(dll);
                loadedAssemblies.Add(assembly);
                Log.Debug("Plugin context loaded {Assembly} from {Path}", assemblyFileName, dll);
            }
            catch (Exception ex)
            {
                Log.Warning("Failed to load {Assembly} from plugin publish output: {Error}",
                    assemblyFileName, ex.Message);
            }
        }

        return loadedAssemblies;
    }

    private static bool IsFrameworkAssembly(string assemblyName)
    {
        return assemblyName.StartsWith("System.", StringComparison.OrdinalIgnoreCase) ||
               assemblyName.StartsWith("Microsoft.", StringComparison.OrdinalIgnoreCase) ||
               assemblyName.StartsWith("NuGet.", StringComparison.OrdinalIgnoreCase) ||
               assemblyName.StartsWith("xunit", StringComparison.OrdinalIgnoreCase) ||
               assemblyName.StartsWith("testhost", StringComparison.OrdinalIgnoreCase) ||
               assemblyName.StartsWith("coverlet", StringComparison.OrdinalIgnoreCase);
    }

    /// <summary>
    /// Verify that the plugin's OpenRewrite.CSharp dependency version is compatible with the host.
    /// Logs a warning if the plugin was compiled against a newer version than the host.
    /// </summary>
    private static void CheckVersionCompatibility(Assembly pluginAssembly)
    {
        var hostAssembly = typeof(RewriteRpcServer).Assembly;
        var hostVersion = hostAssembly.GetName().Version;

        var openRewriteRef = pluginAssembly.GetReferencedAssemblies()
            .FirstOrDefault(a => string.Equals(a.Name, "OpenRewrite.CSharp",
                StringComparison.OrdinalIgnoreCase));

        if (openRewriteRef?.Version == null || hostVersion == null)
            return;

        var pluginRefVersion = openRewriteRef.Version;
        if (pluginRefVersion.Major != hostVersion.Major ||
            pluginRefVersion.Minor > hostVersion.Minor)
        {
            Log.Warning(
                "Plugin {Plugin} references OpenRewrite.CSharp {PluginVersion} " +
                "but host is {HostVersion}. This may cause runtime errors",
                pluginAssembly.GetName().Name, pluginRefVersion, hostVersion);
        }
    }

    private static readonly string[] Languages = [
        "org.openrewrite.csharp.tree.Cs$CompilationUnit",
        "org.openrewrite.xml.tree.Xml$Document",
    ];

    [JsonRpcMethod("GetLanguages")]
    public Task<string[]> GetLanguages()
    {
        return Task.FromResult(Languages);
    }

    [JsonRpcMethod("Generate", UseSingleObjectParameterDeserialization = true)]
    public Task<GenerateResponse> Generate(GenerateRequest request)
    {
        if (!_preparedRecipes.TryGetValue(request.Id, out var recipe))
        {
            throw new InvalidOperationException($"Prepared recipe not found: {request.Id}");
        }

        var response = new GenerateResponse();

        if (recipe is IScanningRecipe scanning)
        {
            var ctx = GetOrCreateExecutionContext(request.P);
            var acc = GetOrCreateAccumulator(request.Id, scanning, ctx);

            var generated = scanning.Generate(acc, ctx);

            foreach (var g in generated)
            {
                var id = g.Id.ToString();
                _localObjects[id] = g;
                response.Ids.Add(id);

                var javaTypeName = RpcSendQueue.ToJavaTypeName(g.GetType());
                if (javaTypeName == null)
                {
                    Log.Warning("Generate: No Java type mapping for {CSharpType}, using fallback",
                        g.GetType().FullName);
                    javaTypeName = "org.openrewrite.csharp.tree.Cs$CompilationUnit";
                }
                response.SourceFileTypes.Add(javaTypeName);
            }
        }

        return Task.FromResult(response);
    }

    [JsonRpcMethod("PrepareRecipe", UseSingleObjectParameterDeserialization = true)]
    public Task<PrepareRecipeResponse> PrepareRecipe(PrepareRecipeRequest request)
    {
        var found = _marketplace.FindRecipe(request.Id);
        if (found == null)
        {
            // The host re-prepares every sub-recipe of a composite by id while building
            // RpcRecipe.getRecipeList(). A sub-recipe that delegates to a Java recipe is not in
            // this marketplace, so a miss means the host owns this recipe: answer with delegatesTo
            // so the host resolves the id locally (the Java recipe is on its classpath) rather than
            // failing with "Recipe not found".
            var delegateId = Guid.NewGuid().ToString();
            return Task.FromResult(new PrepareRecipeResponse
            {
                Id = delegateId,
                Descriptor = new RecipeDescriptorDto
                {
                    Name = request.Id,
                    DisplayName = request.Id,
                    InstanceName = request.Id
                },
                EditVisitor = $"edit:{delegateId}",
                DelegatesTo = new DelegatesTo
                {
                    RecipeName = request.Id,
                    Options = request.Options ?? new()
                }
            });
        }

        var (descriptor, recipe) = found.Value;
        if (recipe == null)
        {
            throw new InvalidOperationException($"Recipe {request.Id} has no live instance (installed without constructor)");
        }

        return Task.FromResult(PrepareInstance(recipe, request.Options));
    }

    /// <summary>
    /// Prepares a single recipe instance (optionally applying options) and recursively
    /// prepares the full child tree, storing every node in <see cref="_preparedRecipes"/>.
    /// A child that is <see cref="IDelegatesTo"/> carries only <c>DelegatesTo</c> and
    /// no children; all other children have their own <c>RecipeList</c> populated.
    /// </summary>
    private PrepareRecipeResponse PrepareInstance(Recipe recipe, Dictionary<string, object?>? options)
    {
        // If options are provided, create a new instance with options applied.
        if (options is { Count: > 0 })
        {
            recipe = InstantiateWithOptions(recipe.GetType(), options);
        }

        // Validate required options on the instantiated recipe — the root against the caller's
        // options, and every child against the values its parent set in GetRecipeList(). Because
        // PrepareInstance recurses, this covers the whole tree, and it's the only place the C# tree
        // gets validated: declarative recipes bundled in an artifact often ship without a test that
        // runs validateAll, so this is the safety net against executing a broken recipe. Delegating
        // recipes forward to a Java recipe that validates its own options, so they are skipped here.
        if (recipe is not IDelegatesTo)
        {
            var descriptor = recipe.GetDescriptor();
            foreach (var option in descriptor.Options)
            {
                if (option.Required && option.Value is null)
                {
                    throw new ArgumentException(
                        $"Missing required option `{option.Name}` for recipe `{descriptor.Name}`.");
                }
            }
        }

        var id = Guid.NewGuid().ToString();
        _preparedRecipes[id] = recipe;

        var response = new PrepareRecipeResponse
        {
            Id = id,
            Descriptor = RecipeDescriptorDto.FromDescriptor(recipe.GetDescriptor()),
            EditVisitor = $"edit:{id}",
            ScanVisitor = recipe is IScanningRecipe ? $"scan:{id}" : null
        };

        if (recipe is IDelegatesTo del)
        {
            // Cross-ecosystem child: host resolves locally; no C# child tree.
            response.DelegatesTo = new DelegatesTo
            {
                RecipeName = del.JavaRecipeName,
                Options = del.Options
            };
        }
        else
        {
            OptimizePreconditions(recipe, response);
            // Whole-tree preparation: children are real instances in this recipe's own ALC.
            response.RecipeList = recipe.GetRecipeList()
                .Select(child => PrepareInstance(child, null))
                .ToList();
        }

        return response;
    }

    /// <summary>
    /// Inspects a recipe's visitor to extract preconditions that Java can evaluate
    /// before sending files via RPC. Also adds a FindTreesOfType precondition based
    /// on the visitor type so Java only sends compatible files.
    /// </summary>
    private void OptimizePreconditions(Recipe recipe, PrepareRecipeResponse response)
    {
        try
        {
            var visitor = recipe.GetVisitor();

            var innerVisitor = visitor;
            if (visitor is Check check)
            {
                // Try to emit the precondition's wire identity so the Java
                // host can evaluate it locally and skip the visit RPC for
                // non-matching files. RecipeCheck is the only shape we can
                // serialize today (recipe identity); Check wrapping a bare
                // visitor or a composite has no recipe-name to point Java
                // at, so we fall through and let the gate run C#-side.
                if (check is RecipeCheck recipeCheck && _preparedRecipes.Values.Contains(recipeCheck.Recipe))
                {
                    var entry = ConditionWireEntry(recipeCheck.Precondition);
                    if (entry != null)
                    {
                        response.EditPreconditions.Add(entry);
                    }
                }
                else
                {
                    var entry = ConditionWireEntry(check.Precondition);
                    if (entry != null)
                    {
                        response.EditPreconditions.Add(entry);
                    }
                }
                innerVisitor = check.Visitor;
            }

            // Add tree type precondition so Java only sends files this visitor can handle
            if (innerVisitor is CSharpVisitor<ExecutionContext>)
            {
                response.EditPreconditions.Add(new Precondition
                {
                    VisitorName = "org.openrewrite.rpc.internal.FindTreesOfType",
                    VisitorOptions = new() { ["type"] = "org.openrewrite.csharp.tree.Cs" }
                });
            }
        }
        catch
        {
            // Some recipes may fail during GetVisitor() — skip precondition detection
        }
    }

    /// <summary>
    /// Translate a precondition condition (operand) to a wire entry.
    /// Composites recurse into <c>op</c> + <c>operands</c>; leaves carry
    /// <c>visitorName</c>. Returns <c>null</c> when the condition can't be
    /// serialized (e.g. an opaque local visitor with no recipe identity);
    /// the caller leaves the wrapper intact so the gate runs C#-side.
    /// </summary>
    private Precondition? ConditionWireEntry(ITreeVisitor<ExecutionContext> condition)
    {
        if (condition is IComposite composite)
        {
            var operands = new List<Precondition>(composite.Operands.Count);
            foreach (var operand in composite.Operands)
            {
                var nested = ConditionWireEntry(operand);
                if (nested == null)
                {
                    return null;
                }
                operands.Add(nested);
            }
            return new Precondition { Op = composite.Op, Operands = operands };
        }
        // Common case: helpers like UsesMethod / UsesType return a
        // lightweight RecipeRef so the recipe author can declare a
        // precondition without firing an RPC at GetVisitor() time.
        // Java's PreparedRecipeCache.instantiateVisitor constructs the
        // named recipe via Jackson and uses its visitor.
        if (condition is RecipeRef recipeRef)
        {
            return new Precondition
            {
                VisitorName = recipeRef.RecipeName,
                VisitorOptions = recipeRef.Options.ToDictionary(kvp => kvp.Key, kvp => kvp.Value!)
            };
        }
        // Leaf with no wire identity — the Java host can't evaluate it
        // remotely. Leave the wrapper intact so the gate runs C#-side.
        return null;
    }

    private static Recipe InstantiateWithOptions(Type recipeType, Dictionary<string, object?> options)
    {
        var recipe = (Recipe)Activator.CreateInstance(recipeType)!;
        foreach (var (key, value) in options)
        {
            var prop = recipeType.GetProperty(key, BindingFlags.Public | BindingFlags.Instance | BindingFlags.IgnoreCase);
            if (prop != null && prop.CanWrite)
            {
                prop.SetValue(recipe, ConvertOptionValue(value, prop.PropertyType));
            }
        }
        return recipe;
    }

    /// <summary>
    /// Coerce a recipe option value received over the wire to its target property type.
    /// The streaming <c>SystemTextJsonFormatter</c> deserializes <c>object</c>-typed values
    /// to <see cref="JsonElement"/>, which is not <see cref="IConvertible"/> — so a plain
    /// <see cref="Convert.ChangeType(object?, Type)"/> throws. Deserialize the fragment
    /// straight to the property type instead. A non-<see cref="JsonElement"/> value (an
    /// in-process call or a test passing a direct CLR value) keeps the prior conversion.
    /// </summary>
    private static object? ConvertOptionValue(object? value, Type targetType)
    {
        if (value is JsonElement element)
        {
            return element.Deserialize(targetType, RpcJson.Options);
        }
        if (value is null || targetType.IsInstanceOfType(value))
        {
            return value;
        }
        var conversionType = Nullable.GetUnderlyingType(targetType) ?? targetType;
        return Convert.ChangeType(value, conversionType);
    }

    [JsonRpcMethod("Visit", UseSingleObjectParameterDeserialization = true)]
    public async Task<VisitResponse> Visit(VisitRequest request)
    {
        // Skip source file types that the C# server can't handle
        if (request.SourceFileType != null &&
            !request.SourceFileType.StartsWith("org.openrewrite.csharp.") &&
            !request.SourceFileType.StartsWith("org.openrewrite.xml."))
        {
            return new VisitResponse { Modified = false };
        }

        // Parse visitor name: "edit:<recipeId>" or "scan:<recipeId>"
        var parts = request.VisitorName.Split(':', 2);
        if (parts.Length != 2)
        {
            throw new ArgumentException($"Invalid visitor name format: {request.VisitorName}");
        }

        var phase = parts[0]; // "edit" or "scan"
        var recipeId = parts[1];

        if (!_preparedRecipes.TryGetValue(recipeId, out var recipe))
        {
            throw new InvalidOperationException($"Prepared recipe not found: {recipeId}");
        }

        // Fetch tree from the remote (Java) process
        CaptureRefCheckpoint(request.TreeId);
        var tree = await GetObjectFromRemoteAsync(request.TreeId, request.SourceFileType);

        if (phase != "scan" && phase != "edit")
        {
            throw new ArgumentException($"Unknown visitor phase: {phase}");
        }

        var ctx = GetOrCreateExecutionContext(request.PId);
        ITreeVisitor<ExecutionContext> visitor;

        if (recipe is IScanningRecipe scanning)
        {
            var acc = GetOrCreateAccumulator(recipeId, scanning, ctx);
            visitor = phase == "scan" ? scanning.Scanner(acc) : scanning.Editor(acc);
        }
        else
        {
            visitor = recipe.GetVisitor();
        }

        var result = visitor.Visit(tree, ctx);

        var modified = !ReferenceEquals(tree, result);
        if (result == null)
        {
            _localObjects.TryRemove(request.TreeId, out _);
        }
        else if (modified)
        {
            _localObjects[request.TreeId] = result;
        }

        return new VisitResponse { Modified = modified };
    }

    [JsonRpcMethod("BatchVisit", UseSingleObjectParameterDeserialization = true)]
    public async Task<BatchVisitResponse> BatchVisit(BatchVisitRequest request)
    {
        // Skip source file types that the C# server can't handle
        if (request.SourceFileType != null &&
            !request.SourceFileType.StartsWith("org.openrewrite.csharp.") &&
            !request.SourceFileType.StartsWith("org.openrewrite.xml."))
        {
            return new BatchVisitResponse
            {
                Results = request.Visitors.Select(_ => new BatchVisitResult
                {
                    Modified = false,
                    Deleted = false
                }).ToList()
            };
        }

        var sw = Stopwatch.StartNew();
        CaptureRefCheckpoint(request.TreeId);
        var tree = await GetObjectFromRemoteAsync(request.TreeId, request.SourceFileType);
        var fetchMs = sw.ElapsedMilliseconds;

        var ctx = GetOrCreateExecutionContext(request.PId);
        var results = new List<BatchVisitResult>();
        var knownIds = CollectSearchResultIds(tree);
        var collectMs = sw.ElapsedMilliseconds - fetchMs;

        long visitMs = 0, searchCollectMs = 0;
        int modifiedCount = 0;

        foreach (var item in request.Visitors)
        {
            // Parse visitor name and instantiate
            var parts = item.Visitor.Split(':', 2);
            if (parts.Length != 2)
                throw new ArgumentException($"Invalid visitor name format: {item.Visitor}");

            var phase = parts[0];
            var recipeId = parts[1];

            if (!_preparedRecipes.TryGetValue(recipeId, out var recipe))
                throw new InvalidOperationException($"Prepared recipe not found: {recipeId}");

            ITreeVisitor<ExecutionContext> visitor;
            if (recipe is IScanningRecipe scanning)
            {
                var acc = GetOrCreateAccumulator(recipeId, scanning, ctx);
                visitor = phase == "scan" ? scanning.Scanner(acc) : scanning.Editor(acc);
            }
            else
            {
                visitor = recipe.GetVisitor();
            }

            var visitStart = sw.ElapsedMilliseconds;
            var result = visitor.Visit(tree, ctx);
            visitMs += sw.ElapsedMilliseconds - visitStart;

            var modified = !ReferenceEquals(tree, result);
            var deleted = result == null;
            if (modified) modifiedCount++;


            // Diff SearchResult IDs against the running set
            var searchStart = sw.ElapsedMilliseconds;
            List<string> searchResultIds;
            if (deleted)
            {
                searchResultIds = [];
            }
            else if (!modified)
            {
                searchResultIds = [];
            }
            else
            {
                var afterIds = CollectSearchResultIds(result!);
                searchResultIds = afterIds.Except(knownIds).ToList();
                knownIds.UnionWith(searchResultIds);
            }
            searchCollectMs += sw.ElapsedMilliseconds - searchStart;

            results.Add(new BatchVisitResult
            {
                Modified = modified,
                Deleted = deleted,
                HasNewMessages = false,
                SearchResultIds = searchResultIds
            });

            if (deleted)
            {
                _localObjects.TryRemove(request.TreeId, out _);
                break;
            }

            if (modified)
            {
                tree = result!;
            }
        }

        sw.Stop();
        if (sw.ElapsedMilliseconds > 100)
        {
            Log.Debug("BatchVisit: {TreeId} {Visitors} visitors, {Modified} modified, " +
                "fetch={FetchMs}ms visit={VisitMs}ms searchCollect={SearchMs}ms total={TotalMs}ms",
                request.TreeId, request.Visitors.Count, modifiedCount,
                fetchMs, visitMs, searchCollectMs, sw.ElapsedMilliseconds);
        }

        // Store final tree in localObjects
        if (tree != null)
        {
            _localObjects[tree.Id.ToString()] = tree;
            if (tree.Id.ToString() != request.TreeId)
            {
                _localObjects[request.TreeId] = tree;
            }
        }

        return new BatchVisitResponse { Results = results };
    }

    private static HashSet<string> CollectSearchResultIds(Tree? tree)
    {
        var ids = new HashSet<string>();
        if (tree == null) return ids;

        new SearchResultCollector(ids).Visit(tree, 0);
        return ids;
    }

    private class SearchResultCollector(HashSet<string> ids) : CSharpVisitor<int>
    {
        public override Marker VisitMarker(Marker marker, int p)
        {
            if (marker is SearchResult sr)
            {
                ids.Add(sr.Id.ToString());
            }
            return base.VisitMarker(marker, p);
        }
    }

    public static async Task RunAsync(RecipeMarketplace? marketplace = null,
        string? recipeInstallDir = null,
        string? metricsCsv = null,
        CancellationToken cancellationToken = default)
    {
        marketplace ??= new RecipeMarketplace();

        // Scan current assembly for IRecipeActivator implementations
        foreach (var type in Assembly.GetExecutingAssembly().GetExportedTypes())
        {
            if (typeof(IRecipeActivator).IsAssignableFrom(type) && !type.IsAbstract && !type.IsInterface)
            {
                var activator = (IRecipeActivator)Activator.CreateInstance(type)!;
                activator.Activate(marketplace);
            }
        }

        using var inputStream = Console.OpenStandardInput();
        using var outputStream = Console.OpenStandardOutput();

        // Stream the JSON-RPC envelope with System.Text.Json (Utf8JsonWriter/Utf8JsonReader)
        // instead of Newtonsoft's JToken-DOM formatter. The wire format stays JSON and matches
        // Java's expectations (camelCase property names, string enum values, omitted nulls) via
        // the shared RpcJson.Options. See RpcJson for why this is far cheaper on the .NET side
        // (no per-message DOM, no per-value converter scan under lock, far less GC pressure).
        var formatter = new SystemTextJsonFormatter
        {
            JsonSerializerOptions = RpcJson.Options,
        };

        var server = new RewriteRpcServer(marketplace, recipeInstallDir);

        // Wrap the handler so each dispatched request records timing + cache residency
        // (local/remote/refs) — flat with per-file Evict, ramping without.
        IJsonRpcMessageHandler handler = new HeaderDelimitedMessageHandler(outputStream, inputStream, formatter);
        RpcMetricsWriter? metrics = null;
        if (!string.IsNullOrEmpty(metricsCsv))
        {
            metrics = new RpcMetricsWriter(metricsCsv, () =>
                (server._localObjects.Count, server._remoteObjects.Count,
                    server._localRefs.Count + server._remoteRefs.Count));
            handler = new MetricsMessageHandler(handler, metrics);
        }

        using var jsonRpc = new StringErrorDataJsonRpc(handler);
        server._jsonRpc = jsonRpc;
        _current = server;
        // Allow concurrent request dispatch so reentrant callbacks don't deadlock.
        // Without this, the default NonConcurrentSynchronizationContext serializes
        // dispatch, blocking incoming GetObject requests while BatchVisit is in progress.
        jsonRpc.SynchronizationContext = null;
        jsonRpc.AddLocalRpcTarget(server);
        jsonRpc.StartListening();

        try
        {
            await jsonRpc.Completion.WaitAsync(cancellationToken);
        }
        finally
        {
            _current = null;
            metrics?.Dispose();
        }
    }

    /// <summary>
    /// Parses source content, handling .csproj files locally and delegating others to Java.
    /// </summary>
    public SourceFile ParseOnRemote(string sourcePath, string content, string? sourceFileType = null)
    {
        // Parse .csproj files locally using C# XmlParser + MSBuildProject marker
        var csprojParser = new Xml.CsprojParser();
        if (csprojParser.Accept(sourcePath))
        {
            var doc = csprojParser.Parse(content, sourcePath);
            var id = doc.Id.ToString();
            _localObjects[id] = doc;
            return doc;
        }

        var response = _jsonRpc!.InvokeWithParameterObjectAsync<List<string>>(
            "Parse",
            new ParseRequest
            {
                Inputs = [new ParseInput { Text = content, SourcePath = sourcePath }]
            }
        ).GetAwaiter().GetResult();

        if (response.Count == 0)
            throw new InvalidOperationException($"Parse returned no results for {sourcePath}");

        var id2 = response[0];
        return (SourceFile)GetObjectFromRemoteAsync(id2, sourceFileType).GetAwaiter().GetResult();
    }

    /// <summary>
    /// Asks the Java peer to prepare a recipe by name and options.
    /// Returns a response containing the edit visitor name for use with VisitOnRemoteAsync.
    /// </summary>
    public PrepareRecipeResponse PrepareRecipeOnRemote(string recipeId, Dictionary<string, object?>? options = null)
    {
        return _jsonRpc!.InvokeWithParameterObjectAsync<PrepareRecipeResponse>(
            "PrepareRecipe",
            new PrepareRecipeRequest { Id = recipeId, Options = options ?? new() }
        ).GetAwaiter().GetResult();
    }

    /// <summary>
    /// Runs a prepared visitor on the Java peer. The tree must already be in _localObjects
    /// so Java can fetch it via the GetObject callback.
    /// Returns the (possibly modified) tree.
    /// </summary>
    public Tree VisitOnRemote(string visitorName, string treeId, string? sourceFileType,
        string? pId = null)
    {
        var response = _jsonRpc!.InvokeWithParameterObjectAsync<VisitResponse>(
            "Visit",
            new VisitRequest { VisitorName = visitorName, TreeId = treeId, SourceFileType = sourceFileType, PId = pId }
        ).GetAwaiter().GetResult();

        Log.Debug("RPC VisitOnRemote: {VisitorName} on {TreeId} => modified={Modified}",
            visitorName, treeId, response.Modified);

        if (response.Modified)
        {
            return GetObjectFromRemoteAsync(treeId, sourceFileType).GetAwaiter().GetResult();
        }

        return (Tree)_localObjects[treeId]!;
    }

    [JsonRpcMethod("SetDataTableStore", UseSingleObjectParameterDeserialization = true)]
    public Task<bool> SetDataTableStore(SetDataTableStoreRequest request)
    {
        _configuredDataTableStore = request.ToDataTableStore();
        return Task.FromResult(true);
    }

    /// <summary>
    /// JSON-RPC handler and public API for resetting all cached state.
    /// When called via JSON-RPC from the remote process, clears local caches.
    /// When called directly (e.g., from test infrastructure), also sends Reset
    /// to the remote process to clear its caches.
    /// </summary>
    [JsonRpcMethod("Reset")]
    public Task<bool> Reset()
    {
        ClearLocalState();
        return Task.FromResult(true);
    }

    /// <summary>
    /// Resets all cached state in both the local and remote RPC processes.
    /// Call between operations that don't share state (e.g., between tests)
    /// to prevent unbounded memory growth from accumulated objects.
    /// </summary>
    [JsonRpcIgnore]
    public void ResetAll()
    {
        // Send reset to remote process (which clears its caches)
        _jsonRpc!.InvokeWithParameterObjectAsync<bool>("Reset", new { }).GetAwaiter().GetResult();

        // Clear local caches
        ClearLocalState();
    }

    private void ClearLocalState()
    {
        _localObjects.Clear();
        _remoteObjects.Clear();
        _localRefs.Clear();
        _remoteRefs.Clear();
        _refCheckpoints.Clear();
        _preparedRecipes.Clear();
        _recipeAccumulators.Clear();
        _executionContexts.Clear();
    }

    /// <summary>
    /// Records the ref high-water before a source file is first visited (first visit wins), so
    /// <see cref="Evict"/> can roll back exactly the refs that file introduced.
    /// </summary>
    private void CaptureRefCheckpoint(string treeId)
    {
        _refCheckpoints.GetOrAdd(treeId, _ =>
        {
            var remoteMax = -1;
            foreach (var key in _remoteRefs.Keys)
            {
                if (key > remoteMax)
                {
                    remoteMax = key;
                }
            }
            return (_localRefs.Count, remoteMax);
        });
    }

    /// <summary>
    /// Drops one source file's tree and rolls back the refs it introduced; recipe/accumulator/
    /// context state (keyed separately) is preserved. Fire-and-forget, so it returns no response.
    /// </summary>
    [JsonRpcMethod("Evict", UseSingleObjectParameterDeserialization = true)]
    public void Evict(EvictRequest request)
    {
        if (string.IsNullOrEmpty(request.Id))
        {
            return;
        }
        _localObjects.TryRemove(request.Id, out _);
        _remoteObjects.TryRemove(request.Id, out _);
        if (_refCheckpoints.TryRemove(request.Id, out var cp))
        {
            foreach (var kv in _localRefs)
            {
                if (kv.Value > cp.LocalRefs)
                {
                    _localRefs.TryRemove(kv.Key, out _);
                }
            }
            foreach (var key in _remoteRefs.Keys)
            {
                if (key > cp.RemoteRefsMax)
                {
                    _remoteRefs.TryRemove(key, out _);
                }
            }
        }
    }

    /// <summary>
    /// Stores a tree in the local object cache so Java can fetch it via GetObject.
    /// </summary>
    internal void StoreLocalObject(string id, object obj) => _localObjects[id] = obj;

    /// <summary>
    /// Gets or creates an ExecutionContext by ID, caching it for reuse across phases.
    /// </summary>
    private ExecutionContext GetOrCreateExecutionContext(string? pId)
    {
        if (pId != null && _executionContexts.TryGetValue(pId, out var existing))
        {
            InstallDataTableStore(existing);
            return existing;
        }

        var ctx = new ExecutionContext();
        // Inject the build context captured during ParseSolution so that
        // reattestation (MSBuildProjectHelper) can materialize build files
        _buildContext?.StoreIn(ctx);
        InstallDataTableStore(ctx);

        if (pId != null)
        {
            _executionContexts[pId] = ctx;
            _localObjects[pId] = ctx;
        }
        return ctx;
    }

    private void InstallDataTableStore(ExecutionContext ctx)
    {
        var store = _configuredDataTableStore;
        if (store != null)
        {
            ctx.PutMessage(DataTable<object>.DataTableStoreKey, store);
        }
    }

    /// <summary>
    /// Gets or creates the accumulator for a scanning recipe, storing it for reuse across scan/generate/edit phases.
    /// </summary>
    private object? GetOrCreateAccumulator(string recipeId, IScanningRecipe recipe, ExecutionContext ctx)
    {
        if (_recipeAccumulators.TryGetValue(recipeId, out var acc))
            return acc;
        
        acc = recipe.InitialValue(ctx);
        _recipeAccumulators[recipeId] = acc;
        return acc;
    }

    private class TreeCodec : IRpcCodec
    {
        public static readonly TreeCodec Instance = new();
        public void RpcSend(object after, RpcSendQueue q)
        {
            switch (after)
            {
                case ParseError pe: pe.RpcSend(pe, q); break;
                case OpenRewrite.Xml.Xml xml: new OpenRewrite.Xml.Rpc.XmlSender().Visit(xml, q); break;
                default: new CSharpSender().Visit((J)after, q); break;
            }
        }
        public object RpcReceive(object before, RpcReceiveQueue q)
        {
            return before switch
            {
                ParseError pe => pe.RpcReceive(pe, q),
                OpenRewrite.Xml.Xml xml => new OpenRewrite.Xml.Rpc.XmlReceiver().Visit(xml, q)!,
                _ => new CSharpReceiver().Visit((J)before, q)!
            };
        }
    }
}

/// <summary>
/// Writes one CSV row per dispatched RPC call: timing, managed-heap memory, and object/ref cache
/// residency. Same schema as the Go and Python servers. Thread-safe; rows are flushed eagerly.
/// </summary>
internal sealed class RpcMetricsWriter : IDisposable
{
    private const string Header =
        "timestamp,method,duration_ms,error,memory_used_bytes,memory_max_bytes,local_objects,remote_objects,refs";

    private readonly StreamWriter _writer;
    private readonly Func<(int Local, int Remote, int Refs)> _cacheSizes;
    private readonly object _lock = new();
    private bool _disposed;

    public RpcMetricsWriter(string path, Func<(int, int, int)> cacheSizes)
    {
        _cacheSizes = cacheSizes;
        _writer = new StreamWriter(new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.Read));
        _writer.WriteLine(Header);
        _writer.Flush();
    }

    public void Record(string method, double durationMs, string? error)
    {
        var (local, remote, refs) = _cacheSizes();
        var used = GC.GetTotalMemory(false);
        var max = GC.GetGCMemoryInfo().HeapSizeBytes;
        var timestamp = DateTimeOffset.UtcNow.ToString("O");
        lock (_lock)
        {
            if (_disposed)
            {
                return;
            }
            _writer.WriteLine(
                $"{timestamp},{method},{durationMs:F0},{Escape(error)},{used},{max},{local},{remote},{refs}");
            _writer.Flush();
        }
    }

    // Quote per RFC 4180 only when the field contains a comma, quote, or newline (errors can).
    private static string Escape(string? field)
    {
        if (string.IsNullOrEmpty(field))
        {
            return "";
        }
        if (field.IndexOfAny([',', '"', '\n', '\r']) < 0)
        {
            return field;
        }
        return $"\"{field.Replace("\"", "\"\"")}\"";
    }

    public void Dispose()
    {
        lock (_lock)
        {
            if (_disposed)
            {
                return;
            }
            _disposed = true;
            _writer.Dispose();
        }
    }
}

/// <summary>
/// Wraps the message handler to record a metrics row when each inbound request's response is
/// written. Notifications (Evict) get no response and aren't recorded; outbound requests are ignored.
/// </summary>
internal sealed class MetricsMessageHandler : IJsonRpcMessageHandler, IDisposable
{
    private readonly IJsonRpcMessageHandler _inner;
    private readonly RpcMetricsWriter _metrics;
    private readonly ConcurrentDictionary<RequestId, (string Method, long Start)> _inflight = new();

    public MetricsMessageHandler(IJsonRpcMessageHandler inner, RpcMetricsWriter metrics)
    {
        _inner = inner;
        _metrics = metrics;
    }

    public bool CanRead => _inner.CanRead;
    public bool CanWrite => _inner.CanWrite;
    public IJsonRpcMessageFormatter Formatter => _inner.Formatter;

    public async ValueTask<JsonRpcMessage?> ReadAsync(CancellationToken cancellationToken)
    {
        var message = await _inner.ReadAsync(cancellationToken).ConfigureAwait(false);
        if (message is JsonRpcRequest { IsResponseExpected: true } request)
        {
            _inflight[request.RequestId] = (request.Method ?? "", Stopwatch.GetTimestamp());
        }
        return message;
    }

    public async ValueTask WriteAsync(JsonRpcMessage message, CancellationToken cancellationToken)
    {
        await _inner.WriteAsync(message, cancellationToken).ConfigureAwait(false);
        // Only responses to inbound requests carry an id we put in _inflight; outbound requests we
        // send to Java are JsonRpcRequest and never match, so their ids can't collide here.
        if (message is JsonRpcResult or JsonRpcError &&
            message is IJsonRpcMessageWithId withId &&
            _inflight.TryRemove(withId.RequestId, out var entry))
        {
            var durationMs = Stopwatch.GetElapsedTime(entry.Start).TotalMilliseconds;
            var error = (message as JsonRpcError)?.Error?.Message;
            _metrics.Record(entry.Method, durationMs, error);
        }
    }

    public void Dispose()
    {
        (_inner as IDisposable)?.Dispose();
        _metrics.Dispose();
    }
}

/// <summary>
/// A JsonRpc subclass that ensures error.data is always a string,
/// for compatibility with the Java io.moderne:jsonrpc library which
/// expects error.detail.data to be a string, not a structured object.
/// </summary>
internal class StringErrorDataJsonRpc : StreamJsonRpc.JsonRpc
{
    public StringErrorDataJsonRpc(StreamJsonRpc.IJsonRpcMessageHandler handler) : base(handler)
    {
    }

    protected override StreamJsonRpc.Protocol.JsonRpcError.ErrorDetail CreateErrorDetails(
        StreamJsonRpc.Protocol.JsonRpcRequest request, Exception exception)
    {
        return new StreamJsonRpc.Protocol.JsonRpcError.ErrorDetail
        {
            Code = (StreamJsonRpc.Protocol.JsonRpcErrorCode)(-32603),
            Message = exception.Message,
            Data = exception.ToString()
        };
    }
}

// --- Request/Response DTOs ---

public class ParseSolutionRequest
{
    public string Path { get; set; } = "";
    public string RootDir { get; set; } = "";
    public Dictionary<string, object>? Options { get; set; }
}

public class ParseSolutionResponse
{
    public List<ParseSolutionResponseItem> Items { get; set; } = new();
}

public class ParseSolutionResponseItem
{
    public string Id { get; set; } = "";
    public string SourceFileType { get; set; } = "";

    // Relative source path; only populated for Quark items, from which the Java
    // side builds the Quark locally. Null for normal items (fetched via GetObject).
    public string? SourcePath { get; set; }
}

public class GetObjectRequest
{
    public string Id { get; set; } = "";
    public string? SourceFileType { get; set; }
}

[JsonPolymorphic(TypeDiscriminatorPropertyName = "kind")]
[JsonDerivedType(typeof(Csv), "CSV")]
[JsonDerivedType(typeof(NoOp), "NOOP")]
public abstract class SetDataTableStoreRequest
{
    public abstract IDataTableStore ToDataTableStore();

    public sealed class Csv : SetDataTableStoreRequest
    {
        public string? OutputDir { get; set; }
        public Dictionary<string, string>? PrefixColumns { get; set; }
        public Dictionary<string, string>? SuffixColumns { get; set; }

        public override IDataTableStore ToDataTableStore() =>
            string.IsNullOrEmpty(OutputDir)
                ? new InMemoryDataTableStore()
                : new CsvDataTableStore(
                    OutputDir,
                    PrefixColumns ?? new Dictionary<string, string>(),
                    SuffixColumns ?? new Dictionary<string, string>());
    }

    public sealed class NoOp : SetDataTableStoreRequest
    {
        public override IDataTableStore ToDataTableStore() => new InMemoryDataTableStore();
    }
}

public class ParseRequest
{
    public List<ParseInput> Inputs { get; set; } = new();
    public string? RelativeTo { get; set; }
}

public class ParseInput
{
    public string Text { get; set; } = "";
    public string SourcePath { get; set; } = "";
}

public class PrintRequest
{
    public string TreeId { get; set; } = "";
    public string? SourcePath { get; set; }
    public string? SourceFileType { get; set; }
    public string? MarkerPrinter { get; set; }
}

public class EvictRequest
{
    public string Id { get; set; } = "";
}

public class GetMarketplaceResponseRow
{
    public RecipeDescriptorDto Descriptor { get; set; } = null!;
    public List<List<CategoryDescriptorDto>> CategoryPaths { get; set; } = [];
    public string? PackageName { get; set; }
}

public class CategoryDescriptorDto
{
    public string DisplayName { get; set; } = "";
    public string? Description { get; set; }
}

/// <summary>
/// Reads <c>estimatedEffortPerOccurrence</c> from either the Java wire shape (a
/// JSON number of seconds) or the C# wire shape (an ISO-8601 duration string),
/// normalizing both to an ISO-8601 string. Writes the ISO-8601 string, which
/// the Java peer's Jackson Duration deserializer accepts.
/// </summary>
public sealed class DurationWireConverter : JsonConverter<string?>
{
    public override string? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        switch (reader.TokenType)
        {
            case JsonTokenType.Null:
                return null;
            case JsonTokenType.String:
                return reader.GetString();
            case JsonTokenType.Number:
                return System.Xml.XmlConvert.ToString(TimeSpan.FromSeconds(reader.GetDouble()));
            default:
                reader.Skip();
                return null;
        }
    }

    public override void Write(Utf8JsonWriter writer, string? value, JsonSerializerOptions options)
    {
        if (value is null)
            writer.WriteNullValue();
        else
            writer.WriteStringValue(value);
    }
}

public class RecipeDescriptorDto
{
    public string Name { get; set; } = "";
    public string DisplayName { get; set; } = "";
    public string InstanceName { get; set; } = "";
    public string Description { get; set; } = "";
    public HashSet<string> Tags { get; set; } = [];

    // The Java peer serializes the recipe's estimatedEffortPerOccurrence
    // (a Duration) as a JSON number of seconds, while the C# side emits it as
    // an ISO-8601 duration string. Accept either on read so System.Text.Json's
    // strict number/string handling doesn't fail descriptor deserialization.
    [JsonConverter(typeof(DurationWireConverter))]
    public string? EstimatedEffortPerOccurrence { get; set; }
    public List<OptionDescriptorDto> Options { get; set; } = [];
    public List<RecipeDescriptorDto> Preconditions { get; set; } = [];
    public List<RecipeDescriptorDto> RecipeList { get; set; } = [];
    public List<object> DataTables { get; set; } = [];
    public List<object> Maintainers { get; set; } = [];
    public List<object> Contributors { get; set; } = [];
    public List<object> Examples { get; set; } = [];

    public static RecipeDescriptorDto FromDescriptor(RecipeDescriptor d)
    {
        return new RecipeDescriptorDto
        {
            Name = d.Name,
            DisplayName = d.DisplayName,
            InstanceName = d.DisplayName,
            Description = d.Description,
            Tags = new HashSet<string>(d.Tags),
            EstimatedEffortPerOccurrence = d.EstimatedEffortPerOccurrence is { } ts
                ? System.Xml.XmlConvert.ToString(ts) // ISO-8601 duration (e.g. "PT5M")
                : null,
            Options = d.Options.Select(OptionDescriptorDto.FromDescriptor).ToList(),
            RecipeList = d.RecipeList.Select(FromDescriptor).ToList()
        };
    }
}

public class OptionDescriptorDto
{
    public string Name { get; set; } = "";
    public string Type { get; set; } = "";
    public string DisplayName { get; set; } = "";
    public string Description { get; set; } = "";
    public string? Example { get; set; }
    public List<string>? Valid { get; set; }
    public bool Required { get; set; }
    public object? Value { get; set; }

    public static OptionDescriptorDto FromDescriptor(OptionDescriptor d)
    {
        return new OptionDescriptorDto
        {
            Name = d.Name,
            Type = d.Type,
            DisplayName = d.DisplayName,
            Description = d.Description,
            Example = d.Example,
            Valid = d.Valid?.ToList(),
            Required = d.Required,
            Value = d.Value
        };
    }
}

public class InstallRecipesRequest
{
    public object? Recipes { get; set; }
}

public class InstallRecipesResponse
{
    public int RecipesInstalled { get; set; }
    public string? Version { get; set; }
}

public class PrepareRecipeRequest
{
    public string Id { get; set; } = "";
    public Dictionary<string, object?>? Options { get; set; }
}

public class PrepareRecipeResponse
{
    public string Id { get; set; } = "";
    public RecipeDescriptorDto Descriptor { get; set; } = null!;
    public string EditVisitor { get; set; } = "";
    public List<Precondition> EditPreconditions { get; set; } = [];
    public string? ScanVisitor { get; set; }
    public List<Precondition> ScanPreconditions { get; set; } = [];
    public DelegatesTo? DelegatesTo { get; set; }
    public List<PrepareRecipeResponse> RecipeList { get; set; } = [];
}

public class DelegatesTo
{
    public string RecipeName { get; set; } = "";
    public Dictionary<string, object?> Options { get; set; } = new();
}

/// <summary>
/// Either a leaf (a single visitor identified by <see cref="VisitorName"/> +
/// optional <see cref="VisitorOptions"/>) or a composite of nested
/// preconditions joined by <see cref="Op"/> ("or" / "and" / "not"). When
/// <see cref="Op"/> is null the entry is a leaf and the visitor fields
/// carry the gate identity; when <see cref="Op"/> is set, <see cref="Operands"/>
/// carries the children and the visitor fields are ignored. Mirrors the
/// Java DTO <c>org.openrewrite.rpc.request.PrepareRecipeResponse.Precondition</c>.
/// </summary>
public class Precondition
{
    public string? VisitorName { get; set; }
    public Dictionary<string, object>? VisitorOptions { get; set; }
    public string? Op { get; set; }
    public List<Precondition>? Operands { get; set; }
}

public class GenerateRequest
{
    public string Id { get; set; } = "";
    [JsonPropertyName("p")]
    public string? P { get; set; }
}

public class GenerateResponse
{
    public List<string> Ids { get; set; } = [];
    public List<string> SourceFileTypes { get; set; } = [];
}

public class VisitRequest
{
    [JsonPropertyName("visitor")]
    public string VisitorName { get; set; } = "";
    public string? SourceFileType { get; set; }
    public string TreeId { get; set; } = "";
    [JsonPropertyName("p")]
    public string? PId { get; set; }
    [JsonPropertyName("cursor")]
    public List<string>? CursorIds { get; set; }
}

public class VisitResponse
{
    public bool Modified { get; set; }
}

public class BatchVisitRequest
{
    public string SourceFileType { get; set; } = "";
    public string TreeId { get; set; } = "";
    [JsonPropertyName("p")]
    public string? PId { get; set; }
    [JsonPropertyName("cursor")]
    public List<string>? CursorIds { get; set; }
    public List<BatchVisitItem> Visitors { get; set; } = new();
}

public class BatchVisitItem
{
    public string Visitor { get; set; } = "";
    public Dictionary<string, object>? VisitorOptions { get; set; }
}

public class BatchVisitResult
{
    public bool Modified { get; set; }
    public bool Deleted { get; set; }
    public bool HasNewMessages { get; set; }
    public List<string> SearchResultIds { get; set; } = new();
}

public class BatchVisitResponse
{
    public List<BatchVisitResult> Results { get; set; } = new();
}

