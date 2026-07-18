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
using OpenRewrite.CSharp;
using Serilog;

namespace OpenRewrite.Xml;

/// <summary>
/// Parses .csproj files as XML and attaches an MSBuildProject marker with project metadata
/// from an in-process NuGet restore (PackageSpec/RestoreRunner → in-memory LockFile).
/// Files are written to a temp directory so MSBuild imports (Directory.Build.props,
/// .targets, packages.config, nuget.config) resolve correctly during restore-graph
/// generation — but no child process is spawned and no project.assets.json is read.
/// </summary>
public class CsprojParser
{
    private static readonly HashSet<string> CsprojExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        "csproj", "vbproj", "fsproj", "props", "targets"
    };

    private static readonly HashSet<string> ProjectExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        "csproj", "vbproj", "fsproj"
    };

    private readonly XmlParser _xmlParser = new();

    /// <summary>
    /// Parses a single .csproj file from source text and attaches an MSBuildProject marker.
    /// Convenience wrapper around <see cref="ParseAll"/> for single-file use.
    /// </summary>
    public Document Parse(string sourceStr, string sourcePath = "project.csproj")
    {
        return ParseAll([(sourceStr, sourcePath)])[0];
    }

    /// <summary>
    /// Parses multiple csproj/props/targets (and supporting packages.config/nuget.config) files
    /// together. Writes all files to a shared temp directory so MSBuild imports resolve, then
    /// resolves each project's dependency graph in-process. Returns parsed Documents in the
    /// same order as input. Only project files (csproj/vbproj/fsproj) receive a marker.
    /// </summary>
    public IList<Document> ParseAll(IList<(string sourceStr, string sourcePath)> files)
    {
        if (files.Count == 0) return [];

        string? tempDir = null;
        try
        {
            tempDir = Path.Combine(Path.GetTempPath(),
                "openrewrite-csproj-" + Guid.NewGuid().ToString("N")[..8]);
            Directory.CreateDirectory(tempDir);

            // Write all files to the shared temp directory
            foreach (var (sourceStr, sourcePath) in files)
            {
                var filePath = Path.Combine(tempDir, sourcePath);
                var dir = Path.GetDirectoryName(filePath);
                if (dir != null) Directory.CreateDirectory(dir);
                File.WriteAllText(filePath, sourceStr);
            }

            // Parse each file — project files get a marker from the in-process restore
            var results = new List<Document>(files.Count);
            foreach (var (sourceStr, sourcePath) in files)
            {
                var doc = _xmlParser.Parse(sourceStr, sourcePath);
                if (IsProjectFile(sourcePath))
                {
                    var marker = MSBuildProjectHelper.CreateMarker(doc, tempDir);
                    if (marker != null)
                        doc = doc.WithMarkers(doc.Markers.Add(marker));
                }
                else if (Accept(sourcePath))
                {
                    // props/targets carry a basic marker (no restore attempt) — recipes use
                    // marker presence as the "this is an MSBuild build file" gate.
                    var marker = MSBuildProjectHelper.CreateMarker(doc);
                    if (marker != null)
                        doc = doc.WithMarkers(doc.Markers.Add(marker));
                }
                results.Add(doc);
            }

            return results;
        }
        catch (Exception ex)
        {
            Log.Debug("CsprojParser: failed to parse files: {Error}", ex.Message);
            // Fall back to basic markers (SDK info only)
            var results = new List<Document>(files.Count);
            foreach (var (sourceStr, sourcePath) in files)
            {
                var doc = _xmlParser.Parse(sourceStr, sourcePath);
                if (Accept(sourcePath))
                {
                    var marker = MSBuildProjectHelper.CreateMarker(doc);
                    if (marker != null)
                        doc = doc.WithMarkers(doc.Markers.Add(marker));
                }
                results.Add(doc);
            }
            return results;
        }
        finally
        {
            if (tempDir != null)
            {
                try { Directory.Delete(tempDir, true); }
                catch { /* best effort cleanup */ }
            }
        }
    }

    public bool Accept(string path)
    {
        var dot = path.LastIndexOf('.');
        if (dot > 0 && dot < path.Length - 1)
        {
            return CsprojExtensions.Contains(path[(dot + 1)..]);
        }
        return false;
    }

    /// <summary>
    /// Returns true for actual project files (.csproj/.vbproj/.fsproj) that participate in
    /// restore, as opposed to .props/.targets files which cannot be restored independently.
    /// </summary>
    private static bool IsProjectFile(string path)
    {
        var dot = path.LastIndexOf('.');
        if (dot > 0 && dot < path.Length - 1)
        {
            return ProjectExtensions.Contains(path[(dot + 1)..]);
        }
        return false;
    }
}
