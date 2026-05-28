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

namespace OpenRewrite.Xml;

/// <summary>
/// Changes the text value of a tag. Preserves prefix and afterText when replacing existing CharData.
/// Port of Java's org.openrewrite.xml.ChangeTagValueVisitor.
/// </summary>
public class ChangeTagValueVisitor<P>(Tag? scope, string? value) : XmlVisitor<P>
{
    public override Xml VisitTag(Tag tag, P p)
    {
        var t = (Tag)base.VisitTag(tag, p);
        if (scope != null && scope.Id == t.Id)
        {
            if (value == null)
            {
                DoAfterVisit(new RemoveContentVisitor<P>(t, false, true));
                return tag;
            }

            var prefix = "";
            var afterText = "";
            if (t.ContentList is { Count: 1 } && t.ContentList[0] is CharData existingValue)
            {
                if (existingValue.Text == value)
                    return tag;

                prefix = existingValue.Prefix;
                afterText = existingValue.AfterText;
            }

            t = t.WithContentList(new List<Content>
            {
                new CharData(Guid.NewGuid(), prefix, Markers.Empty, false, value, afterText)
            });
        }

        return t;
    }
}
