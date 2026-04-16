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
/// Visitor that changes the target framework in .csproj files.
/// Handles both single-TFM (&lt;TargetFramework&gt;) and multi-TFM (&lt;TargetFrameworks&gt;) elements.
/// Can be used standalone in custom recipe edit phases.
/// </summary>
public class ChangeDotNetTargetFrameworkVisitor(string oldTfm, string newTfm) : XmlVisitor<ExecutionContext>
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

        if (t.Name == "TargetFramework")
        {
            var value = t.GetValue() ?? "";
            if (oldTfm == value)
            {
                _modified = true;
                DoAfterVisit(new ChangeTagValueVisitor<ExecutionContext>(t, newTfm));
            }
        }
        else if (t.Name == "TargetFrameworks")
        {
            var value = t.GetValue() ?? "";
            var frameworks = value.Split(';');
            var changed = false;
            var seen = new LinkedList<string>();
            foreach (var framework in frameworks)
            {
                var fw = framework.Trim();
                if (oldTfm == fw)
                {
                    changed = true;
                    fw = newTfm;
                }
                if (!seen.Contains(fw))
                    seen.AddLast(fw);
            }

            if (changed)
            {
                _modified = true;
                DoAfterVisit(new ChangeTagValueVisitor<ExecutionContext>(
                    t, string.Join(";", seen)));
            }
        }

        return t;
    }
}
