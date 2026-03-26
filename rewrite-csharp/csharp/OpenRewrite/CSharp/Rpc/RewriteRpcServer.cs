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
using System.Xml.Linq;
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json.Serialization;
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;
using Serilog;
using StreamJsonRpc;
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
    private readonly ConcurrentDictionary<string, Recipe> _preparedRecipes = new();
    private readonly ConcurrentDictionary<string, object?> _recipeAccumulators = new();
    private readonly ConcurrentDictionary<string, ExecutionContext> _executionContexts = new();
    private string? _recipesProjectDir;
    private JsonRpc? _jsonRpc;

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

    public RewriteRpcServer(RecipeMarketplace marketplace)
    {
        _marketplace = marketplace;

        // Register type name overrides for nagoya types that don't match Java names
        RpcSendQueue.RegisterJavaTypeName(typeof(CsLambda),
            "org.openrewrite.csharp.tree.Cs$Lambda");
        // Cs-prefixed types in C# that correspond to unprefixed Java names
        RpcSendQueue.RegisterJavaTypeName(typeof(CsBinary),
            "org.openrewrite.csharp.tree.Cs$Binary");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsUnary),
            "org.openrewrite.csharp.tree.Cs$Unary");

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

        var requirePrintEqualsInput = true;
        if (request.Options?.TryGetValue("org.openrewrite.requirePrintEqualsInput", out var val) == true)
        {
            // StreamJsonRpc with Newtonsoft.Json may deliver values as JToken wrappers
            if (val is JToken jt)
                requirePrintEqualsInput = jt.Value<bool>();
            else
                requirePrintEqualsInput = Convert.ToBoolean(val);
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
                    SourceFileType = sourceFileType,
                    ProjectPath = project.FilePath!
                });
            }

            // Extract MSBuild project metadata from .csproj
            try
            {
                var metadata = ExtractProjectMetadata(project.FilePath!, rootDir);
                response.Projects.Add(metadata);
            }
            catch (Exception ex)
            {
                Log.Debug("RPC ParseSolution: failed to extract metadata for {ProjectPath}: {ExType}: {ExMessage}",
                    project.FilePath, ex.GetType().Name, ex.Message);
            }
        }

        Log.Debug("RPC ParseSolution: completed, {ItemCount} source files, {ProjectCount} project metadata",
            response.Items.Count, response.Projects.Count);
        return response;
    }

    /// <summary>
    /// Extracts MSBuild project metadata from a .csproj file by parsing its XML
    /// and reading the resolved dependency tree from project.assets.json.
    /// </summary>
    private static ProjectMetadata ExtractProjectMetadata(string projectPath, string rootDir)
    {
        var doc = XDocument.Load(projectPath);
        var root = doc.Root!;
        var ns = root.Name.Namespace;

        var relativePath = Path.GetRelativePath(rootDir, projectPath);

        var metadata = new ProjectMetadata
        {
            ProjectPath = relativePath,
            Sdk = root.Attribute("Sdk")?.Value
        };

        // Extract TargetFramework(s)
        var tfmElement = root.Descendants(ns + "TargetFramework").FirstOrDefault();
        var tfmsElement = root.Descendants(ns + "TargetFrameworks").FirstOrDefault();

        var frameworks = new List<string>();
        if (tfmsElement != null)
        {
            foreach (var tfm in tfmsElement.Value.Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
                frameworks.Add(tfm);
        }
        else if (tfmElement != null)
        {
            frameworks.Add(tfmElement.Value.Trim());
        }

        // Extract PackageReferences
        var packageRefs = root.Descendants(ns + "PackageReference")
            .Select(e => new PackageReferenceEntry
            {
                Include = e.Attribute("Include")?.Value ?? "",
                RequestedVersion = e.Attribute("Version")?.Value,
                ResolvedVersion = e.Attribute("Version")?.Value // raw = resolved in XML context
            })
            .Where(r => !string.IsNullOrEmpty(r.Include))
            .ToList();

        // Extract ProjectReferences
        var projectRefs = root.Descendants(ns + "ProjectReference")
            .Select(e => new ProjectReferenceEntry
            {
                Include = e.Attribute("Include")?.Value ?? ""
            })
            .Where(r => !string.IsNullOrEmpty(r.Include))
            .ToList();

        // Read resolved packages from project.assets.json
        var assetsPath = Path.Combine(Path.GetDirectoryName(projectPath)!, "obj", "project.assets.json");
        var resolvedByTfm = new Dictionary<string, List<ResolvedPackageEntry>>();
        if (File.Exists(assetsPath))
        {
            try
            {
                resolvedByTfm = ReadResolvedPackages(assetsPath);
            }
            catch (Exception ex)
            {
                Log.Debug("Failed to read project.assets.json at {Path}: {Ex}", assetsPath, ex.Message);
            }
        }

        // Extract properties with provenance
        foreach (var propGroup in root.Descendants(ns + "PropertyGroup"))
        {
            foreach (var prop in propGroup.Elements())
            {
                var propName = prop.Name.LocalName;
                if (!metadata.Properties.ContainsKey(propName))
                {
                    metadata.Properties[propName] = new PropertyEntry
                    {
                        Value = prop.Value,
                        DefinedIn = relativePath
                    };
                }
            }
        }

        // Discover NuGet package sources from nuget.config files
        metadata.PackageSources = FindNuGetPackageSources(projectPath, rootDir);

        // Build per-TFM metadata
        foreach (var tfm in frameworks)
        {
            resolvedByTfm.TryGetValue(tfm, out var resolved);
            metadata.TargetFrameworks.Add(new TargetFrameworkEntry
            {
                TargetFramework = tfm,
                PackageReferences = packageRefs,
                ResolvedPackages = resolved ?? new List<ResolvedPackageEntry>(),
                ProjectReferences = projectRefs
            });
        }

        // If no frameworks found, still include a default entry
        if (frameworks.Count == 0)
        {
            metadata.TargetFrameworks.Add(new TargetFrameworkEntry
            {
                PackageReferences = packageRefs,
                ProjectReferences = projectRefs
            });
        }

        return metadata;
    }

    /// <summary>
    /// Discovers NuGet package sources by walking up from the project directory
    /// to the repository root looking for nuget.config files.
    /// NuGet resolves sources hierarchically — closest config wins.
    /// </summary>
    private static List<PackageSourceEntry> FindNuGetPackageSources(string projectPath, string rootDir)
    {
        var sources = new List<PackageSourceEntry>();
        var dir = Path.GetDirectoryName(projectPath);

        while (dir != null && dir.StartsWith(rootDir, StringComparison.OrdinalIgnoreCase))
        {
            var configPath = Path.Combine(dir, "nuget.config");
            // Case-insensitive check (NuGet.Config, nuget.config, NuGet.config all valid)
            if (!File.Exists(configPath))
            {
                configPath = Path.Combine(dir, "NuGet.Config");
                if (!File.Exists(configPath))
                {
                    configPath = Path.Combine(dir, "NuGet.config");
                }
            }

            if (File.Exists(configPath))
            {
                try
                {
                    var configDoc = XDocument.Load(configPath);
                    var packageSources = configDoc.Root?
                        .Element("packageSources")?
                        .Elements("add");

                    if (packageSources != null)
                    {
                        foreach (var source in packageSources)
                        {
                            var key = source.Attribute("key")?.Value;
                            var url = source.Attribute("value")?.Value;
                            if (key != null && url != null &&
                                !sources.Any(s => s.Key == key))
                            {
                                sources.Add(new PackageSourceEntry
                                {
                                    Key = key,
                                    Url = url
                                });
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    Log.Debug("Failed to parse nuget.config at {Path}: {Ex}", configPath, ex.Message);
                }
                // NuGet uses the closest config — stop walking up once we find one
                break;
            }

            if (dir == rootDir) break;
            dir = Path.GetDirectoryName(dir);
        }

        // Default to nuget.org if no sources found
        if (sources.Count == 0)
        {
            sources.Add(new PackageSourceEntry
            {
                Key = "nuget.org",
                Url = "https://api.nuget.org/v3/index.json"
            });
        }

        return sources;
    }

    /// <summary>
    /// Reads the resolved dependency tree from project.assets.json.
    /// Returns a dictionary keyed by target framework moniker.
    /// </summary>
    private static Dictionary<string, List<ResolvedPackageEntry>> ReadResolvedPackages(string assetsPath)
    {
        var result = new Dictionary<string, List<ResolvedPackageEntry>>();
        var json = JObject.Parse(File.ReadAllText(assetsPath));
        var targets = json["targets"] as JObject;
        if (targets == null) return result;

        foreach (var (tfmKey, tfmValue) in targets)
        {
            // tfmKey is like "net8.0" or ".NETCoreApp,Version=v8.0"
            var tfm = tfmKey.Contains(',')
                ? NormalizeTfm(tfmKey)
                : tfmKey;

            var packages = new List<ResolvedPackageEntry>();
            if (tfmValue is JObject tfmObj)
            {
                foreach (var (pkgKey, pkgValue) in tfmObj)
                {
                    // pkgKey is "PackageName/Version"
                    var parts = pkgKey.Split('/', 2);
                    if (parts.Length != 2) continue;

                    var type = pkgValue?["type"]?.Value<string>();
                    if (type != "package") continue;

                    var deps = new List<ResolvedPackageEntry>();
                    var dependencies = pkgValue?["dependencies"] as JObject;
                    if (dependencies != null)
                    {
                        foreach (var (depName, depVersion) in dependencies)
                        {
                            deps.Add(new ResolvedPackageEntry
                            {
                                Name = depName,
                                ResolvedVersion = depVersion?.Value<string>() ?? "",
                                Depth = 1
                            });
                        }
                    }

                    packages.Add(new ResolvedPackageEntry
                    {
                        Name = parts[0],
                        ResolvedVersion = parts[1],
                        Dependencies = deps,
                        Depth = 0
                    });
                }
            }

            result[tfm] = packages;
        }

        return result;
    }

    /// <summary>
    /// Normalizes a full framework identifier like ".NETCoreApp,Version=v8.0" to "net8.0".
    /// </summary>
    private static string NormalizeTfm(string fullTfm)
    {
        // Simple heuristic: extract the version part
        if (fullTfm.StartsWith(".NETCoreApp,Version=v") || fullTfm.StartsWith(".NETCoreApp,Version=V"))
        {
            var version = fullTfm.Substring(".NETCoreApp,Version=v".Length);
            return "net" + version;
        }
        if (fullTfm.StartsWith(".NETStandard,Version=v") || fullTfm.StartsWith(".NETStandard,Version=V"))
        {
            var version = fullTfm.Substring(".NETStandard,Version=v".Length);
            return "netstandard" + version;
        }
        return fullTfm;
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

    private static void CollectRecipes(
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
                    CategoryPaths = []
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
        string? version = null;

        if (request.Recipes is string path)
        {
            // Local assembly path
            var absolutePath = Path.GetFullPath(path);
            var context = new PluginLoadContext(absolutePath);
            var assembly = context.LoadFromAssemblyPath(absolutePath);
            CheckVersionCompatibility(assembly);
            ActivateAssembly(assembly);
        }
        else if (request.Recipes is JObject packageObj)
        {
            var packageName = packageObj["packageName"]?.ToString()
                              ?? throw new ArgumentException("Missing packageName in recipes object");
            version = packageObj["version"]?.ToString();

            if (File.Exists(packageName))
            {
                var absolutePath = Path.GetFullPath(packageName);
                var context = new PluginLoadContext(absolutePath);
                var assembly = context.LoadFromAssemblyPath(absolutePath);
                CheckVersionCompatibility(assembly);
                ActivateAssembly(assembly);
            }
            else
            {
                // NuGet package download via dotnet CLI
                var csprojPath = EnsureRecipesProject();
                var args = $"add \"{csprojPath}\" package {packageName}";
                if (version != null)
                    args += $" --version {version}";
                RunDotnet(args);

                version = ResolveVersionFromCsproj(csprojPath, packageName);

                var assemblies = PublishAndLoadPlugin(csprojPath, packageName);
                foreach (var assembly in assemblies)
                {
                    CheckVersionCompatibility(assembly);
                    ActivateAssembly(assembly);
                }
            }
        }
        else
        {
            throw new ArgumentException($"Unexpected recipes type: {request.Recipes?.GetType().Name ?? "null"}");
        }

        var afterCount = _marketplace.AllRecipes().Count;
        return Task.FromResult(new InstallRecipesResponse
        {
            RecipesInstalled = afterCount - beforeCount,
            Version = version
        });
    }

    private void ActivateAssembly(Assembly assembly)
    {
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
    }

    private string EnsureRecipesProject()
    {
        if (_recipesProjectDir != null)
        {
            var existing = Path.Combine(_recipesProjectDir, "Recipes.csproj");
            if (File.Exists(existing))
                return existing;
        }

        _recipesProjectDir = Path.Combine(Path.GetTempPath(), "rewrite-recipes", Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(_recipesProjectDir);

        var csprojPath = Path.Combine(_recipesProjectDir, "Recipes.csproj");
        File.WriteAllText(csprojPath, """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
                <EnableDefaultCompileItems>false</EnableDefaultCompileItems>
              </PropertyGroup>
            </Project>
            """);

        // Add local NuGet feed as a package source if it exists, so that
        // locally-published SDK snapshots are discovered alongside nuget.org
        var localFeed = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            ".nuget", "local-feed");
        if (Directory.Exists(localFeed))
        {
            var nugetConfig = Path.Combine(_recipesProjectDir, "nuget.config");
            File.WriteAllText(nugetConfig, $"""
                <?xml version="1.0" encoding="utf-8"?>
                <configuration>
                  <packageSources>
                    <add key="nuget.org" value="https://api.nuget.org/v3/index.json" />
                    <add key="local-feed" value="{localFeed}" />
                  </packageSources>
                </configuration>
                """);
        }

        return csprojPath;
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

    private static string ResolveVersionFromCsproj(string csprojPath, string packageName)
    {
        var doc = XDocument.Load(csprojPath);
        var ns = doc.Root?.Name.Namespace ?? XNamespace.None;

        var packageRef = doc.Descendants(ns + "PackageReference")
            .FirstOrDefault(e => string.Equals(
                e.Attribute("Include")?.Value, packageName, StringComparison.OrdinalIgnoreCase));

        return packageRef?.Attribute("Version")?.Value
               ?? throw new InvalidOperationException(
                   $"Could not find resolved version for {packageName} in {csprojPath}");
    }

    /// <summary>
    /// Publish the temp recipes project to produce a flat output directory with all transitive
    /// dependencies and a .deps.json, then load plugin assemblies in an isolated
    /// <see cref="PluginLoadContext"/>. Because the NuGet package name may not match the assembly
    /// name, we scan all non-host DLLs in the publish output for <see cref="IRecipeActivator"/>
    /// implementations.
    /// </summary>
    private List<Assembly> PublishAndLoadPlugin(string csprojPath, string packageName)
    {
        var projectDir = Path.GetDirectoryName(csprojPath)!;
        var publishDir = Path.Combine(projectDir, "publish");

        RunDotnet($"publish \"{csprojPath}\" -c Release -o \"{publishDir}\"");

        // Use the Recipes.deps.json (from the temp project) for the dependency resolver
        var depsJson = Path.Combine(publishDir, "Recipes.deps.json");
        if (!File.Exists(depsJson))
        {
            Log.Warning("No .deps.json found in publish output at {PublishDir}", publishDir);
        }

        // The temp project's main DLL is the anchor for AssemblyDependencyResolver
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

        var scanningBase = GetScanningRecipeBase(recipe.GetType());
        if (scanningBase != null)
        {
            var ctx = GetOrCreateExecutionContext(request.P);
            var acc = GetOrCreateAccumulator(request.Id, recipe, scanningBase, ctx);

            var generateMethod = scanningBase.GetMethod("Generate")
                ?? throw new InvalidOperationException(
                    $"Could not find Generate method on {scanningBase.Name}");
            var generated = (IEnumerable<SourceFile>)generateMethod.Invoke(recipe, [acc, ctx])!;

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
            throw new InvalidOperationException($"Recipe not found: {request.Id}");
        }

        var (descriptor, recipe) = found.Value;
        if (recipe == null)
        {
            throw new InvalidOperationException($"Recipe {request.Id} has no live instance (installed without constructor)");
        }

        // If options are provided, create a new instance with options applied
        if (request.Options is { Count: > 0 })
        {
            recipe = InstantiateWithOptions(recipe.GetType(), request.Options);
        }

        var id = Guid.NewGuid().ToString();
        _preparedRecipes[id] = recipe;

        var response = new PrepareRecipeResponse
        {
            Id = id,
            Descriptor = RecipeDescriptorDto.FromDescriptor(recipe.GetDescriptor()),
            EditVisitor = $"edit:{id}",
            ScanVisitor = GetScanningRecipeBase(recipe.GetType()) != null ? $"scan:{id}" : null
        };

        if (recipe is IDelegatesTo del)
        {
            response.DelegatesTo = new DelegatesTo
            {
                RecipeName = del.JavaRecipeName,
                Options = del.Options
            };
        }
        else
        {
            OptimizePreconditions(recipe, response);
        }

        return Task.FromResult(response);
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

    private static Recipe InstantiateWithOptions(Type recipeType, Dictionary<string, object?> options)
    {
        var recipe = (Recipe)Activator.CreateInstance(recipeType)!;
        foreach (var (key, value) in options)
        {
            var prop = recipeType.GetProperty(key, BindingFlags.Public | BindingFlags.Instance | BindingFlags.IgnoreCase);
            if (prop != null && prop.CanWrite)
            {
                var convertedValue = Convert.ChangeType(value, prop.PropertyType);
                prop.SetValue(recipe, convertedValue);
            }
        }
        return recipe;
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
        var tree = await GetObjectFromRemoteAsync(request.TreeId, request.SourceFileType);

        if (phase != "scan" && phase != "edit")
        {
            throw new ArgumentException($"Unknown visitor phase: {phase}");
        }

        var ctx = GetOrCreateExecutionContext(request.PId);
        ITreeVisitor<ExecutionContext> visitor;

        var scanningBase = GetScanningRecipeBase(recipe.GetType());
        if (scanningBase != null)
        {
            var acc = GetOrCreateAccumulator(recipeId, recipe, scanningBase, ctx);
            if (phase == "scan")
            {
                var getScannerMethod = scanningBase.GetMethod("GetScanner")
                    ?? throw new InvalidOperationException(
                        $"Could not find GetScanner method on {scanningBase.Name}");
                visitor = (ITreeVisitor<ExecutionContext>)getScannerMethod.Invoke(recipe, [acc])!;
            }
            else
            {
                var getVisitorMethod = scanningBase.GetMethod("GetVisitor",
                    [scanningBase.GetGenericArguments()[0]])
                    ?? throw new InvalidOperationException(
                        $"Could not find GetVisitor(T) method on {scanningBase.Name}");
                visitor = (ITreeVisitor<ExecutionContext>)getVisitorMethod.Invoke(recipe, [acc])!;
            }
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
            var scanningBase = GetScanningRecipeBase(recipe.GetType());
            if (scanningBase != null)
            {
                var acc = GetOrCreateAccumulator(recipeId, recipe, scanningBase, ctx);
                if (phase == "scan")
                {
                    var getScannerMethod = scanningBase.GetMethod("GetScanner")
                        ?? throw new InvalidOperationException($"Could not find GetScanner on {scanningBase.Name}");
                    visitor = (ITreeVisitor<ExecutionContext>)getScannerMethod.Invoke(recipe, [acc])!;
                }
                else
                {
                    var getVisitorMethod = scanningBase.GetMethod("GetVisitor",
                        [scanningBase.GetGenericArguments()[0]])
                        ?? throw new InvalidOperationException($"Could not find GetVisitor on {scanningBase.Name}");
                    visitor = (ITreeVisitor<ExecutionContext>)getVisitorMethod.Invoke(recipe, [acc])!;
                }
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

        // Configure JSON serialization to match Java expectations:
        // - camelCase property names
        // - string enum values (not integers)
        var formatter = new JsonMessageFormatter();
        formatter.JsonSerializer.ContractResolver = new CamelCasePropertyNamesContractResolver();
        formatter.JsonSerializer.Converters.Add(new StringEnumConverter());
        formatter.JsonSerializer.NullValueHandling = NullValueHandling.Ignore;

        var handler = new HeaderDelimitedMessageHandler(outputStream, inputStream, formatter);
        using var jsonRpc = new StringErrorDataJsonRpc(handler);

        var server = new RewriteRpcServer(marketplace);
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
        }
    }

    /// <summary>
    /// Asks the Java peer to parse source content and returns the parsed tree.
    /// The Java side selects the appropriate parser based on the file extension.
    /// </summary>
    public Tree ParseOnRemote(string sourcePath, string content, string? sourceFileType = null)
    {
        var response = _jsonRpc!.InvokeWithParameterObjectAsync<List<string>>(
            "Parse",
            new ParseRequest
            {
                Inputs = [new ParseInput { Text = content, SourcePath = sourcePath }]
            }
        ).GetAwaiter().GetResult();

        if (response.Count == 0)
            throw new InvalidOperationException($"Parse returned no results for {sourcePath}");

        var id = response[0];
        return GetObjectFromRemoteAsync(id, sourceFileType).GetAwaiter().GetResult();
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
        _preparedRecipes.Clear();
        _recipeAccumulators.Clear();
        _executionContexts.Clear();
    }

    /// <summary>
    /// Stores a tree in the local object cache so Java can fetch it via GetObject.
    /// </summary>
    internal void StoreLocalObject(string id, object obj) => _localObjects[id] = obj;

    /// <summary>
    /// Finds the closed generic ScanningRecipe&lt;T&gt; base type, or null if the recipe is not a scanning recipe.
    /// </summary>
    private static Type? GetScanningRecipeBase(Type recipeType)
    {
        var type = recipeType;
        while (type != null)
        {
            if (type.IsGenericType && type.GetGenericTypeDefinition() == typeof(ScanningRecipe<>))
                return type;
            type = type.BaseType;
        }
        return null;
    }

    /// <summary>
    /// Gets or creates an ExecutionContext by ID, caching it for reuse across phases.
    /// </summary>
    private ExecutionContext GetOrCreateExecutionContext(string? pId)
    {
        if (pId != null && _executionContexts.TryGetValue(pId, out var existing))
            return existing;

        var ctx = new ExecutionContext();
        if (pId != null)
        {
            _executionContexts[pId] = ctx;
            _localObjects[pId] = ctx;
        }
        return ctx;
    }

    /// <summary>
    /// Gets or creates the accumulator for a scanning recipe, storing it for reuse across scan/generate/edit phases.
    /// </summary>
    private object? GetOrCreateAccumulator(string recipeId, Recipe recipe, Type scanningBase, ExecutionContext ctx)
    {
        if (_recipeAccumulators.TryGetValue(recipeId, out var acc))
            return acc;

        var getInitialValue = scanningBase.GetMethod("GetInitialValue")
            ?? throw new InvalidOperationException(
                $"Could not find GetInitialValue method on {scanningBase.Name}");
        acc = getInitialValue.Invoke(recipe, [ctx]);
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
    public List<ProjectMetadata> Projects { get; set; } = new();
}

public class ParseSolutionResponseItem
{
    public string Id { get; set; } = "";
    public string SourceFileType { get; set; } = "";
    public string ProjectPath { get; set; } = "";
}

public class ProjectMetadata
{
    public string ProjectPath { get; set; } = "";
    public string? Sdk { get; set; }
    public Dictionary<string, PropertyEntry> Properties { get; set; } = new();
    public List<TargetFrameworkEntry> TargetFrameworks { get; set; } = new();
    public List<PackageSourceEntry> PackageSources { get; set; } = new();
}

public class PackageSourceEntry
{
    public string Key { get; set; } = "";
    public string Url { get; set; } = "";
}

public class PropertyEntry
{
    public string Value { get; set; } = "";
    public string? DefinedIn { get; set; }
}

public class TargetFrameworkEntry
{
    public string TargetFramework { get; set; } = "";
    public List<PackageReferenceEntry> PackageReferences { get; set; } = new();
    public List<ResolvedPackageEntry> ResolvedPackages { get; set; } = new();
    public List<ProjectReferenceEntry> ProjectReferences { get; set; } = new();
}

public class PackageReferenceEntry
{
    public string Include { get; set; } = "";
    public string? RequestedVersion { get; set; }
    public string? ResolvedVersion { get; set; }
}

public class ResolvedPackageEntry
{
    public string Name { get; set; } = "";
    public string ResolvedVersion { get; set; } = "";
    public List<ResolvedPackageEntry> Dependencies { get; set; } = new();
    public int Depth { get; set; }
}

public class ProjectReferenceEntry
{
    public string Include { get; set; } = "";
}

public class GetObjectRequest
{
    public string Id { get; set; } = "";
    public string? SourceFileType { get; set; }
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

public class GetMarketplaceResponseRow
{
    public RecipeDescriptorDto Descriptor { get; set; } = null!;
    public List<List<CategoryDescriptorDto>> CategoryPaths { get; set; } = [];
}

public class CategoryDescriptorDto
{
    public string DisplayName { get; set; } = "";
    public string? Description { get; set; }
}

public class RecipeDescriptorDto
{
    public string Name { get; set; } = "";
    public string DisplayName { get; set; } = "";
    public string InstanceName { get; set; } = "";
    public string Description { get; set; } = "";
    public HashSet<string> Tags { get; set; } = [];
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
}

public class DelegatesTo
{
    public string RecipeName { get; set; } = "";
    public Dictionary<string, object?> Options { get; set; } = new();
}

public class Precondition
{
    public string VisitorName { get; set; } = "";
    public Dictionary<string, object> VisitorOptions { get; set; } = [];
}

public class GenerateRequest
{
    public string Id { get; set; } = "";
    [JsonProperty("p")]
    public string? P { get; set; }
}

public class GenerateResponse
{
    public List<string> Ids { get; set; } = [];
    public List<string> SourceFileTypes { get; set; } = [];
}

public class VisitRequest
{
    [JsonProperty("visitor")]
    public string VisitorName { get; set; } = "";
    public string? SourceFileType { get; set; }
    public string TreeId { get; set; } = "";
    [JsonProperty("p")]
    public string? PId { get; set; }
    [JsonProperty("cursor")]
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
    [JsonProperty("p")]
    public string? PId { get; set; }
    [JsonProperty("cursor")]
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

