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
    public static String extractNamespacePrefix(String name) {
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
