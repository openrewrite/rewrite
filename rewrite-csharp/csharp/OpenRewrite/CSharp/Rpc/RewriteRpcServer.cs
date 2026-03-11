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
    private readonly Dictionary<string, Recipe> _preparedRecipes = new();
    private string? _recipesProjectDir;
    private JsonRpc? _jsonRpc;

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

    /// <summary>
    /// Refs received from the remote process (Java) for deduplication.
    /// </summary>
    private readonly Dictionary<int, object> _remoteRefs = new();

    /// <summary>
    /// Connects this server to a remote JSON-RPC peer. Used by test infrastructure
    /// to wire up an RPC connection to a Java process.
    /// </summary>
    public void Connect(JsonRpc jsonRpc)
    {
        _jsonRpc = jsonRpc;
        jsonRpc.AddLocalRpcTarget(this);
        jsonRpc.StartListening();
    }

    public RewriteRpcServer(RecipeMarketplace marketplace)
    {
        _marketplace = marketplace;

        // Register type name overrides for nagoya types that don't match Java names
        RpcSendQueue.RegisterJavaTypeName(typeof(NamespaceDeclaration),
            "org.openrewrite.csharp.tree.Cs$BlockScopeNamespaceDeclaration");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsLambda),
            "org.openrewrite.csharp.tree.Cs$Lambda");
        // Cs-prefixed types in C# that correspond to unprefixed Java names
        RpcSendQueue.RegisterJavaTypeName(typeof(CsBinary),
            "org.openrewrite.csharp.tree.Cs$Binary");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsUnary),
            "org.openrewrite.csharp.tree.Cs$Unary");
        RpcSendQueue.RegisterJavaTypeName(typeof(ConstrainedTypeParameter),
            "org.openrewrite.csharp.tree.Cs$ConstrainedTypeParameter");

        // Types in nagoya's Rewrite.Java namespace that don't follow nesting conventions
        RpcSendQueue.RegisterJavaTypeName(typeof(Java.NamedVariable),
            "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable");

        // Types in nagoya's Rewrite.Java namespace that are Cs types in Java
        RpcSendQueue.RegisterJavaTypeName(typeof(Java.ExpressionStatement),
            "org.openrewrite.csharp.tree.Cs$ExpressionStatement");

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
    }

    [JsonRpcMethod("ParseSolution", UseSingleObjectParameterDeserialization = true)]
    public async Task<List<ParseSolutionResponseItem>> ParseSolution(ParseSolutionRequest request)
    {
        Log.Debug("RPC ParseSolution: received request path={Path} rootDir={RootDir}", request.Path, request.RootDir);
        var solutionParser = new SolutionParser();
        var path = ResolvePath(request.Path);
        var rootDir = ResolvePath(request.RootDir);

        var solution = await solutionParser.LoadAsync(path, CancellationToken.None);

        var items = new List<ParseSolutionResponseItem>();
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

            List<CompilationUnit> results;
            try
            {
                results = solutionParser.ParseProject(solution, project.FilePath!, rootDir);
            }
            catch (Exception ex)
            {
                Log.Debug("RPC ParseSolution: EXCEPTION parsing project {ProjectPath}: {ExType}: {ExMessage}",
                    project.FilePath, ex.GetType().Name, ex.Message);
                throw;
            }

            foreach (var cu in results)
            {
                var id = cu.Id.ToString();
                _localObjects[id] = cu;
                items.Add(new ParseSolutionResponseItem
                {
                    Id = id,
                    SourceFileType = "org.openrewrite.csharp.tree.Cs$CompilationUnit",
                    ProjectPath = project.FilePath!
                });
            }
        }

        Log.Debug("RPC ParseSolution: completed, {ItemCount} compilation units total", items.Count);
        return items;
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
            var printer = new CSharpPrinter<int>();
            return printer.Print(tree);
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
        if (q.Take().State != END_OF_OBJECT)
            throw new InvalidOperationException("Expected END_OF_OBJECT");

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

        return Task.FromResult(new PrepareRecipeResponse
        {
            Id = id,
            Descriptor = RecipeDescriptorDto.FromDescriptor(recipe.GetDescriptor()),
            EditVisitor = $"edit:{id}",
            ScanVisitor = recipe is ScanningRecipe<object> ? $"scan:{id}" : null
        });
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
    public Task<VisitResponse> Visit(VisitRequest request)
    {
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

        if (!_localObjects.TryGetValue(request.TreeId, out var obj) || obj is not Tree tree)
        {
            throw new InvalidOperationException($"Tree not found: {request.TreeId}");
        }

        var ctx = new ExecutionContext();
        var visitor = recipe.GetVisitor();
        var result = visitor.Visit(tree, ctx);

        var modified = !ReferenceEquals(tree, result);
        if (modified && result != null)
        {
            _localObjects[request.TreeId] = result;
        }

        return Task.FromResult(new VisitResponse { Modified = modified });
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
    /// Stores a tree in the local object cache so Java can fetch it via GetObject.
    /// </summary>
    internal void StoreLocalObject(string id, object obj) => _localObjects[id] = obj;

    private class TreeCodec : IRpcCodec
    {
        public static readonly TreeCodec Instance = new();
        public void RpcSend(object after, RpcSendQueue q) => new CSharpSender().Visit((J)after, q);
        public object RpcReceive(object before, RpcReceiveQueue q) => new CSharpReceiver().Visit((J)before, q)!;
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
}

public class ParseSolutionResponseItem
{
    public string Id { get; set; } = "";
    public string SourceFileType { get; set; } = "";
    public string ProjectPath { get; set; } = "";
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
    public HashSet<string> Tags { get; set; } = new();
    public string? EstimatedEffortPerOccurrence { get; set; }
    public List<OptionDescriptorDto> Options { get; set; } = new();
    public List<RecipeDescriptorDto> Preconditions { get; set; } = new();
    public List<RecipeDescriptorDto> RecipeList { get; set; } = new();
    public List<object> DataTables { get; set; } = new();
    public List<object> Maintainers { get; set; } = new();
    public List<object> Contributors { get; set; } = new();
    public List<object> Examples { get; set; } = new();

    public static RecipeDescriptorDto FromDescriptor(RecipeDescriptor d)
    {
        return new RecipeDescriptorDto
        {
            Name = d.Name,
            DisplayName = d.DisplayName,
            InstanceName = d.DisplayName,
            Description = d.Description,
            Tags = new HashSet<string>(d.Tags),
            EstimatedEffortPerOccurrence = d.EstimatedEffortPerOccurrence is { } effort
                ? System.Xml.XmlConvert.ToString(effort)
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
    public string? ScanVisitor { get; set; }
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

