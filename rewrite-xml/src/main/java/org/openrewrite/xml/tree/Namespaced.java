package org.openrewrite.xml.tree;

import org.openrewrite.Cursor;

import java.util.Map;
import java.util.Optional;

public interface Namespaced extends Xml{

    String getName();
    String getLocalName();
    Optional<String> getNamespacePrefix();
    Optional<String> getNamespaceUri(Cursor cursor);
    Map<String, String> getAllNamespaces(Cursor cursor);
}
