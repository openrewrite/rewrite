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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp.Recipes;

/// <summary>
/// Visitor that upgrades NuGet package reference versions in .csproj and Directory.Packages.props files.
/// Handles PackageReference, PackageVersion (CPM), and property-based versioning.
/// Can be used standalone in custom recipe edit phases when resolved versions are known.
/// </summary>
public class UpgradeNuGetPackageVersionVisitor(
    string packageName,
    Dictionary<string, string> resolvedVersions,
    Dictionary<string, Dictionary<string, string>> propertyUpdates)
    : XmlVisitor<ExecutionContext>
{
    private bool _modified;

    public override Xml.Xml VisitDocument(Document document, ExecutionContext ctx)
    {
        _modified = false;
        var d = (Document)base.VisitDocument(document, ctx);
        if (!ReferenceEquals(d, document))
        {
            _modified = true;
        }
        if (_modified)
        {
            DoAfterVisit(MSBuildProjectHelper.RegenerateMarkerVisitor());
        }
        return d;
    }

    public override Xml.Xml VisitTag(Tag tag, ExecutionContext ctx)
    {
        var t = (Tag)base.VisitTag(tag, ctx);

        if (t.Name == "PackageReference")
        {
            var include = t.GetAttributeValue("Include");
            if (include != null && GlobMatcher.Matches(include, packageName))
            {
                var targetVersion = resolvedVersions.GetValueOrDefault(include);
                if (targetVersion != null)
                {
                    var versionAttr = t.GetAttributeValue("Version");
                    if (versionAttr != null && !IsPropertyReference(versionAttr) && versionAttr != targetVersion)
                    {
                        t = t.ChangeAttribute("Version", targetVersion);
                    }

                    if (versionAttr == null)
                    {
                        var versionTag = t.GetChild("Version");
                        if (versionTag != null)
                        {
                            var currentValue = versionTag.GetValue() ?? "";
                            if (currentValue != targetVersion)
                                DoAfterVisit(new ChangeTagValueVisitor<ExecutionContext>(versionTag, targetVersion));
                        }
                    }
                }
            }
        }

        if (t.Name == "PackageVersion")
        {
            var include = t.GetAttributeValue("Include");
            if (include != null && GlobMatcher.Matches(include, packageName))
            {
                var targetVersion = resolvedVersions.GetValueOrDefault(include);
                if (targetVersion != null)
                {
                    var versionAttr = t.GetAttributeValue("Version");
                    if (versionAttr != null && !IsPropertyReference(versionAttr) && versionAttr != targetVersion)
                        t = t.ChangeAttribute("Version", targetVersion);
                }
            }
        }

        // Handle property definitions
        var sourcePath = Cursor.FirstEnclosing<Document>()?.SourcePath;
        if (sourcePath != null && propertyUpdates.TryGetValue(sourcePath, out var propsForFile)
                               && propsForFile.TryGetValue(t.Name, out var propTargetVersion))
        {
            var currentValue = t.GetValue() ?? "";
            if (propTargetVersion != currentValue)
                DoAfterVisit(new ChangeTagValueVisitor<ExecutionContext>(t, propTargetVersion));
        }

        return t;
    }

    private static bool IsPropertyReference(string? value)
        => value != null && value.StartsWith("$(") && value.EndsWith(")");
}
