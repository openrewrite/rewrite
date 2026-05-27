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
/// Visitor that changes the targetFramework attribute on &lt;httpRuntime&gt; and
/// &lt;compilation&gt; elements in web.config and app.config files.
/// </summary>
public class ChangeWebConfigTargetFrameworkVisitor(string oldVersion, string newVersion) : XmlVisitor<ExecutionContext>
{
    private readonly string _oldCanonical = Canonicalize(oldVersion);
    private readonly string _newCanonical = Canonicalize(newVersion);

    public override Xml.Xml VisitTag(Tag tag, ExecutionContext ctx)
    {
        var t = (Tag)base.VisitTag(tag, ctx);

        if (t.Name == "httpRuntime" || t.Name == "compilation")
        {
            var current = t.GetAttributeValue("targetFramework");
            if (current != null && Canonicalize(current) == _oldCanonical)
                return t.ChangeAttribute("targetFramework", _newCanonical);
        }

        return t;
    }

    // Strips a leading 'v' / 'V' so "4.7.2" and "v4.7.2" both match the no-prefix
    // form used in web.config / app.config attribute values.
    private static string Canonicalize(string version)
    {
        var trimmed = version.Trim();
        if (trimmed.Length == 0) return trimmed;
        return trimmed[0] is 'v' or 'V' ? trimmed[1..] : trimmed;
    }
}
