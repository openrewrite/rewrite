/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.xml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddTagAttribute extends Recipe {

    String displayName = "Add new XML attribute for an Element";

    String description = "Add new XML attribute with value on a specified element.";

    @Option(displayName = "Element name",
            description = "The name of the element whose attribute's value is to be added. Interpreted as an XPath expression.",
            example = "//beans/bean")
    String elementName;

    @Option(displayName = "Attribute name",
            description = "The name of the new attribute.",
            example = "attribute-name")
    String attributeName;

    @Option(displayName = "New value",
            description = "The new value to be used for key specified by `attributeName`.",
            example = "value-to-add")
    String newValue;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (!new XPathMatcher(elementName).matches(getCursor())) {
                    return t;
                }

                for (Xml.Attribute attr : t.getAttributes()) {
                    if (attributeName.equals(attr.getKeyAsString())) {
                        return t;
                    }
                }

                Xml.Ident name = new Xml.Ident(randomId(), "", Markers.EMPTY, attributeName);
                Xml.Attribute.Value value = new Xml.Attribute.Value(randomId(), "", Markers.EMPTY, Xml.Attribute.Value.Quote.Double, newValue);
                return t.withAttributes(ListUtils.concat(t.getAttributes(),
                        new Xml.Attribute(randomId(), " ", Markers.EMPTY, name, "", value)));
            }
        };
    }
}
