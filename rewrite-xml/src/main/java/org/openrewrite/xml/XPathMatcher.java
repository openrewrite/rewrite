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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrewrite.Cursor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.xml.tree.Xml.Attribute;
import org.openrewrite.xml.tree.Xml.Tag;

/**
 * Supports a limited set of XPath expressions, specifically those documented on <a
 * href="https://www.w3schools.com/xml/xpath_syntax.asp">this page</a>.
 * <p>
 * Used for checking whether a visitor's cursor meets a certain XPath expression.
 * <p>
 * The "current node" for XPath evaluation is always the root node of the document. As a result, '.' and '..' are not
 * recognized.
 */
public class XPathMatcher {

    // Regular expression to support conditional tags like `plugin[artifactId='maven-compiler-plugin']` or foo[@bar='baz']
    private static final Pattern PATTERN = Pattern.compile("([-\\w]+)\\[(@)?([-\\w]+)='([-\\w.]+)']");

    private final String expression;
    private final boolean startsWithSlash;
    private final boolean startsWithDoubleSlash;
    private final String[] parts;

    public XPathMatcher(String expression) {
        this.expression = expression;
        startsWithSlash = expression.startsWith("/");
        startsWithDoubleSlash = expression.startsWith("//");
        parts = expression.substring(startsWithDoubleSlash ? 2 : startsWithSlash ? 1 : 0).split("/");
    }

    /**
     * Checks if the given XPath expression matches the provided cursor.
     *
     * @param cursor the cursor representing the XML document
     * @return true if the expression matches the cursor, false otherwise
     */
    public boolean matches(Cursor cursor) {
        List<Tag> path = new ArrayList<>();
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof Tag) {
                path.add(c.getValue());
            }
        }

        if (startsWithDoubleSlash || !startsWithSlash) {
            int pathIndex = 0;
            for (int i = parts.length - 1; i >= 0; i--, pathIndex++) {
                String part = parts[i];

                //todo anpassen --> attribute und normale conditions

                String partWithCondition = null;
                Tag tagForCondition = null;
                if (part.endsWith("]") && i < path.size()) {
                    int index = part.indexOf("[");
                    if (index < 0) {
                        return false;
                    }
                    //if is Attribute
                    if (part.charAt(index + 1) == '@') {
                        partWithCondition = part;
                        tagForCondition = path.get(i);
                    }
                } else if (i < path.size() && i > 0 && parts[i - 1].endsWith("]")) {
                    String partBefore = parts[i - 1];
                    int index = partBefore.indexOf("[");
                    if (index < 0) {
                        return false;
                    }
                    if (!partBefore.contains("@")) {
                        partWithCondition = partBefore;
                        tagForCondition = path.get(path.size() - 1 - i);
                    }
                }

                String partName;

                Matcher matcher = partWithCondition != null ? PATTERN.matcher(partWithCondition) : null;
                if (tagForCondition != null && partWithCondition.endsWith("]") && matcher.matches()) {
                    String optionalPartName = matchesCondition(matcher, tagForCondition);
                    if (optionalPartName == null) {
                        return false;
                    }
                    partName = optionalPartName;
                } else {
                    partName = null;
                }

                if (part.startsWith("@")) {
                    if (!(cursor.getValue() instanceof Attribute &&
                            (((Attribute) cursor.getValue()).getKeyAsString().equals(part.substring(1))) ||
                            "*".equals(part.substring(1)))) {
                        return false;
                    }

                    pathIndex--;
                    continue;
                }

                boolean conditionNotFulfilled = (tagForCondition != null
                        && !part.equals(partName) && !tagForCondition.getName()
                        .equals(partName)) && !"*".equals(part);
                if (path.size() < i + 1 || (
                        !path.get(pathIndex).getName().equals(part) && conditionNotFulfilled)) {
                    return false;
                }
            }

            return startsWithSlash || path.size() - pathIndex <= 1;
        } else {
            Collections.reverse(path);

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

                Tag tag = i < path.size() ? path.get(i) : null;
                String partName;

                Matcher matcher = PATTERN.matcher(part);
                if (tag != null && part.endsWith("]") && matcher.matches()) {
                    String optionalPartName = matchesCondition(matcher, tag);
                    if (optionalPartName == null) {
                        return false;
                    }
                    partName = optionalPartName;
                } else {
                    partName = part;
                }

                if (part.startsWith("@")) {
                    return cursor.getValue() instanceof Attribute &&
                            (((Attribute) cursor.getValue()).getKeyAsString().equals(part.substring(1)) ||
                                    "*".equals(part.substring(1)));
                }

                if (path.size() < i + 1 || (tag != null && !tag.getName().equals(partName) && !"*".equals(part))) {
                    return false;
                }
            }

            return cursor.getValue() instanceof Tag && path.size() == parts.length;
        }
    }

    //nullable because Optional API is not available
    @Nullable
    private String matchesCondition(Matcher matcher, Xml.Tag tag) {
        String name = matcher.group(1);
        boolean isAttribute = Objects.equals(matcher.group(2), "@");
        String selector = matcher.group(3);
        String value = matcher.group(4);

        boolean matchCondition = isAttribute ? tag.getAttributes().stream().anyMatch(a ->
                a.getKeyAsString().equals(selector) && a.getValueAsString().equals(value)) :
                FindTags.find(tag, selector).stream().anyMatch(t ->
                        t.getValue().map(v -> v.equals(value)).orElse(false)
                );
        if (!matchCondition) {
            return null;
        }
        return name;
    }
}
