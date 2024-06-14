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
package org.openrewrite.xml.internal;

import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class XmlNamespaceUtils {

    public static final String XML_SCHEMA_INSTANCE_PREFIX = "xsi";
    public static final String XML_SCHEMA_INSTANCE_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private XmlNamespaceUtils() {
    }

    public static boolean isNamespaceDefinitionAttribute(String name) {
        return name.startsWith("xmlns");
    }

    public static String getAttributeNameForPrefix(String namespacePrefix) {
        return namespacePrefix.isEmpty() ? "xmlns" : "xmlns:" + namespacePrefix;
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

    /**
     * Extract the namespace prefix from a namespace definition attribute name (xmlns* attributes).
     *
     * @param name the attribute name
     * @return the namespace prefix
     */
    public static String extractPrefixFromNamespaceDefinition(String name) {
        if (!isNamespaceDefinitionAttribute(name)) {
            throw new IllegalArgumentException("Namespace definition attribute names must start with \"xmlns\".");
        }
        return "xmlns".equals(name) ? "" : extractLocalName(name);
    }

    public static Namespaces extractNamespaces(Collection<Xml.Attribute> attributes) {
        final Map<String, String> namespaces = new HashMap<>(attributes.size());
        if (!attributes.isEmpty()) {
                attributes.stream()
                    .filter(attribute -> isNamespaceDefinitionAttribute(attribute.getKeyAsString()))
                    .map(attribute -> new PrefixUri(
                            extractPrefixFromNamespaceDefinition(attribute.getKeyAsString()),
                            attribute.getValueAsString()
                    ))
                    .distinct()
                    .forEach(prefixUri -> namespaces.put(prefixUri.getPrefix(), prefixUri.getUri()));
        }
        return new Namespaces(namespaces);
    }

    /**
     * Gets a map containing all namespaces defined in the current scope, including all parent scopes.
     *
     * @param cursor     the cursor to search from
     * @param currentTag the current tag
     * @return a map containing all namespaces defined in the current scope, including all parent scopes.
     */
    public static Namespaces findNamespaces(Cursor cursor, @Nullable Xml.Tag currentTag) {
        Namespaces namespaces = new Namespaces();
        if (currentTag != null) {
            namespaces = namespaces.combine(currentTag.getNamespaces());
        }

        while (cursor != null) {
            Xml.Tag enclosing = cursor.firstEnclosing(Xml.Tag.class);
            if (enclosing != null) {
                for (Map.Entry<String, String> ns : enclosing.getNamespaces().entrySet()) {
                    if (namespaces.containsUri(ns.getKey())) {
                        throw new IllegalStateException(java.lang.String.format("Cannot have two namespaces with the same prefix (%s): '%s' and '%s'", ns.getKey(), namespaces.get(ns.getKey()), ns.getValue()));
                    }
                    namespaces = namespaces.add(ns.getKey(), ns.getValue());
                }
            }
            cursor = cursor.getParent();
        }

        return namespaces;
    }

    /**
     * Find the tag that contains the declaration of the {@link #XML_SCHEMA_INSTANCE_URI} namespace.
     *
     * @param cursor     the cursor to search from
     * @param currentTag the current tag
     * @return the tag that contains the declaration of the given namespace URI.
     */
    public static Xml.Tag findTagContainingXmlSchemaInstanceNamespace(Cursor cursor, Xml.Tag currentTag) {
        Xml.Tag tag = currentTag;
        if (tag.getNamespaces().containsUri(XML_SCHEMA_INSTANCE_URI)) {
            return tag;
        }
        while (cursor != null) {
            if (cursor.getValue() instanceof Xml.Document) {
                return ((Xml.Document) cursor.getValue()).getRoot();
            }
            tag = cursor.firstEnclosing(Xml.Tag.class);
            if (tag != null) {
                if (tag.getNamespaces().containsUri(XML_SCHEMA_INSTANCE_URI)) {
                    return tag;
                }
            }
            cursor = cursor.getParent();
        }

        // Should never happen
        throw new IllegalArgumentException("Could not find tag containing namespace '" + XML_SCHEMA_INSTANCE_URI + "' or the enclosing Xml.Document instance.");
    }

    @Value
    static class PrefixUri {
        String prefix;
        String uri;
    }
}
