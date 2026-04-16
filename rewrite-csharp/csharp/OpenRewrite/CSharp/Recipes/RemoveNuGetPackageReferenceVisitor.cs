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
/// Visitor that removes a NuGet PackageReference from .csproj files.
/// Supports glob patterns for the package name.
/// Can be used standalone in custom recipe edit phases.
/// </summary>
public class RemoveNuGetPackageReferenceVisitor(string packageName) : XmlVisitor<ExecutionContext>
{
    private bool _modified;

    public override Xml.Xml VisitDocument(Document document, ExecutionContext ctx)
    {
        _modified = false;
        var d = (Document)base.VisitDocument(document, ctx);
        if (_modified)
            DoAfterVisit(MSBuildProjectHelper.RegenerateMarkerVisitor());
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
                _modified = true;
                DoAfterVisit(new RemoveContentVisitor<ExecutionContext>(t, true, false));
            }
        }
        return t;
    }
}
