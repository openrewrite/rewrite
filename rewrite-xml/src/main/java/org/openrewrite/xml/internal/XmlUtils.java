package org.openrewrite.xml.internal;

import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class XmlUtils {

    private XmlUtils() {
    }

    private static boolean isNamespaceAttribute(String name) {
        return name.startsWith("xmlns");
    }

    private static String extractNamespacePrefix(String name) {
        int colon = name.indexOf(':');
        return colon == -1 ? "" : name.substring(colon + 1);
    }

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
}
