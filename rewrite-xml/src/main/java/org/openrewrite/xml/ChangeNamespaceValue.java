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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.trait.Namespaced;
import org.openrewrite.xml.tree.Xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toMap;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeNamespaceValue extends Recipe {
    private static final String XMLNS_PREFIX = "xmlns";
    private static final String VERSION_PREFIX = "version";
    private static final String SCHEMA_LOCATION_MATCH_PATTERN = "(?m)(.*)(%s)(\\s+)(.*)";
    private static final String SCHEMA_LOCATION_REPLACEMENT_PATTERN = "$1%s$3%s";

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
    @Option(displayName = "Search all namespaces",
            description = "Specify whether evaluate all namespaces. Defaults to true",
            example = "true",
            required = false)
    Boolean searchAllNamespaces;

    @Nullable
    @Option(displayName = "New Resource version",
            description = "The new version of the resource",
            example = "2.0")
    String newVersion;

    @Option(displayName = "Schema location",
            description = "The new value to be used for the namespace schema location.",
            example = "newfoo.bar.attribute.value.string",
            required = false)
    @Nullable
    String newSchemaLocation;

    public static final String XML_SCHEMA_INSTANCE_PREFIX = "xsi";
    public static final String XML_SCHEMA_INSTANCE_URI = "http://www.w3.org/2001/XMLSchema-instance";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher elementNameMatcher = elementName != null ? new XPathMatcher(elementName) : null;
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);
                if (d != document) {
                    d = d.withRoot(addOrUpdateSchemaLocation(d.getRoot()));
                }
                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (matchesElementName(getCursor()) && matchesVersion(t)) {
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::maybeReplaceNamespaceAttribute));
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::maybeReplaceVersionAttribute));
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

            private Xml.Attribute maybeReplaceVersionAttribute(Xml.Attribute attribute) {
                if (isVersionAttribute(attribute) && newVersion != null) {
                    return attribute.withValue(
                            new Xml.Attribute.Value(attribute.getId(),
                                    "",
                                    attribute.getMarkers(),
                                    attribute.getValue().getQuote(),
                                    newVersion));
                }
                return attribute;
            }

            private boolean isXmlnsAttribute(Xml.Attribute attribute) {
                boolean searchAll = searchAllNamespaces == null || Boolean.TRUE.equals(searchAllNamespaces);
                return searchAll && attribute.getKeyAsString().startsWith(XMLNS_PREFIX) ||
                       !searchAll && XMLNS_PREFIX.equals(attribute.getKeyAsString());
            }

            private boolean isVersionAttribute(Xml.Attribute attribute) {
                return attribute.getKeyAsString().startsWith(VERSION_PREFIX);
            }

            private boolean isOldValue(Xml.Attribute attribute) {
                return oldValue == null || attribute.getValueAsString().equals(oldValue);
            }

            private boolean isVersionMatch(Xml.Attribute attribute) {
                if (versionMatcher == null) {
                    return true;
                }
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

            private Xml.Tag addOrUpdateSchemaLocation(Xml.Tag root) {
                if (StringUtils.isBlank(newSchemaLocation)) {
                    return root;
                }
                Xml.Tag newRoot = maybeAddNamespace(root);
                Namespaced n = new Namespaced(new Cursor(null, newRoot));
                Optional<Xml.Attribute> maybeSchemaLocation = maybeGetSchemaLocation(n);
                if (maybeSchemaLocation.isPresent() && oldValue != null) {
                    newRoot = updateSchemaLocation(newRoot, maybeSchemaLocation.get());
                } else if (!maybeSchemaLocation.isPresent()) {
                    newRoot = addSchemaLocation(newRoot);
                }
                return newRoot;
            }

            private Optional<Xml.Attribute> maybeGetSchemaLocation(Namespaced n) {
                Map<String, String> namespaces = n.getNamespaces();
                for (Xml.Attribute attribute : n.getAttributes()) {
                    String attributeNamespace = namespaces.get(Namespaced.extractNamespacePrefix(attribute.getKeyAsString()));
                    if(XML_SCHEMA_INSTANCE_URI.equals(attributeNamespace) &&
                       attribute.getKeyAsString().endsWith("schemaLocation")) {
                        return Optional.of(attribute);
                    }
                }

                return Optional.empty();
            }
            private Xml.Tag maybeAddNamespace(Xml.Tag root) {
                Namespaced n = new Namespaced(new Cursor(null, root));
                Map<String, String> namespaces = n.getNamespaces();
                if (namespaces.containsValue(newValue) && !namespaces.containsValue(XML_SCHEMA_INSTANCE_URI)) {
                    Map<String, String> newNamespaces = new LinkedHashMap<>(namespaces);
                    newNamespaces.put(XML_SCHEMA_INSTANCE_PREFIX, XML_SCHEMA_INSTANCE_URI);
                    root = withNamespaces(root, newNamespaces);
                }
                return root;
            }

            public Xml.Tag withNamespaces(Xml.Tag tag, Map<String, String> namespaces) {
                List<Xml.Attribute> attributes = tag.getAttributes();
                if (attributes.isEmpty()) {
                    for (Map.Entry<String, String> ns : namespaces.entrySet()) {
                        String key = Namespaced.getAttributeNameForPrefix(ns.getKey());
                        attributes = ListUtils.concat(attributes, new Xml.Attribute(
                                randomId(),
                                "",
                                Markers.EMPTY,
                                new Xml.Ident(
                                        randomId(),
                                        "",
                                        Markers.EMPTY,
                                        key
                                ),
                                "",
                                new Xml.Attribute.Value(
                                        randomId(),
                                        "",
                                        Markers.EMPTY,
                                        Xml.Attribute.Value.Quote.Double, ns.getValue()
                                )
                        ));
                    }
                } else {
                    Map<String, Xml.Attribute> attributeByKey = attributes.stream()
                            .collect(toMap(
                                    Xml.Attribute::getKeyAsString,
                                    a -> a
                            ));

                    for (Map.Entry<String, String> ns : namespaces.entrySet()) {
                        String key = Namespaced.getAttributeNameForPrefix(ns.getKey());
                        if (attributeByKey.containsKey(key)) {
                            Xml.Attribute attribute = attributeByKey.get(key);
                            if (!ns.getValue().equals(attribute.getValueAsString())) {
                                ListUtils.map(attributes, a -> a.getKeyAsString().equals(key) ?
                                        attribute.withValue(new Xml.Attribute.Value(randomId(), "", Markers.EMPTY, Xml.Attribute.Value.Quote.Double, ns.getValue())) :
                                        a
                                );
                            }
                        } else {
                            attributes = ListUtils.concat(attributes, new Xml.Attribute(
                                    randomId(),
                                    " ",
                                    Markers.EMPTY,
                                    new Xml.Ident(
                                            randomId(),
                                            "",
                                            Markers.EMPTY,
                                            key
                                    ),
                                    "",
                                    new Xml.Attribute.Value(
                                            randomId(),
                                            "",
                                            Markers.EMPTY,
                                            Xml.Attribute.Value.Quote.Double, ns.getValue()
                                    )
                            ));
                        }
                    }
                }
                return tag.withAttributes(attributes);
            }

            private Xml.Tag updateSchemaLocation(Xml.Tag newRoot, Xml.Attribute attribute) {
                if(oldValue == null) {
                    return newRoot;
                }
                String oldSchemaLocation = attribute.getValueAsString();
                Matcher pattern = Pattern.compile(String.format(SCHEMA_LOCATION_MATCH_PATTERN, Pattern.quote(oldValue)))
                        .matcher(oldSchemaLocation);
                if (pattern.find()) {
                    String newSchemaLocationValue = pattern.replaceFirst(
                            String.format(SCHEMA_LOCATION_REPLACEMENT_PATTERN, newValue, newSchemaLocation)
                    );
                    Xml.Attribute newAttribute = attribute.withValue(attribute.getValue().withValue(newSchemaLocationValue));
                    newRoot = newRoot.withAttributes(ListUtils.map(newRoot.getAttributes(), a -> a == attribute ? newAttribute : a));
                }
                return newRoot;
            }

            private Xml.Tag addSchemaLocation(Xml.Tag newRoot) {
                return newRoot.withAttributes(
                        ListUtils.concat(
                                newRoot.getAttributes(),
                                new Xml.Attribute(
                                        randomId(),
                                        " ",
                                        Markers.EMPTY,
                                        new Xml.Ident(
                                                randomId(),
                                                "",
                                                Markers.EMPTY,
                                                String.format("%s:schemaLocation", XML_SCHEMA_INSTANCE_PREFIX)
                                        ),
                                        "",
                                        new Xml.Attribute.Value(
                                                randomId(),
                                                "",
                                                Markers.EMPTY,
                                                Xml.Attribute.Value.Quote.Double,
                                                String.format("%s %s", newValue, newSchemaLocation)
                                        )
                                )
                        )
                );
            }
        };
    }
}
