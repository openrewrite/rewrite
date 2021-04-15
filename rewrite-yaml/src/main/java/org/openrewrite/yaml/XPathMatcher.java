package org.openrewrite.yaml;

import org.openrewrite.Cursor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Supports a limited set of XPath expressions, specifically those
 * documented on <a href="https://www.w3schools.com/xml/xpath_syntax.asp">this page</a>.
 * <p>
 * Used for checking whether a visitor's cursor meets a certain XPath expression.
 * <p>
 * The "current node" for XPath evaluation is always the root node of the document.
 * As a result, '.' and '..' are not recognized.
 */
public class XPathMatcher {
    private final String expression;

    public XPathMatcher(String expression) {
        this.expression = expression;
    }

    public boolean matches(Cursor cursor) {
        List<Yaml.Mapping.Entry> path = cursor.getPathAsStream()
                .filter(p -> p instanceof Yaml.Mapping.Entry)
                .map(Yaml.Mapping.Entry.class::cast)
                .collect(Collectors.toList());

        if (expression.startsWith("//") || !expression.startsWith("/")) {
            List<String> parts = new ArrayList<>(Arrays.asList((expression.startsWith("//") ?
                    expression.substring(2) : expression).split("/")));
            Collections.reverse(parts);

            int pathIndex = 0;
            for (int i = 0; i < parts.size(); i++, pathIndex++) {
                String part = parts.get(i);
                if (path.size() < i + 1 || (!path.get(pathIndex).getKey().getValue().equals(part) &&
                        !part.equals("*"))) {
                    return false;
                }
            }

            return expression.startsWith("/") || path.size() - pathIndex <= 1;
        } else if (expression.startsWith("/")) {
            Collections.reverse(path);

            String[] parts = expression.substring(1).split("/");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (path.size() < i + 1 || (!path.get(i).getKey().getValue().equals(part) &&
                        !part.equals("*"))) {
                    return false;
                }
            }

            return cursor.getValue() instanceof Yaml.Mapping.Entry && path.size() == parts.length;
        }

        return false;
    }
}
