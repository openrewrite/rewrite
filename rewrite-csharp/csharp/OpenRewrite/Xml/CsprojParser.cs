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

namespace OpenRewrite.Xml;

/// <summary>
/// Parses .csproj files as XML and attaches an MSBuildProject marker with
/// project metadata extracted from the XML structure and project.assets.json.
/// </summary>
public class CsprojParser
{
    private static readonly HashSet<string> CsprojExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        "csproj", "vbproj", "fsproj"
    };

    private readonly XmlParser _xmlParser = new();

    /// <summary>
    /// Parses a .csproj file from source text and attaches an MSBuildProject marker.
    /// </summary>
    /// <param name="sourceStr">The .csproj XML content.</param>
    /// <param name="sourcePath">The file path. Defaults to "project.csproj".</param>
    /// <param name="rootDir">The root directory for resolving project.assets.json. If null, only XML extraction is used.</param>
    public Document Parse(string sourceStr, string sourcePath = "project.csproj", string? rootDir = null)
    {
        var doc = _xmlParser.Parse(sourceStr, sourcePath);
        var marker = MSBuildProjectHelper.CreateMarker(doc, rootDir);
        if (marker != null)
        {
            doc = doc.WithMarkers(doc.Markers.Add(marker));
        }
        return doc;
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
}
