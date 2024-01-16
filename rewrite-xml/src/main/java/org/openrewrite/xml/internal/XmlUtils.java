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

import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class XmlUtils {

    private XmlUtils() {
    }

    private static boolean isNamespaceAttribute(String name) {
        return name.startsWith("xmlns");
    }

    @NonNull
    public static String getAttributeNameFor(String namespacePrefix) {
        return namespacePrefix.isEmpty() ? "xmlns" : "xmlns:" + namespacePrefix;
    }

    @NonNull
    public static String extractNamespacePrefix(String name) {
        if (!isNamespaceAttribute("xmlns")) {
            throw new IllegalArgumentException("Namespace attribute names must start with \"xmlns\".");
        }
        int colon = name.indexOf(':');
        return colon == -1 ? "" : name.substring(colon + 1);
    }

    @NonNull
    public static Map<String, String> extractNamespaces(Collection<Xml.Attribute> attributes) {
        return attributes.isEmpty()
                ? Collections.emptyMap()
                : attributes.stream()
                    .filter(attribute -> isNamespaceAttribute(attribute.getKeyAsString()))
                    .collect(Collectors.toMap(
                            attribute -> extractNamespacePrefix(attribute.getKeyAsString()),
                            attribute -> attribute.getValue().getValue()
                    ));
    }

    @NonNull
    public static Optional<String> findNamespacePrefix(Cursor cursor, String namespacePrefix) {
        String resolvedNamespace = null;
        while (cursor != null) {
            Xml.Tag enclosing = cursor.firstEnclosing(Xml.Tag.class);
            if (enclosing != null) {
                resolvedNamespace = enclosing.getNamespaces().get(namespacePrefix);
                break;
            }
            cursor = cursor.getParent();
        }

        return Optional.ofNullable(resolvedNamespace);
    }
}
