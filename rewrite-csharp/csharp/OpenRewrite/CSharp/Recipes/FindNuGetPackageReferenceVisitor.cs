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
/// Visitor that searches for .csproj files referencing a specific NuGet package.
/// Supports glob patterns for the package name.
/// Can be used standalone as a precondition in custom recipe edit phases.
/// </summary>
public class FindNuGetPackageReferenceVisitor(string packageName) : XmlVisitor<ExecutionContext>
{
    public override Xml.Xml VisitDocument(Document document, ExecutionContext ctx)
    {
        var marker = document.Markers.FindFirst<MSBuildProject>();
        if (marker != null)
        {
            foreach (var tfm in marker.TargetFrameworks)
            {
                foreach (var pkgRef in tfm.PackageReferences)
                {
                    if (GlobMatcher.Matches(pkgRef.Include, packageName))
                        return document.WithMarkers(
                            document.Markers.Add(new SearchResult(Guid.NewGuid(), null)));
                }
            }
        }
        return document;
    }
}
