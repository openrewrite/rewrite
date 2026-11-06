/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.xml.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import javax.xml.XMLConstants;
import java.util.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

@Value
public class Namespaced implements Trait<Xml> {

    public static final String XML_SCHEMA_INSTANCE_URI = "http://www.w3.org/2001/XMLSchema-instance";

    Cursor cursor;

    public Optional<String> getName() {
        Optional<String> maybeName = Optional.empty();
        if (cursor.getValue() instanceof Xml.Tag) {
            maybeName = Optional.of(((Xml.Tag) cursor.getValue()).getName());
        } else if (cursor.getValue() instanceof Xml.Attribute) {
            Xml.Attribute attribute = cursor.getValue();
            maybeName = Optional.of(attribute.getKey().getName());
        }
        return maybeName;
    }

    public Optional<String> getLocalName() {
        Optional<String> maybeLocalName = Optional.empty();
        if (cursor.getValue() instanceof Xml.Tag) {
            Xml.Tag tag = cursor.getValue();
            maybeLocalName = Optional.of(extractLocalName(tag.getName()));
        } else if (cursor.getValue() instanceof Xml.Attribute) {
            Xml.Attribute attribute = cursor.getValue();
            maybeLocalName = Optional.of(extractLocalName(attribute.getKeyAsString()));
        }
        return maybeLocalName;
    }

    public Optional<String> getNamespacePrefix() {
        String extractedNamespacePrefix = null;
        if (cursor.getValue() instanceof Xml.Tag) {
            Xml.Tag tag = cursor.getValue();
            extractedNamespacePrefix = extractNamespacePrefix(tag.getName());
        } else if (cursor.getValue() instanceof Xml.Attribute) {
            Xml.Attribute attribute = cursor.getValue();
            extractedNamespacePrefix = extractNamespacePrefix(attribute.getKeyAsString());
        }
        return StringUtils.isBlank(extractedNamespacePrefix) ?
                Optional.empty() :
                Optional.of(extractedNamespacePrefix);
    }

    /**
     * @return The namespace URI bound to this tag's or attribute's prefix in the current scope. An
     * unprefixed tag is in the default namespace, and an unprefixed attribute is in no namespace.
     * Reserved bindings need no declaration. A namespace declaration is in the xmlns namespace, and
     * the xml prefix is bound to the XML namespace.
     */
    public Optional<String> getNamespaceUri() {
        Object value = cursor.getValue();
        if (value instanceof Xml.Attribute && isNamespaceDefinitionAttribute(((Xml.Attribute) value).getKeyAsString())) {
            return Optional.of(XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
        }
        String prefix = getNamespacePrefix().orElse("");
        if (prefix.isEmpty() && !(value instanceof Xml.Tag)) {
            return Optional.empty();
        }
        String uri = getAllNamespaces().get(prefix);
        if (uri == null && XMLConstants.XML_NS_PREFIX.equals(prefix)) {
            return Optional.of(XMLConstants.XML_NS_URI);
        }
        // An empty namespace name, as in xmlns="", undeclares the binding
        return Optional.ofNullable(uri).filter(u -> !u.isEmpty());
    }

    public Map<String, String> getNamespaces() {
        Map<String, String> namespaces = emptyMap();
        if (cursor.getValue() instanceof Xml.Tag) {
            Xml.Tag tag = cursor.getValue();
            if (!tag.getAttributes().isEmpty()) {
                namespaces = new LinkedHashMap<>(tag.getAttributes().size());
                for (Xml.Attribute attribute : tag.getAttributes()) {
                    if (isNamespaceDefinitionAttribute(attribute.getKeyAsString())) {
                        namespaces.put(
                                extractPrefixFromNamespaceDefinition(attribute.getKeyAsString()),
                                attribute.getValueAsString());
                    }
                }
            }
        } else if (cursor.getValue() instanceof Xml.Attribute) {
            Xml.Attribute attribute = cursor.getValue();
            String prefix = extractPrefixFromNamespaceDefinition(attribute.getKeyAsString());
            if (prefix != null) {
                namespaces = singletonMap(prefix, attribute.getValueAsString());
            }
        }
        return namespaces;
    }

    public List<Xml.Attribute> getSchemaLocations() {
        if (cursor.getValue() instanceof Xml.Tag) {
            Xml.Tag tag = cursor.getValue();
            if (tag.getAttributes().isEmpty()) {
                return emptyList();
            }
            List<Xml.Attribute> schemaLocations = new ArrayList<>();
            for (Xml.Attribute attribute : tag.getAttributes()) {
                String localName = extractLocalName(attribute.getKeyAsString());
                if (("schemaLocation".equals(localName) || "noNamespaceSchemaLocation".equals(localName)) &&
                    XML_SCHEMA_INSTANCE_URI.equals(new Namespaced(new Cursor(cursor, attribute)).getNamespaceUri().orElse(null))) {
                    schemaLocations.add(attribute);
                }
            }
            return schemaLocations;
        }
        return emptyList();
    }

    public List<Xml.Attribute> getAttributes() {
        if (cursor.getValue() instanceof Xml.Tag) {
            Xml.Tag tag = cursor.getValue();
            return tag.getAttributes();
        } else if (cursor.getValue() instanceof Xml.Attribute) {
            return singletonList(cursor.getValue());
        }
        return emptyList();
    }

    public List<String> attributePrefixes() {
        return getAttributes().stream()
                .map(Xml.Attribute::getKeyAsString)
                .map(Namespaced::extractNamespacePrefix)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(toList());

    }

    /**
     * @return All namespaces in the current scope, including those defined in parent scopes. A
     * declaration overrides one of the same prefix in an enclosing scope.
     */
    public Map<String, String> getAllNamespaces() {
        Map<String, String> result = cursor.getParent() == null ?
                new LinkedHashMap<>() :
                new LinkedHashMap<>(new Namespaced(cursor.getParent()).getAllNamespaces());
        result.putAll(getNamespaces());
        return result;
    }

    public static boolean isNamespaceDefinitionAttribute(String name) {
        return "xmlns".equals(name) || name.startsWith("xmlns:");
    }

    public static String getAttributeNameForPrefix(String namespacePrefix) {
        return namespacePrefix.isEmpty() ? "xmlns" : "xmlns:" + namespacePrefix;
    }

    /**
     * Extract the namespace prefix from a namespace definition attribute name (xmlns* attributes).
     *
     * @param name the attribute name or null if not a namespace definition attribute
     * @return the namespace prefix
     */
    public static @Nullable String extractPrefixFromNamespaceDefinition(String name) {
        if (!isNamespaceDefinitionAttribute(name)) {
            return null;
        }
        return "xmlns".equals(name) ? "" : extractLocalName(name);
    }

    /**
     * Extract the namespace prefix from a tag or attribute name.
     *
     * @param name the tag or attribute name
     * @return the namespace prefix (empty string for the default namespace)
     */
    public static String extractNamespacePrefix(String name) {
        int colon = name.indexOf(':');
        return colon == -1 ? "" : name.substring(0, colon);
    }

    /**
     * Extract the local name from a tag or attribute name.
     *
     * @param name the tag or attribute name
     * @return the local name
     */
    public static String extractLocalName(String name) {
        int colon = name.indexOf(':');
        return colon == -1 ? name : name.substring(colon + 1);
    }

    public static Matcher matcher() {
        return new Matcher();
    }

    public static class Matcher extends SimpleTraitMatcher<Namespaced> {
        @Nullable
        private String prefix;

        @Nullable
        private String uri;

        @Nullable
        private XPathMatcher xPath;

        public Matcher prefix(@Nullable String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Matcher uri(@Nullable String uri) {
            this.uri = uri;
            return this;
        }

        public Matcher xPath(@Nullable String xPath) {
            if(xPath != null) {
                this.xPath = new XPathMatcher(xPath);
            }
            return this;
        }

        public Matcher xPath(@Nullable XPathMatcher xPath) {
            this.xPath = xPath;
            return this;
        }

        @Override
        protected @Nullable Namespaced test(Cursor cursor) {
            if (xPath != null && !xPath.matches(cursor)) {
                return null;
            }
            Namespaced namespaced = new Namespaced(cursor);
            if (uri != null || prefix != null) {
                Map<String, String> namespaces = namespaced.getNamespaces();
                if ((uri != null && !namespaces.containsValue(uri)) ||
                    (prefix != null && !namespaces.containsKey(prefix))) {
                    return null;
                }
            }

            return namespaced;
        }
    }
}
