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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeNamespaceValue extends Recipe {
    private static final String XMLNS_PREFIX = "xmlns";
    private static final String VERSION_PREFIX = "version";

    @Override
    public String getDisplayName() {
        return "Change XML attribute of a specific resource version";
    }

    @Override
    public String getDescription() {
        return "Alters XML Attribute value within specified element of a specific resource versions.";
    }

    @Nullable
    @Option(displayName = "Element name",
            description = "The name of the element whose attribute's value is to be changed. Interpreted as an XPath Expression.",
            example = "property",
            required = false)
    String elementName;

    @Nullable
    @Option(displayName = "Old value",
            description = "Only change the property value if it matches the configured `oldValue`.",
            example = "newfoo.bar.attribute.value.string",
            required = false)
    String oldValue;

    @Option(displayName = "New value",
            description = "The new value to be used for the namespace.",
            example = "newfoo.bar.attribute.value.string")
    String newValue;

    @Nullable
    @Option(displayName = "Resource version",
            description = "The version of resource to change",
            example = "1.1",
            required = false)
    String versionMatcher;

    @Nullable
    @Option(displayName = "Search All Namespaces",
            description = "Specify whether evaluate all namespaces. Defaults to true",
            example = "true",
            required = false)
    Boolean searchAllNamespaces;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher elementNameMatcher = elementName != null ? new XPathMatcher(elementName) : null;
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (matchesElementName(getCursor()) && matchesVersion(t)) {
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::maybeReplaceNamespaceAttribute));
                }

                return t;
            }

            private boolean matchesElementName(Cursor cursor) {
                return elementNameMatcher == null || elementNameMatcher.matches(cursor);
            }

            private boolean matchesVersion(Xml.Tag tag) {
                if (versionMatcher == null) {
                    return true;
                }
                for (Xml.Attribute attribute : tag.getAttributes()) {
                    if (isVersionAttribute(attribute) && isVersionMatch(attribute)) {
                        return true;
                    }
                }
                return false;
            }

            private Xml.Attribute maybeReplaceNamespaceAttribute(Xml.Attribute attribute) {
                if (isXmlnsAttribute(attribute) && isOldValue(attribute)) {
                    return attribute.withValue(
                            new Xml.Attribute.Value(attribute.getId(),
                                    "",
                                    attribute.getMarkers(),
                                    attribute.getValue().getQuote(),
                                    newValue));
                }
                return attribute;
            }

            private boolean isXmlnsAttribute(Xml.Attribute attribute) {
                boolean searchAll = searchAllNamespaces == null || Boolean.TRUE.equals(searchAllNamespaces);
                return searchAll && attribute.getKeyAsString().startsWith(XMLNS_PREFIX) ||
                       !searchAll && attribute.getKeyAsString().equals(XMLNS_PREFIX);
            }

            private boolean isVersionAttribute(Xml.Attribute attribute) {
                return attribute.getKeyAsString().startsWith(VERSION_PREFIX);
            }

            private boolean isOldValue(Xml.Attribute attribute) {
                return oldValue == null || attribute.getValueAsString().equals(oldValue);
            }

            private boolean isVersionMatch(Xml.Attribute attribute) {
                String[] versions = versionMatcher.split(",");
                double dversion = Double.parseDouble(attribute.getValueAsString());
                for (String splitVersion : versions) {
                    boolean checkGreaterThan = false;
                    double dversionExpected;
                    if (splitVersion.endsWith("+")) {
                        splitVersion = splitVersion.substring(0, splitVersion.length() - 1);
                        checkGreaterThan = true;
                    }
                    try {
                        dversionExpected = Double.parseDouble(splitVersion);
                    } catch (NumberFormatException e) {
                        return false;
                    }
                    if (!checkGreaterThan && dversionExpected == dversion || checkGreaterThan && dversionExpected <= dversion) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
