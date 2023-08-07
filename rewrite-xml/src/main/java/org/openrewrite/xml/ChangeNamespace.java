/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeNamespace extends Recipe {

    private static final String XMLNS_PREFIX = "xmlns";

    @Override
    public String getDisplayName() {
        return "Change XML Namespace";
    }

    @Override
    public String getDescription() {
        return "Alters XML Namespace value within all elements.";
    }

    @Option(displayName = "Old value",
            example = "foo.bar.attribute.value.string",
            description = "Only change the property value if it matches the configured `oldValue`.")
    String oldNamespace;

    @Option(displayName = "New value",
            description = "The new value to be used for the namespace.",
            example = "newfoo.bar.attribute.value.string")
    String newNamespace;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (hasNamespace(t)) {
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::visitChosenNamespaceAttribute));
                }
                return t;
            }

            public Xml.Attribute visitChosenNamespaceAttribute(Xml.Attribute attribute) {
                final String valueAsString = attribute.getValueAsString();
                if (!attribute.getKeyAsString().startsWith(XMLNS_PREFIX) && valueAsString.contains(oldNamespace)) {
                    return attribute;
                }

                final String changedValue = valueAsString.replace(oldNamespace, newNamespace);
                return attribute.withValue(
                        new Xml.Attribute.Value(attribute.getId(),
                                "",
                                attribute.getMarkers(),
                                attribute.getValue().getQuote(),
                                changedValue));
            }

            private boolean hasNamespace(Xml.Tag tag) {
                return tag.getAttributes()
                        .stream().
                        anyMatch(a -> a.getKeyAsString().startsWith(XMLNS_PREFIX) && a.getValueAsString().contains(oldNamespace));
            }
        };
    }

}
