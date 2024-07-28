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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;

import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTagAttribute extends Recipe {

    @Override
    public String getDisplayName() {
        return "Change XML attribute";
    }

    @Override
    public String getDescription() {
        return "Alters XML attribute value on a specified element.";
    }

    @Option(displayName = "Element name",
            description = "The name of the element whose attribute's value is to be changed. Interpreted as an XPath expression.",
            example = "property")
    String elementName;

    @Option(displayName = "Attribute name",
            description = "The name of the attribute whose value is to be changed.",
            example = "name")
    String attributeName;

    @Option(displayName = "New value",
            description = "The new value to be used for key specified by `attributeName`, Set to null if you want to remove the attribute.",
            example = "newfoo.bar.attribute.value.string")
    @Nullable
    String newValue;

    @Option(displayName = "Old value",
            example = "foo.bar.attribute.value.string",
            required = false,
            description = "Only change the property value if it matches the configured `oldValue`.")
    @Nullable
    String oldValue;

    @Option(displayName = "Regex",
            description = "Default false. If true, `oldValue` will be interpreted as a Regular Expression, and capture group contents will be available in `newValue`.",
            required = false)
    @Nullable
    Boolean regex;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (new XPathMatcher(elementName).matches(getCursor())) {
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::visitChosenElementAttribute));
                }
                return t;
            }

            public Xml.Attribute visitChosenElementAttribute(Xml.Attribute attribute) {
                if (!attribute.getKeyAsString().equals(attributeName)) {
                    return attribute;
                }

                String stringValue = attribute.getValueAsString();
                if (oldValue != null) {
                    if (Boolean.TRUE.equals(regex) && !Pattern.matches(oldValue, stringValue)) {
                        return attribute;
                    }
                    if ((regex == null || Boolean.FALSE.equals(regex)) && !stringValue.startsWith(oldValue)) {
                        return attribute;
                    }
                }

                if (newValue == null) {
                    //noinspection DataFlowIssue
                    return null;
                }

                String changedValue = oldValue != null
                        ? (Boolean.TRUE.equals(regex) ? stringValue.replaceAll(oldValue, newValue) : stringValue.replace(oldValue, newValue))
                        : newValue;

                return attribute.withValue(
                        new Xml.Attribute.Value(attribute.getId(),
                                "",
                                attribute.getMarkers(),
                                attribute.getValue().getQuote(),
                                changedValue));
            }
        };
    }
}
