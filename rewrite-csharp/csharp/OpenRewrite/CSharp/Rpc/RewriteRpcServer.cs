using System.Diagnostics;
using System.Reflection;
using System.Runtime.Loader;
using System.Text.Json;
using System.Xml.Linq;
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json.Serialization;
using Rewrite.Core;
using Rewrite.Core.Rpc;
using Rewrite.Java;
using StreamJsonRpc;
using static Rewrite.Core.Rpc.RpcObjectData.ObjectState;

namespace Rewrite.CSharp.Rpc;

public class RewriteRpcServer
{
    private static RewriteRpcServer? _current;

    /// <summary>
    /// The current RPC server instance, or null if not running.
    /// Used by RpcVisitor and Preconditions to delegate to the Java peer.
    /// </summary>
    public static RewriteRpcServer? Current => _current;

    private readonly CSharpParser _parser = new();
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

    public RewriteRpcServer(RecipeMarketplace marketplace)
    {
        _marketplace = marketplace;

        // Register type name overrides for nagoya types that don't match Java names
        RpcSendQueue.RegisterJavaTypeName(typeof(NamespaceDeclaration),
            "org.openrewrite.csharp.tree.Cs$BlockScopeNamespaceDeclaration");
        RpcSendQueue.RegisterJavaTypeName(typeof(CsLambda),
            "org.openrewrite.csharp.tree.Cs$Lambda");
        // Cs-prefixed types in C# that correspond to unprefixed Java names
        RpcSendQueue.RegisterJavaTypeName(typeof(CsMethodDeclaration),
            "org.openrewrite.csharp.tree.Cs$MethodDeclaration");
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

    [JsonRpcMethod("Parse", UseSingleObjectParameterDeserialization = true)]
    public Task<string[]> Parse(ParseRequest request)
    {
        var sourceFileIds = new List<string>();

        if (request.Inputs == null)
        {
            return Task.FromResult(sourceFileIds.ToArray());
        }

        // Resolve assembly references into a Roslyn compilation if provided
        Microsoft.CodeAnalysis.CSharp.CSharpCompilation? compilation = null;
        var syntaxTrees = new List<Microsoft.CodeAnalysis.SyntaxTree>();
        var sourceTexts = new Dictionary<string, string>();

        // First pass: parse all syntax trees
        foreach (var input in request.Inputs)
        {
            string content = input.Text ?? File.ReadAllText(input.SourcePath);
            sourceTexts[input.SourcePath] = content;
            var tree = Microsoft.CodeAnalysis.CSharp.CSharpSyntaxTree.ParseText(content, path: input.SourcePath);
            syntaxTrees.Add(tree);
        }

        // Create compilation if assembly references are provided (even if empty, to get framework refs)
        if (request.AssemblyReferences != null)
        {
            var references = ResolveAssemblyReferences(request.AssemblyReferences);

            compilation = Microsoft.CodeAnalysis.CSharp.CSharpCompilation.Create(
                "ParseCompilation",
                syntaxTrees,
                references,
                new Microsoft.CodeAnalysis.CSharp.CSharpCompilationOptions(
                    Microsoft.CodeAnalysis.OutputKind.DynamicallyLinkedLibrary));
        }

        // Second pass: create LST with optional type attribution
        foreach (var tree in syntaxTrees)
        {
            var sourcePath = tree.FilePath;
            var content = sourceTexts[sourcePath];
            Microsoft.CodeAnalysis.SemanticModel? semanticModel =
                compilation?.GetSemanticModel(tree);

            var cu = _parser.Parse(content, sourcePath, semanticModel);
            var id = cu.Id.ToString();
            _localObjects[id] = cu;
            sourceFileIds.Add(id);
        }

        return Task.FromResult(sourceFileIds.ToArray());
    }

    /// <summary>
    /// Resolves assembly reference strings to MetadataReference objects.
    /// Supports direct DLL paths and includes core framework references.
    /// </summary>
    private List<Microsoft.CodeAnalysis.MetadataReference> ResolveAssemblyReferences(
        List<string> assemblyReferences)
    {
        var references = new List<Microsoft.CodeAnalysis.MetadataReference>();

        // Always include core framework assemblies
        var trustedAssemblies = AppContext.GetData("TRUSTED_PLATFORM_ASSEMBLIES") as string;
        if (trustedAssemblies != null)
        {
            foreach (var assemblyPath in trustedAssemblies.Split(Path.PathSeparator))
            {
                if (File.Exists(assemblyPath))
                {
                    references.Add(Microsoft.CodeAnalysis.MetadataReference.CreateFromFile(assemblyPath));
                }
            }
        }

        // Resolve user-specified references
        foreach (var reference in assemblyReferences)
        {
            if (File.Exists(reference) && reference.EndsWith(".dll", StringComparison.OrdinalIgnoreCase))
            {
                // Direct DLL path
                references.Add(Microsoft.CodeAnalysis.MetadataReference.CreateFromFile(reference));
            }
            else if (reference.Contains('@'))
            {
                // NuGet package with version: "PackageName@Version"
                var parts = reference.Split('@', 2);
                var packageName = parts[0];
                var version = parts[1];

                var dlls = FindPackageAssemblies(packageName, version);
                if (dlls.Count == 0)
                {
                    // Package not in cache — download it
                    var csprojPath = EnsureRecipesProject();
                    RunDotnet($"add \"{csprojPath}\" package {packageName} --version {version}");
                    RunDotnet($"restore \"{csprojPath}\"");
                    dlls = FindPackageAssemblies(packageName, version);
                }

                foreach (var dll in dlls)
                {
                    references.Add(Microsoft.CodeAnalysis.MetadataReference.CreateFromFile(dll));
                }
            }
        }

        return references;
    }

    [JsonRpcMethod("ParseProject", UseSingleObjectParameterDeserialization = true)]
    public Task<List<ParseProjectResponseItem>> ParseProject(ParseProjectRequest request)
    {
        var items = new List<ParseProjectResponseItem>();
        var projectPath = ResolvePath(request.ProjectPath);

        if (!File.Exists(projectPath))
        {
            throw new FileNotFoundException($".csproj not found: {projectPath}");
        }

        var projectDir = Path.GetDirectoryName(projectPath)!;
        var relativeTo = request.RelativeTo != null ? ResolvePath(request.RelativeTo) : projectDir;

        // Read the .csproj to extract PackageReferences
        var doc = XDocument.Load(projectPath);
        var ns = doc.Root?.Name.Namespace ?? XNamespace.None;

        var packageReferences = doc.Descendants(ns + "PackageReference")
            .Select(e =>
            {
                var name = e.Attribute("Include")?.Value ?? "";
                var version = e.Attribute("Version")?.Value;
                return version != null ? $"{name}@{version}" : name;
            })
            .Where(s => !string.IsNullOrEmpty(s))
            .ToList();

        // Discover user source files
        var exclusionSet = new HashSet<string>(request.Exclusions ?? [], StringComparer.OrdinalIgnoreCase);
        var userFiles = DiscoverSourceFiles(projectDir, exclusionSet);

        // Discover generated files from obj/
        var generatedFiles = DiscoverGeneratedFiles(projectDir);

        // Parse all syntax trees, using relative paths as the tree file path
        // so that the CompilationUnit.SourcePath is set correctly
        var syntaxTrees = new List<Microsoft.CodeAnalysis.SyntaxTree>();
        var sourceTexts = new Dictionary<string, string>();
        var generatedRelativePaths = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        foreach (var filePath in userFiles.Concat(generatedFiles))
        {
            string content = File.ReadAllText(filePath);
            var sourcePath = Path.GetRelativePath(relativeTo, filePath);
            sourceTexts[sourcePath] = content;
            // Use the relative path as the tree's file path — the CSharpParser uses
            // node.SyntaxTree.FilePath to set the CompilationUnit.SourcePath
            var tree = Microsoft.CodeAnalysis.CSharp.CSharpSyntaxTree.ParseText(content, path: sourcePath);
            syntaxTrees.Add(tree);

            if (generatedFiles.Contains(filePath))
            {
                generatedRelativePaths.Add(sourcePath);
            }
        }

        // Create compilation with resolved references
        var references = ResolveAssemblyReferences(packageReferences);
        var compilation = Microsoft.CodeAnalysis.CSharp.CSharpCompilation.Create(
            "ParseProjectCompilation",
            syntaxTrees,
            references,
            new Microsoft.CodeAnalysis.CSharp.CSharpCompilationOptions(
                Microsoft.CodeAnalysis.OutputKind.DynamicallyLinkedLibrary));

        // Build LSTs
        foreach (var tree in syntaxTrees)
        {
            var sourcePath = tree.FilePath;
            var content = sourceTexts[sourcePath];
            var semanticModel = compilation.GetSemanticModel(tree);

            var cu = _parser.Parse(content, sourcePath, semanticModel);
            var id = cu.Id.ToString();
            _localObjects[id] = cu;

            items.Add(new ParseProjectResponseItem
            {
                Id = id,
                SourceFileType = "org.openrewrite.csharp.tree.Cs$CompilationUnit",
                Generated = generatedRelativePaths.Contains(sourcePath)
            });
        }

        return Task.FromResult(items);
    }

    /// <summary>
    /// Discovers C# source files in the project directory, excluding bin/, obj/, and user-specified patterns.
    /// </summary>
    private static HashSet<string> DiscoverSourceFiles(string projectDir, HashSet<string> exclusions)
    {
        var files = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        foreach (var file in Directory.EnumerateFiles(projectDir, "*.cs", SearchOption.AllDirectories))
        {
            var relativePath = Path.GetRelativePath(projectDir, file);

            // Skip bin/ and obj/ directories
            if (relativePath.StartsWith("bin" + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase) ||
                relativePath.StartsWith("obj" + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }

            // Check user exclusions
            bool excluded = false;
            foreach (var exclusion in exclusions)
            {
                if (relativePath.Contains(exclusion, StringComparison.OrdinalIgnoreCase))
                {
                    excluded = true;
                    break;
                }
            }

            if (!excluded)
            {
                files.Add(ResolvePath(file));
            }
        }

        return files;
    }

    /// <summary>
    /// Discovers source-generator-produced files in the obj/ directory.
    /// Source generators write output to obj/{Config}/{TFM}/generated/{GeneratorAssembly}/{GeneratorName}/*.cs
    /// </summary>
    private static HashSet<string> DiscoverGeneratedFiles(string projectDir)
    {
        var files = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var objDir = Path.Combine(projectDir, "obj");

        if (!Directory.Exists(objDir))
        {
            return files;
        }

        // Search for generated/ directories under obj/
        // Pattern: obj/{Config}/{TFM}/generated/**/*.cs
        foreach (var dir in Directory.EnumerateDirectories(objDir, "generated", SearchOption.AllDirectories))
        {
            foreach (var file in Directory.EnumerateFiles(dir, "*.cs", SearchOption.AllDirectories))
            {
                files.Add(ResolvePath(file));
            }
        }

        return files;
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
    public async Task<string> Print(PrintRequest request)
    {
        var tree = await GetObjectFromRemoteAsync(request.TreeId, request.SourceFileType);
        var printer = new CSharpPrinter<int>();
        return printer.Print(tree);
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

        var remoteObject = q.Receive(localObject, (Func<object, object>?)null);
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
            var assembly = AssemblyLoadContext.Default.LoadFromAssemblyPath(absolutePath);
            ActivateAssembly(assembly);
        }
        else if (request.Recipes is JObject packageObj)
        {
            var packageName = packageObj["packageName"]?.ToString()
                              ?? throw new ArgumentException("Missing packageName in recipes object");
            version = packageObj["version"]?.ToString();

            if (File.Exists(packageName))
            {
                var assembly = AssemblyLoadContext.Default.LoadFromAssemblyPath(Path.GetFullPath(packageName));
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

                var assemblies = LoadPackageWithDependencies(csprojPath, packageName, version);
                foreach (var assembly in assemblies)
                {
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
        foreach (var type in assembly.GetExportedTypes())
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

    private static string GetNuGetPackagesPath()
    {
        var envPath = Environment.GetEnvironmentVariable("NUGET_PACKAGES");
        if (!string.IsNullOrEmpty(envPath))
            return envPath;

        return Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            ".nuget", "packages");
    }

    /// <summary>
    /// Find DLL paths for a package in the NuGet global packages cache.
    /// </summary>
    private static List<string> FindPackageAssemblies(string packageName, string version)
    {
        var packagesPath = GetNuGetPackagesPath();
        var packageDir = Path.Combine(packagesPath, packageName.ToLowerInvariant(), version, "lib");

        if (!Directory.Exists(packageDir))
            return [];

        // Prefer TFMs in order of compatibility
        string[] tfmPreference = ["net10.0", "net9.0", "net8.0", "net7.0", "net6.0", "netstandard2.1", "netstandard2.0"];

        foreach (var tfm in tfmPreference)
        {
            var tfmDir = Path.Combine(packageDir, tfm);
            if (Directory.Exists(tfmDir))
            {
                return Directory.GetFiles(tfmDir, "*.dll").ToList();
            }
        }

        // If no preferred TFM found, try any available net* folder
        var availableTfms = Directory.GetDirectories(packageDir);
        foreach (var tfmDir in availableTfms)
        {
            var dlls = Directory.GetFiles(tfmDir, "*.dll");
            if (dlls.Length > 0)
                return dlls.ToList();
        }

        return [];
    }

    /// <summary>
    /// Load a NuGet package and its transitive dependencies from the NuGet cache.
    /// Parses obj/project.assets.json to discover all packages in the dependency graph.
    /// Skips assemblies that are already loaded in the current AppDomain.
    /// </summary>
    private static List<Assembly> LoadPackageWithDependencies(string csprojPath, string packageName, string version)
    {
        var loadedAssemblyNames = new HashSet<string>(
            AppDomain.CurrentDomain.GetAssemblies()
                .Select(a => a.GetName().Name!)
                .Where(n => n != null),
            StringComparer.OrdinalIgnoreCase);

        var projectDir = Path.GetDirectoryName(csprojPath)!;
        var assetsPath = Path.Combine(projectDir, "obj", "project.assets.json");
        var loadedAssemblies = new List<Assembly>();

        if (File.Exists(assetsPath))
        {
            // Parse project.assets.json to find all packages and their DLL paths
            var assetsJson = JObject.Parse(File.ReadAllText(assetsPath));
            var libraries = assetsJson["libraries"] as JObject;

            if (libraries != null)
            {
                foreach (var (key, _) in libraries)
                {
                    // key format: "PackageName/Version"
                    var parts = key.Split('/', 2);
                    if (parts.Length != 2) continue;

                    var depName = parts[0];
                    var depVersion = parts[1];

                    var dlls = FindPackageAssemblies(depName, depVersion);
                    foreach (var dll in dlls)
                    {
                        var assemblyName = Path.GetFileNameWithoutExtension(dll);
                        if (loadedAssemblyNames.Contains(assemblyName))
                            continue;

                        try
                        {
                            var assembly = AssemblyLoadContext.Default.LoadFromAssemblyPath(dll);
                            loadedAssemblies.Add(assembly);
                            loadedAssemblyNames.Add(assemblyName);
                        }
                        catch (Exception)
                        {
                            // Skip assemblies that can't be loaded (e.g., platform-specific)
                        }
                    }
                }
            }
        }
        else
        {
            // Fallback: just load the requested package directly from cache
            var dlls = FindPackageAssemblies(packageName, version);
            foreach (var dll in dlls)
            {
                var assemblyName = Path.GetFileNameWithoutExtension(dll);
                if (loadedAssemblyNames.Contains(assemblyName))
                    continue;

                try
                {
                    var assembly = AssemblyLoadContext.Default.LoadFromAssemblyPath(dll);
                    loadedAssemblies.Add(assembly);
                    loadedAssemblyNames.Add(assemblyName);
                }
                catch (Exception)
                {
                    // Skip assemblies that can't be loaded
                }
            }
        }

        return loadedAssemblies;
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

        var ctx = new Core.ExecutionContext();
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
        using var jsonRpc = new JsonRpc(handler);

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
    public Tree VisitOnRemote(string visitorName, string treeId, string? sourceFileType)
    {
        var response = _jsonRpc!.InvokeWithParameterObjectAsync<VisitResponse>(
            "Visit",
            new VisitRequest { VisitorName = visitorName, TreeId = treeId, SourceFileType = sourceFileType }
        ).GetAwaiter().GetResult();

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

// --- Request/Response DTOs ---

public class ParseRequest
{
    public List<ParseInput>? Inputs { get; set; }
    public List<string>? AssemblyReferences { get; set; }
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
    public string Description { get; set; } = "";
    public IReadOnlySet<string>? Tags { get; set; }
    public string? EstimatedEffortPerOccurrence { get; set; }
    public List<OptionDescriptorDto>? Options { get; set; }
    public List<RecipeDescriptorDto>? RecipeList { get; set; }

    public static RecipeDescriptorDto FromDescriptor(RecipeDescriptor d)
    {
        return new RecipeDescriptorDto
        {
            Name = d.Name,
            DisplayName = d.DisplayName,
            Description = d.Description,
            Tags = d.Tags.Count > 0 ? d.Tags : null,
            EstimatedEffortPerOccurrence = d.EstimatedEffortPerOccurrence?.ToString(),
            Options = d.Options.Count > 0
                ? d.Options.Select(OptionDescriptorDto.FromDescriptor).ToList()
                : null,
            RecipeList = d.RecipeList.Count > 0
                ? d.RecipeList.Select(FromDescriptor).ToList()
                : null
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
    public string VisitorName { get; set; } = "";
    public string? SourceFileType { get; set; }
    public string TreeId { get; set; } = "";
    public string? PId { get; set; }
    public List<string>? CursorIds { get; set; }
}

public class VisitResponse
{
    public bool Modified { get; set; }
}

public class ParseProjectRequest
{
    public string ProjectPath { get; set; } = "";
    public List<string>? Exclusions { get; set; }
    public string? RelativeTo { get; set; }
}

public class ParseProjectResponseItem
{
    public string Id { get; set; } = "";
    public string SourceFileType { get; set; } = "";
    public bool Generated { get; set; }
}
