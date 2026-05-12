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
/// Visitor that changes the .NET Framework version in legacy .csproj &lt;TargetFrameworkVersion&gt;
/// elements and in app.config &lt;supportedRuntime sku=...&gt; attributes.
/// </summary>
public class ChangeDotNetFrameworkVersionVisitor(string oldVersion, string newVersion) : XmlVisitor<ExecutionContext>
{
    // ".NETFramework,Version=v4.7.2" — the prefix that precedes the version inside the sku attribute.
    private const string SkuVersionPrefix = ".NETFramework,Version=";

    private readonly string _oldCanonical = Canonicalize(oldVersion);
    private readonly string _newCanonical = Canonicalize(newVersion);

    private bool _modified;

    public override Xml.Xml VisitDocument(Document document, ExecutionContext ctx)
    {
        _modified = false;
        var d = (Document)base.VisitDocument(document, ctx);
        if (_modified && d.Markers.FindFirst<MSBuildProject>() != null)
            DoAfterVisit(MSBuildProjectHelper.RegenerateMarkerVisitor());
        return d;
    }

    public override Xml.Xml VisitTag(Tag tag, ExecutionContext ctx)
    {
        var t = (Tag)base.VisitTag(tag, ctx);

        // Legacy csproj: <TargetFrameworkVersion>v4.x.y</TargetFrameworkVersion>
        if (t.Name == "TargetFrameworkVersion")
        {
            var value = t.GetValue();
            if (value != null && Canonicalize(value) == _oldCanonical)
            {
                _modified = true;
                DoAfterVisit(new ChangeTagValueVisitor<ExecutionContext>(t, _newCanonical));
            }
            return t;
        }

        // app.config: <supportedRuntime version="v4.0" sku=".NETFramework,Version=v4.x.y" />
        if (t.Name == "supportedRuntime")
        {
            var sku = t.GetAttributeValue("sku");
            if (sku != null)
            {
                var idx = sku.IndexOf(SkuVersionPrefix, StringComparison.Ordinal);
                if (idx >= 0)
                {
                    var versionStart = idx + SkuVersionPrefix.Length;
                    var versionPart = sku[versionStart..];
                    if (Canonicalize(versionPart) == _oldCanonical)
                    {
                        _modified = true;
                        var newSku = sku[..versionStart] + _newCanonical;
                        return t.ChangeAttribute("sku", newSku);
                    }
                }
            }
        }

        return t;
    }

    // Accept "v4.7.2" or "4.7.2" interchangeably. Canonical form has the leading 'v'.
    private static string Canonicalize(string version)
    {
        var trimmed = version.Trim();
        if (trimmed.Length == 0) return trimmed;
        return trimmed[0] is 'v' or 'V' ? "v" + trimmed[1..] : "v" + trimmed;
    }
}
