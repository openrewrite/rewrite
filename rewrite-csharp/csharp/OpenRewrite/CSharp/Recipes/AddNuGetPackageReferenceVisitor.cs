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
/// Visitor that adds a NuGet PackageReference to .csproj files if not already present.
/// Can be used standalone in custom recipe edit phases.
/// </summary>
public class AddNuGetPackageReferenceVisitor(string packageName, string? version) : XmlVisitor<ExecutionContext>
{
    private bool _alreadyPresent;
    private Tag? _lastItemGroup;

    public override Xml.Xml VisitDocument(Document document, ExecutionContext ctx)
    {
        _alreadyPresent = false;
        _lastItemGroup = null;

        // Check if already present via marker
        var marker = document.Markers.FindFirst<MSBuildProject>();
        if (marker != null)
        {
            foreach (var tfm in marker.TargetFrameworks)
            {
                foreach (var pkgRef in tfm.PackageReferences)
                {
                    if (packageName == pkgRef.Include)
                    {
                        _alreadyPresent = true;
                        break;
                    }
                }
                if (_alreadyPresent) break;
            }
        }

        if (_alreadyPresent)
            return document;

        var d = (Document)base.VisitDocument(document, ctx);

        if (_alreadyPresent)
            return d;

        // Build the new PackageReference tag
        var tag = version != null
            ? $"<PackageReference Include=\"{packageName}\" Version=\"{version}\" />"
            : $"<PackageReference Include=\"{packageName}\" />";
        var newRef = TagExtensions.BuildTag(tag);

        if (_lastItemGroup != null)
        {
            DoAfterVisit(new AddToTagVisitor<ExecutionContext>(_lastItemGroup, newRef));
        }
        else
        {
            var itemGroup = TagExtensions.BuildTag(
                $"<ItemGroup>\n    {tag}\n  </ItemGroup>");
            DoAfterVisit(new AddToTagVisitor<ExecutionContext>(d.Root, itemGroup));
        }

        DoAfterVisit(MSBuildProjectHelper.RegenerateMarkerVisitor());
        return d;
    }

    public override Xml.Xml VisitTag(Tag tag, ExecutionContext ctx)
    {
        var t = (Tag)base.VisitTag(tag, ctx);

        // Check XML directly for idempotency
        if (t.Name == "PackageReference")
        {
            var include = t.GetAttributeValue("Include");
            if (packageName == include)
                _alreadyPresent = true;
        }

        // Track the last ItemGroup that contains PackageReference elements
        if (t.Name == "ItemGroup")
        {
            var hasPackageRef = false;
            foreach (var child in t.GetChildren())
            {
                if (child.Name == "PackageReference")
                {
                    hasPackageRef = true;
                    break;
                }
            }
            if (hasPackageRef)
                _lastItemGroup = t;
        }

        return t;
    }
}
