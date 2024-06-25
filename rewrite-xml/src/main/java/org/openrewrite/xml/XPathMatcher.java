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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.tree.Namespaced;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern PATTERN = Pattern.compile("(@)?([-\\w]+|\\*)\\[((local-name|namespace-uri)\\(\\)|(@)?([-\\w]+|\\*))='(.*)']");

    private final String expression;
    private final boolean startsWithSlash;
    private final boolean startsWithDoubleSlash;
    private final String[] parts;

    public XPathMatcher(String expression) {
        this.expression = expression;
        startsWithSlash = expression.startsWith("/");
        startsWithDoubleSlash = expression.startsWith("//");
        parts = splitOnXPathSeparator(expression.substring(startsWithDoubleSlash ? 2 : startsWithSlash ? 1 : 0));
    }

    private String[] splitOnXPathSeparator(String input) {
        List<String> matches = new ArrayList<>();
        Pattern p = Pattern.compile("((?<=/)(?=/)|[^/\\[]|\\[[^]]*\\])+");
        Matcher m = p.matcher(input);
        while (m.find()) {
            matches.add(m.group());
        }
        return matches.toArray(new String[0]);
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

        if (startsWithDoubleSlash || !startsWithSlash) {
            int pathIndex = 0;
            for (int i = parts.length - 1; i >= 0; i--, pathIndex++) {
                String part = parts[i];

                String partWithCondition = null;
                Xml.Tag tagForCondition = null;
                boolean conditionIsBefore = false;
                if (part.endsWith("]") && i < path.size()) {
                    int index = part.indexOf("[");
                    if (index < 0) {
                        return false;
                    }
                    //if is Attribute
                    if (part.charAt(index + 1) == '@') {
                        partWithCondition = part;
                        tagForCondition = path.get(i);
                    } else if (part.contains("(") && part.contains(")")) { //if is function
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
                        conditionIsBefore = true;
                        partWithCondition = partBefore;
                        tagForCondition = path.get(parts.length - i);
                    }
                } else if (part.endsWith(")")) { // is xpath method
                    // TODO: implement other xpath methods
                }

                String partName;
                boolean matchedCondition = false;

                Matcher matcher;
                if (tagForCondition != null && partWithCondition.endsWith("]") && (matcher = PATTERN.matcher(
                        partWithCondition)).matches()) {
                    String optionalPartName = matchesElementWithConditionFunction(matcher, tagForCondition, cursor);
                    if (optionalPartName == null) {
                        return false;
                    }
                    partName = optionalPartName;
                    matchedCondition = true;
                } else {
                    partName = null;
                }

                if (part.startsWith("@")) {
                    if (!matchedCondition) {
                        if (!(cursor.getValue() instanceof Xml.Attribute &&
                              (((Xml.Attribute) cursor.getValue()).getKeyAsString().equals(part.substring(1))) ||
                              "*".equals(part.substring(1)))) {
                            return false;
                        }
                    }

                    pathIndex--;
                    continue;
                }

                boolean conditionNotFulfilled =
                        tagForCondition == null || (!part.equals(partName) && !tagForCondition.getName()
                                .equals(partName));

                int idx = part.indexOf("[");
                if (idx > 0) {
                    part = part.substring(0, idx);
                }
                if (path.size() < i + 1 || (
                        !(path.get(pathIndex).getName().equals(part)) && !"*".equals(part)) || conditionIsBefore && conditionNotFulfilled) {
                    return false;
                }
            }

            return startsWithSlash || path.size() - pathIndex <= 1;
        } else {
            Collections.reverse(path);

            // Deal with the two forward slashes in the expression; works, but I'm not proud of it.
            if (expression.contains("//") && !expression.contains("://") && Arrays.stream(parts).anyMatch(StringUtils::isBlank)) {
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
                boolean matchedCondition = false;

                Matcher matcher;
                if (tag != null && part.endsWith("]") && (matcher = PATTERN.matcher(part)).matches()) {
                    String optionalPartName = matchesElementWithConditionFunction(matcher, tag, cursor);
                    if (optionalPartName == null) {
                        return false;
                    }
                    partName = optionalPartName;
                    matchedCondition = true;
                } else {
                    partName = part;
                }

                if (part.startsWith("@")) {
                    if (matchedCondition) {
                        return true;
                    }
                    return cursor.getValue() instanceof Xml.Attribute &&
                           (((Xml.Attribute) cursor.getValue()).getKeyAsString().equals(part.substring(1)) ||
                            "*".equals(part.substring(1)));
                }

                if (path.size() < i + 1 || (tag != null && !tag.getName().equals(partName) && !partName.equals("*") && !"*".equals(part))) {
                    return false;
                }
            }

            return cursor.getValue() instanceof Xml.Tag && path.size() == parts.length;
        }
    }

    @Nullable
    private String matchesElementWithConditionFunction(Matcher matcher, Xml.Tag tag, Cursor cursor) {
        boolean isAttributeElement = matcher.group(1) != null;
        String element = matcher.group(2);
        boolean isAttributeCondition = matcher.group(5) != null; // either group4 != null, or group 2 startsWith @
        String selector = isAttributeCondition ? matcher.group(6) : matcher.group(3);
        boolean isFunctionCondition = selector.endsWith("()");
        String value = matcher.group(7);

        boolean matchCondition = false;
        if (isAttributeCondition) {
            for (Xml.Attribute a : tag.getAttributes()) {
                if ((a.getKeyAsString().equals(selector) || "*".equals(selector)) && a.getValueAsString().equals(value)) {
                    matchCondition = true;
                    break;
                }
            }
        } else if (isFunctionCondition) {
            if (isAttributeElement) {
                for (Xml.Attribute a : tag.getAttributes()) {
                    if (matchesElementAndFunction(a, cursor, element, selector, value)) {
                        matchCondition = true;
                        break;
                    }
                }
            } else {
                matchCondition = matchesElementAndFunction(tag, cursor, element, selector, value);
            }
        } else { // other [] conditions
            for (Xml.Tag t : FindTags.find(tag, selector)) {
                if (t.getValue().map(v -> v.equals(value)).orElse(false)) {
                    matchCondition = true;
                    break;
                }
            }
        }

        return matchCondition ? element : null;
    }

    private static boolean matchesElementAndFunction(Namespaced tagOrAttribute, Cursor cursor, String element, String selector, String value) {
        if (!element.equals("*") && !tagOrAttribute.getName().equals(element)) {
            return false;
        } else if (selector.equals("local-name()")) {
            return tagOrAttribute.getLocalName().equals(value);
        } else if (selector.equals("namespace-uri()")) {
            Optional<String> nsUri= tagOrAttribute.getNamespaceUri(cursor);
            return nsUri.isPresent() && nsUri.get().equals(value);
        }
        return false;
    }
}
