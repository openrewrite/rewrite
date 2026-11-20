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
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Format;

namespace OpenRewrite.Test;

/// <summary>
/// The .editorconfig files a test gives with its sources, written to a directory of their own so that
/// <see cref="EditorConfigResolver"/> reads them as it reads those of a repository.
/// </summary>
internal sealed class EditorConfigSources : IDisposable
{
    private readonly string _root =
        Path.Combine(Path.GetTempPath(), "rewrite-test-editorconfig-" + Guid.NewGuid().ToString("N"));

    private readonly EditorConfigResolver _resolver;

    public EditorConfigSources(IEnumerable<SourceSpec> specs)
    {
        foreach (var spec in specs)
        {
            var path = Path.Combine(_root, spec.SourcePath!);
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            File.WriteAllText(path, spec.Before);
        }
        _resolver = new EditorConfigResolver(_root);
    }

    public static bool Is(SourceSpec spec) =>
        spec.SourcePath != null &&
        Path.GetFileName(spec.SourcePath).Equals(".editorconfig", StringComparison.OrdinalIgnoreCase);

    /// <summary><paramref name="cu"/> with the style the .editorconfig files give <paramref name="sourcePath"/>.</summary>
    public CompilationUnit Style(CompilationUnit cu, string sourcePath) =>
        cu.WithMarkers(cu.Markers.Add(_resolver.Resolve(Path.Combine(_root, sourcePath), cu)));

    public void Dispose()
    {
        try
        {
            Directory.Delete(_root, recursive: true);
        }
        catch (IOException)
        {
            // A leftover temporary directory fails no test
        }
    }
}
