/*
 * Copyright 2020 the original author or authors.
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
import org.jspecify.annotations.Nullable;
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

    @Override
    public String getDisplayName() {
        return "Add new XML attribute for an Element";
    }

    @Override
    public String getDescription() {
        return "Add new XML attribute with value on a specified element.";
    }

    @Option(displayName = "Element name",
            description = "The name of the element whose attribute's value is to be added. Interpreted as an XPath expression.",
            example = "property")
    String elementName;

    @Option(displayName = "Attribute name",
            description = "The name of the new attribute.",
            example = "name")
    String attributeName;

    @Option(displayName = "New value",
            description = "The new value to be used for key specified by `attributeName`, Set to null if you want to remove the attribute.",
            example = "newfoo.bar.attribute.value.string")
    @Nullable
    String newValue;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);

                // Match the tag using XPath
                if (new XPathMatcher(elementName).matches(getCursor())) {
                    // Check if the attribute already exists
                    boolean attributeExists = t.getAttributes().stream()
                            .anyMatch(attr -> attr.getKeyAsString().equals(attributeName));

                    // If attribute doesn't exist, add the new attribute
                    if (!attributeExists && newValue != null) {
                        Xml.Attribute newAttribute = new Xml.Attribute(
                                randomId(),
                                "",
                                Markers.EMPTY,
                                new Xml.Ident(
                                        randomId(),
                                        " ",
                                        Markers.EMPTY,
                                        attributeName
                                ),
                                "",
                                new Xml.Attribute.Value(
                                        randomId(),
                                        "",
                                        Markers.EMPTY,
                                        Xml.Attribute.Value.Quote.Double,
                                        newValue

                                )
                        );

                        t = t.withAttributes(ListUtils.concat(t.getAttributes(), newAttribute));
                    }
                }

                return t;
            }
        };
    }

}
