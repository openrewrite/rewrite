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
/// Visitor that removes a single named switch from the value attribute of
/// &lt;AppContextSwitchOverrides&gt; in app.config / web.config.
/// </summary>
public class RemoveAppContextSwitchVisitor(string switchName) : XmlVisitor<ExecutionContext>
{
    public override Xml.Xml VisitTag(Tag tag, ExecutionContext ctx)
    {
        var t = (Tag)base.VisitTag(tag, ctx);

        if (t.Name != "AppContextSwitchOverrides")
            return t;

        var value = t.GetAttributeValue("value");
        if (value == null)
            return t;

        var (newValue, removed) = StripSwitch(value, switchName);
        if (!removed)
            return t;

        if (newValue.Length == 0)
        {
            DoAfterVisit(new RemoveContentVisitor<ExecutionContext>(t, true, false));
            return t;
        }

        return t.ChangeAttribute("value", newValue);
    }

    // Returns the value with the named switch removed, plus a flag indicating whether
    // a removal actually happened. Switch names are matched exactly (case-sensitive),
    // ignoring whitespace around tokens.
    private static (string Value, bool Removed) StripSwitch(string value, string targetName)
    {
        var entries = value.Split(';');
        var kept = new List<string>(entries.Length);
        var removed = false;

        foreach (var entry in entries)
        {
            var trimmed = entry.Trim();
            if (trimmed.Length == 0) continue;

            var eq = trimmed.IndexOf('=');
            var name = eq >= 0 ? trimmed[..eq].Trim() : trimmed;

            if (name == targetName)
            {
                removed = true;
                continue;
            }

            kept.Add(trimmed);
        }

        return (string.Join(";", kept), removed);
    }
}
