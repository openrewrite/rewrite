/*
 * Copyright 2025 the original author or authors.
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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Xml.Recipes;

/// <summary>
/// A simple XML recipe that replaces text content in CharData nodes.
/// Used for testing the XML RPC bridge.
/// </summary>
public class ChangeXmlCharData : Recipe
{
    public override string DisplayName => "Change XML CharData text";
    public override string Description => "Replaces occurrences of OldText with NewText in XML CharData nodes.";

    [Option(DisplayName = "Old text", Description = "The text to search for")]
    public string OldText { get; set; } = "";

    [Option(DisplayName = "New text", Description = "The replacement text")]
    public string NewText { get; set; } = "";

    public override ITreeVisitor<ExecutionContext> GetVisitor() => new ChangeCharDataVisitor(OldText, NewText);

    private class ChangeCharDataVisitor(string oldText, string newText) : XmlVisitor<ExecutionContext>
    {
        public override Xml VisitCharData(CharData charData, ExecutionContext ctx)
        {
            if (charData.Text == oldText)
            {
                return charData.WithText(newText);
            }
            return charData;
        }
    }
}

/// <summary>
/// A recipe that changes an XML attribute value by key name.
/// Used for testing the XML RPC bridge.
/// </summary>
public class ChangeXmlAttribute : Recipe
{
    public override string DisplayName => "Change XML attribute value";
    public override string Description => "Changes the value of attributes matching AttrName to NewValue.";

    [Option(DisplayName = "Attribute name", Description = "The attribute name to match")]
    public string AttrName { get; set; } = "";

    [Option(DisplayName = "New value", Description = "The new attribute value")]
    public string NewValue { get; set; } = "";

    public override ITreeVisitor<ExecutionContext> GetVisitor() => new ChangeAttributeVisitor(AttrName, NewValue);

    private class ChangeAttributeVisitor(string attrName, string newValue) : XmlVisitor<ExecutionContext>
    {
        public override Xml VisitAttribute(Attribute attribute, ExecutionContext ctx)
        {
            if (attribute.Key.Name == attrName)
            {
                return attribute.WithVal(attribute.Val.WithVal(newValue));
            }
            return base.VisitAttribute(attribute, ctx);
        }
    }
}
