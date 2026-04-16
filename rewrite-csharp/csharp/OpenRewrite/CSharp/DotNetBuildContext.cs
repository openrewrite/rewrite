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
using OpenRewrite.Core;
using OpenRewrite.Xml;
using Serilog;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp;

/// <summary>
/// Captures the key build files from the repository so they can be
/// materialized to disk during reattestation (dotnet restore).
///
/// Stores raw LST <see cref="Document"/> objects for files in the LST stream
/// (printed only at materialization time) and raw string content for files
/// captured from disk that are not part of the LST stream.
///
/// Populated in two ways:
/// 1. From disk during ParseSolution (captures Directory.Build.props, .targets,
///    nuget.config, and other files not in the LST stream)
/// 2. From LSTs during the scan phase of <see cref="CsProjRecipe"/> implementations
///    (captures the raw Document objects being visited)
///
/// Retrieved by <see cref="MSBuildProjectHelper"/> when regenerating MSBuildProject markers.
/// </summary>
public class DotNetBuildContext
{
    private const string ContextKey = "org.openrewrite.csharp.DotNetBuildContext";

    /// <summary>
    /// File extensions and names that are needed for a correct dotnet restore.
    /// </summary>
    private static readonly HashSet<string> BuildFileExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        ".csproj", ".vbproj", ".fsproj",
        ".sln", ".slnx",
        ".props", ".targets"
    };

    private static readonly HashSet<string> BuildFileNames = new(StringComparer.OrdinalIgnoreCase)
    {
        "nuget.config"
    };

    /// <summary>
    /// LST documents captured during the scan phase. Printed at materialization time.
    /// </summary>
    public Dictionary<string, Document> Documents { get; } = new();

    /// <summary>
    /// Raw string content for files captured from disk (not in the LST stream).
    /// </summary>
    private readonly Dictionary<string, string> _diskFiles = new();

    private readonly object _lock = new();

    /// <summary>
    /// Gets or creates the DotNetBuildContext from the ExecutionContext.
    /// </summary>
    public static DotNetBuildContext GetOrCreate(ExecutionContext ctx)
    {
        return ctx.ComputeMessageIfAbsent(ContextKey, _ => new DotNetBuildContext());
    }

    /// <summary>
    /// Gets the DotNetBuildContext if one exists, or null.
    /// </summary>
    public static DotNetBuildContext? Get(ExecutionContext ctx)
    {
        return ctx.GetMessage<DotNetBuildContext>(ContextKey);
    }

    /// <summary>
    /// Returns true if the given source path is a build-related file that should be captured.
    /// </summary>
    public static bool IsBuildFile(string sourcePath)
    {
        var fileName = Path.GetFileName(sourcePath);
        if (BuildFileNames.Contains(fileName))
            return true;

        var ext = Path.GetExtension(sourcePath);
        return ext.Length > 0 && BuildFileExtensions.Contains(ext);
    }

    /// <summary>
    /// Captures the raw LST Document if it is a build-related file.
    /// The document is stored as-is; printing is deferred to materialization.
    /// </summary>
    public void CaptureIfBuildFile(Document doc)
    {
        if (!IsBuildFile(doc.SourcePath))
            return;

        lock (_lock)
        {
            Documents[doc.SourcePath] = doc;
        }
    }

    /// <summary>
    /// Captures a file with explicit path and content (for files not in the LST stream).
    /// </summary>
    public void Capture(string relativePath, string content)
    {
        lock (_lock)
        {
            _diskFiles[relativePath] = content;
        }
    }

    /// <summary>
    /// Scans a root directory for build-related files and captures their content.
    /// Call this during ParseSolution to capture files that may not appear in the LST stream
    /// (e.g., Directory.Build.props, Directory.Build.targets, nuget.config).
    /// </summary>
    /// <param name="rootDir">The repository root directory to scan.</param>
    public void CaptureFromDisk(string rootDir)
    {
        try
        {
            foreach (var file in Directory.EnumerateFiles(rootDir, "*", SearchOption.AllDirectories))
            {
                // Skip common non-build directories
                var relativePath = Path.GetRelativePath(rootDir, file);
                if (relativePath.Contains($"{Path.DirectorySeparatorChar}bin{Path.DirectorySeparatorChar}") ||
                    relativePath.Contains($"{Path.DirectorySeparatorChar}obj{Path.DirectorySeparatorChar}") ||
                    relativePath.Contains($"{Path.DirectorySeparatorChar}.git{Path.DirectorySeparatorChar}") ||
                    relativePath.Contains($"{Path.DirectorySeparatorChar}node_modules{Path.DirectorySeparatorChar}") ||
                    relativePath.StartsWith($"bin{Path.DirectorySeparatorChar}") ||
                    relativePath.StartsWith($"obj{Path.DirectorySeparatorChar}") ||
                    relativePath.StartsWith($".git{Path.DirectorySeparatorChar}"))
                    continue;

                if (!IsBuildFile(file))
                    continue;

                try
                {
                    var content = File.ReadAllText(file);
                    // Normalize to forward slashes for consistency with SourcePath
                    var normalizedPath = relativePath.Replace(Path.DirectorySeparatorChar, '/');
                    lock (_lock)
                    {
                        _diskFiles.TryAdd(normalizedPath, content);
                    }
                }
                catch (Exception ex)
                {
                    Log.Debug("DotNetBuildContext: failed to read {Path}: {Error}", file, ex.Message);
                }
            }

            Log.Debug("DotNetBuildContext: captured {Count} build files from {RootDir}", _diskFiles.Count, rootDir);
        }
        catch (Exception ex)
        {
            Log.Debug("DotNetBuildContext: failed to scan {RootDir}: {Error}", rootDir, ex.Message);
        }
    }

    /// <summary>
    /// Stores this DotNetBuildContext into the given ExecutionContext.
    /// </summary>
    public void StoreIn(ExecutionContext ctx)
    {
        ctx.PutMessage(ContextKey, this);
    }

    /// <summary>
    /// Writes all captured files to the given root directory, preserving
    /// their relative path structure. LST documents are printed at this point.
    /// </summary>
    /// <param name="rootDir">The directory to write files into.</param>
    /// <param name="exclude">Optional path to exclude (e.g., the .csproj already written by the caller).</param>
    public void MaterializeAll(string rootDir, string? exclude = null)
    {
        Dictionary<string, Document> lstFiles;
        Dictionary<string, string> diskFiles;
        lock (_lock)
        {
            lstFiles = new Dictionary<string, Document>(Documents);
            diskFiles = new Dictionary<string, string>(_diskFiles);
        }

        // Collect all paths that have LST versions (they take precedence)
        var lstPaths = new HashSet<string>(lstFiles.Keys, StringComparer.OrdinalIgnoreCase);

        // Write disk-captured files first (skipping any that have LST versions)
        foreach (var (relativePath, content) in diskFiles)
        {
            if (exclude != null && string.Equals(relativePath, exclude, StringComparison.OrdinalIgnoreCase))
                continue;
            if (lstPaths.Contains(relativePath))
                continue;

            WriteFile(rootDir, relativePath, content);
        }

        // Write LST-captured files (printed at materialization time)
        foreach (var (relativePath, doc) in lstFiles)
        {
            if (exclude != null && string.Equals(relativePath, exclude, StringComparison.OrdinalIgnoreCase))
                continue;

            var content = XmlParser.Print(doc);
            WriteFile(rootDir, relativePath, content);
        }
    }

    private static void WriteFile(string rootDir, string relativePath, string content)
    {
        var fullPath = Path.Combine(rootDir, relativePath);
        var dir = Path.GetDirectoryName(fullPath);
        if (dir != null)
            Directory.CreateDirectory(dir);

        File.WriteAllText(fullPath, content);
    }
}
