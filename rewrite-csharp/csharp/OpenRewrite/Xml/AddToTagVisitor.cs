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
/// Adds a tag to the children of another tag.
/// Port of Java's org.openrewrite.xml.AddToTagVisitor.
/// </summary>
public class AddToTagVisitor<P>(Tag scope, Tag tagToAdd, Comparison<Content>? tagComparator = null) : XmlVisitor<P>
{
    public override Xml VisitTag(Tag tag, P p)
    {
        if (scope.Id == tag.Id)
        {
            var t = tag;

            // Ensure the tag has a closing element
            if (t.ClosingTag == null)
            {
                t = t.WithClosingTag(new Tag.Closing(
                        Guid.NewGuid(), "\n", Markers.Empty, t.Name, ""))
                    .WithBeforeTagDelimiterPrefix("");
            }

            // Ensure closing tag is on its own line
            if (!t.ClosingTag!.Prefix.Contains('\n'))
            {
                t = t.WithClosingTag(t.ClosingTag.WithPrefix("\n"));
            }

            var formattedTagToAdd = tagToAdd;
            if (!formattedTagToAdd.Prefix.Contains('\n'))
            {
                // Infer indentation from existing children or the closing tag
                var indent = InferChildIndent(t);
                formattedTagToAdd = formattedTagToAdd.WithPrefix("\n" + indent);
            }

            var content = t.ContentList == null ? new List<Content>() : new List<Content>(t.ContentList);
            if (tagComparator != null)
            {
                var i = 0;
                for (; i < content.Count; i++)
                {
                    if (tagComparator(content[i], formattedTagToAdd) > 0)
                    {
                        content.Insert(i, formattedTagToAdd);
                        break;
                    }
                }

                if (i == content.Count)
                {
                    content.Add(formattedTagToAdd);
                }
            }
            else
            {
                content.Add(formattedTagToAdd);
            }

            t = t.WithContentList(content);
            return base.VisitTag(t, p);
        }

        return base.VisitTag(tag, p);
    }

    /// <summary>
    /// Infer the indentation used by existing children of the tag.
    /// Falls back to the closing tag's prefix + 2 spaces, or just 4 spaces.
    /// </summary>
    private static string InferChildIndent(Tag tag)
    {
        // Try to get indent from existing children
        if (tag.ContentList != null)
        {
            foreach (var content in tag.ContentList)
            {
                if (content is Xml child)
                {
                    var prefix = child.Prefix;
                    var lastNewline = prefix.LastIndexOf('\n');
                    if (lastNewline >= 0)
                        return prefix[(lastNewline + 1)..];
                }
            }
        }

        // Infer from closing tag
        if (tag.ClosingTag != null)
        {
            var closingPrefix = tag.ClosingTag.Prefix;
            var lastNewline = closingPrefix.LastIndexOf('\n');
            if (lastNewline >= 0)
                return closingPrefix[(lastNewline + 1)..] + "  ";
        }

        return "    ";
    }
}
