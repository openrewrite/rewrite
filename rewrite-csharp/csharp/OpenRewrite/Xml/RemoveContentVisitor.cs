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
/// Removes a Content element from a tag's children.
/// Optionally removes empty ancestors and preceding comments.
/// Port of Java's org.openrewrite.xml.RemoveContentVisitor.
/// </summary>
public class RemoveContentVisitor<P>(Content scope, bool removeEmptyAncestors, bool removePrecedingComment) : XmlVisitor<P>
{
    public override Xml VisitTag(Tag tag, P p)
    {
        var t = (Tag)base.VisitTag(tag, p);

        if (t.ContentList != null)
        {
            foreach (var content in t.ContentList)
            {
                if (content is Tree tree && tree.Id == scope.Id)
                {
                    var contents = new List<Content>(t.ContentList);
                    var indexOf = contents.IndexOf(content);
                    contents.RemoveAt(indexOf);

                    if (removePrecedingComment && indexOf > 0 && contents[indexOf - 1] is Comment)
                    {
                        DoAfterVisit(new RemoveContentVisitor<P>(contents[indexOf - 1], true, removePrecedingComment));
                    }

                    if (removeEmptyAncestors && contents.Count == 0 && t.Attributes.Count == 0)
                    {
                        if (Cursor.Parent?.Value is Document)
                        {
                            return t.WithContentList(null).WithClosingTag(null);
                        }

                        DoAfterVisit(new RemoveContentVisitor<P>(t, true, removePrecedingComment));
                        return t.WithContentList(null).WithClosingTag(null);
                    }
                    else
                    {
                        return t.WithContentList(contents);
                    }
                }
            }
        }

        return t;
    }
}
