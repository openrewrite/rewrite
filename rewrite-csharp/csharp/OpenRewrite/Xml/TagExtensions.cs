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
/// Extension methods for XML Tag manipulation.
/// </summary>
public static class TagExtensions
{
    /// <summary>
    /// Builds a Tag from an XML string snippet.
    /// Equivalent to Java's Xml.Tag.build(String).
    /// </summary>
    public static Tag BuildTag(string xmlSource)
    {
        var parser = new XmlParser();
        var doc = parser.Parse(xmlSource);
        return doc.Root;
    }

    /// <summary>
    /// Gets the first child tag with the given name.
    /// Equivalent to Java's Xml.Tag.getChild(String).
    /// </summary>
    public static Tag? GetChild(this Tag tag, string name)
    {
        if (tag.ContentList == null) return null;
        foreach (var content in tag.ContentList)
        {
            if (content is Tag child && child.Name == name)
                return child;
        }
        return null;
    }

    /// <summary>
    /// Gets the text value of a tag (its CharData content).
    /// Equivalent to Java's Xml.Tag.getValue().
    /// </summary>
    public static string? GetValue(this Tag tag)
    {
        if (tag.ContentList == null) return null;
        foreach (var content in tag.ContentList)
        {
            if (content is CharData charData)
                return charData.Text;
        }
        return null;
    }

    /// <summary>
    /// Gets the value of an attribute by name (case-insensitive).
    /// </summary>
    public static string? GetAttributeValue(this Tag tag, string attrName)
    {
        foreach (var attr in tag.Attributes)
        {
            if (string.Equals(attr.Key.Name, attrName, StringComparison.OrdinalIgnoreCase))
                return attr.Val.Val;
        }
        return null;
    }

    /// <summary>
    /// Returns a new tag with the specified attribute's value changed.
    /// </summary>
    public static Tag ChangeAttribute(this Tag tag, string attrName, string newValue)
    {
        var attrs = new List<Attribute>(tag.Attributes);
        for (var i = 0; i < attrs.Count; i++)
        {
            if (string.Equals(attrs[i].Key.Name, attrName, StringComparison.OrdinalIgnoreCase))
            {
                attrs[i] = attrs[i].WithVal(attrs[i].Val.WithVal(newValue));
                return tag.WithAttributes(attrs);
            }
        }
        return tag;
    }

    /// <summary>
    /// Gets all direct child tags.
    /// </summary>
    public static IEnumerable<Tag> GetChildren(this Tag tag)
    {
        if (tag.ContentList == null) yield break;
        foreach (var content in tag.ContentList)
        {
            if (content is Tag child)
                yield return child;
        }
    }
}
