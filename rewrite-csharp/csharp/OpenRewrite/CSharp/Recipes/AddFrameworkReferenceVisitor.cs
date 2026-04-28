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
/// Visitor that adds a <c>&lt;FrameworkReference&gt;</c> to a .csproj file root if
/// one with a matching Include doesn't already exist. Skips projects whose Sdk
/// attribute already implicitly imports the same framework
/// (<c>Microsoft.NET.Sdk.Web</c> implicitly references
/// <c>Microsoft.AspNetCore.App</c>).
/// </summary>
public class AddFrameworkReferenceVisitor(string frameworkName, string? triggerPackageGlob = null) : XmlVisitor<ExecutionContext>
{
    private bool _alreadyPresent;
    private bool _triggerMatched;

    public override Xml.Xml VisitDocument(Document document, ExecutionContext ctx)
    {
        _alreadyPresent = false;
        _triggerMatched = string.IsNullOrEmpty(triggerPackageGlob);

        // Skip when the SDK already imports the same framework implicitly.
        var sdk = document.Root.GetAttributeValue("Sdk");
        if (sdk != null && SdkImplicitlyReferences(sdk, frameworkName))
            return document;

        var d = (Document)base.VisitDocument(document, ctx);

        if (_alreadyPresent || !_triggerMatched)
            return d;

        var tag = $"<FrameworkReference Include=\"{frameworkName}\" />";
        var itemGroup = TagExtensions.BuildTag(
            $"<ItemGroup>\n    {tag}\n  </ItemGroup>");
        DoAfterVisit(new AddToTagVisitor<ExecutionContext>(d.Root, itemGroup));
        DoAfterVisit(MSBuildProjectHelper.RegenerateMarkerVisitor());
        return d;
    }

    public override Xml.Xml VisitTag(Tag tag, ExecutionContext ctx)
    {
        var t = (Tag)base.VisitTag(tag, ctx);
        if (t.Name == "FrameworkReference")
        {
            var include = t.GetAttributeValue("Include");
            if (include == frameworkName)
                _alreadyPresent = true;
        }
        else if (!string.IsNullOrEmpty(triggerPackageGlob) && t.Name == "PackageReference")
        {
            var include = t.GetAttributeValue("Include");
            if (include != null && GlobMatcher.Matches(include, triggerPackageGlob!))
                _triggerMatched = true;
        }
        return t;
    }

    private static bool SdkImplicitlyReferences(string sdk, string framework)
    {
        return sdk switch
        {
            "Microsoft.NET.Sdk.Web" => framework is "Microsoft.AspNetCore.App" or "Microsoft.NETCore.App",
            "Microsoft.NET.Sdk.Worker" => framework is "Microsoft.AspNetCore.App" or "Microsoft.NETCore.App",
            "Microsoft.NET.Sdk.BlazorWebAssembly" => framework is "Microsoft.AspNetCore.App" or "Microsoft.NETCore.App",
            _ => false
        };
    }
}
