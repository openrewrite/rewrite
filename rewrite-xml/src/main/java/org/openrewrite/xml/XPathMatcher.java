/*
 * Copyright 2020 the original author or authors.
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

import org.openrewrite.Cursor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    // Regular expression to support conditional tags like `plugin[artifactId='maven-compiler-plugin']`
    private static final Pattern PATTERN = Pattern.compile("([-\\w]+)\\[([-\\w]+)='([-\\w.]+)']");
    private final String expression;

    public XPathMatcher(String expression) {
        this.expression = expression;
    }

    /**
     * Checks if the given XPath expression matches the provided cursor.
     *
     * @param cursor the cursor representing the XML document
     * @return true if the expression matches the cursor, false otherwise
     */
    public boolean matches(Cursor cursor) {
        List<Xml.Tag> path = new ArrayList<>();
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof Xml.Tag) {
                path.add(c.getValue());
            }
        }

        if (expression.startsWith("//") || !expression.startsWith("/")) {
            List<String> parts = new ArrayList<>(Arrays.asList((expression.startsWith("//") ?
                    expression.substring(2) : expression).split("/")));
            Collections.reverse(parts);

            int pathIndex = 0;
            for (int i = 0; i < parts.size(); i++, pathIndex++) {
                String part = parts.get(i);
                if (part.startsWith("@")) {
                    if (!(cursor.getValue() instanceof Xml.Attribute &&
                          (((Xml.Attribute) cursor.getValue()).getKeyAsString().equals(part.substring(1))) ||
                          "*".equals(part.substring(1)))) {
                        return false;
                    }

                    pathIndex--;
                    continue;
                }

                if (path.size() < i + 1 || (!path.get(pathIndex).getName().equals(part) && !"*".equals(part))) {
                    return false;
                }
            }

            return expression.startsWith("/") || path.size() - pathIndex <= 1;
        } else if (expression.startsWith("/")) {
            Collections.reverse(path);
            String[] parts = expression.substring(1).split("/");

            // Deal with the two forward slashes in the expression; works, but I'm not proud of it.
            if (expression.contains("//") && Arrays.stream(parts).anyMatch(StringUtils::isBlank)) {
                int blankPartIndex = Arrays.asList(parts).indexOf("");
                int doubleSlashIndex = expression.indexOf("//");

                if (path.size() > blankPartIndex && path.size() >= parts.length - 1) {
                    String newExpression;
                    if (Objects.equals(path.get(blankPartIndex).getName(), parts[blankPartIndex + 1])) {
                        newExpression = String.format(
                                "%s/%s",
                                expression.substring(0, doubleSlashIndex),
                                expression.substring(doubleSlashIndex + 2)
                        );
                    } else {
                        newExpression = String.format(
                                "%s/%s/%s",
                                expression.substring(0, doubleSlashIndex),
                                path.get(blankPartIndex).getName(),
                                expression.substring(doubleSlashIndex + 2)
                        );
                    }
                    return new XPathMatcher(newExpression).matches(cursor);
                }
            }

            if (parts.length > path.size() + 1) {
                return false;
            }

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];

                Xml.Tag tag = i < path.size() ? path.get(i) : null;
                String partName;

                Matcher matcher;
                if (tag != null && part.endsWith("]") && (matcher = PATTERN.matcher(part)).matches()) {
                    String name = matcher.group(1);
                    String subTag = matcher.group(2);
                    String subTagValue = matcher.group(3);

                    boolean matchCondition =
                            FindTags.find(tag, subTag).stream().anyMatch(t ->
                                    t.getValue().map(v -> v.equals(subTagValue)).orElse(false)
                            );
                    if (!matchCondition) {
                        return false;
                    }
                    partName = name;
                } else {
                    partName = part;
                }

                if (part.startsWith("@")) {
                    return cursor.getValue() instanceof Xml.Attribute &&
                           (((Xml.Attribute) cursor.getValue()).getKeyAsString().equals(part.substring(1)) ||
                            "*".equals(part.substring(1)));
                }

                if (path.size() < i + 1 || (tag != null && !tag.getName().equals(partName) && !"*".equals(part))) {
                    return false;
                }
            }

            return cursor.getValue() instanceof Xml.Tag && path.size() == parts.length;
        }

        return false;
    }
}
