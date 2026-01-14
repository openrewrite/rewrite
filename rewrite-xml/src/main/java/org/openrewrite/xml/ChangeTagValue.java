/*
 * Copyright 2022 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Xml;

import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTagValue extends Recipe {

    @Option(displayName = "Element name",
            description = "The name of the element whose value is to be changed. Interpreted as an XPath Expression.",
            example = "/settings/servers/server/username")
    String elementName;

    @Option(displayName = "Old value",
            description = "The old value of the tag. Interpreted as pattern if regex is enabled.",
            required = false,
            example = "user")
    @Nullable
    String oldValue;

    @Option(displayName = "New value",
            description = "The new value for the tag. Supports capture groups when regex is enabled. " +
                    "If literal $,\\ characters are needed in newValue, with regex true, then it should be escaped.",
            example = "user")
    String newValue;

    @Option(displayName = "Regex",
            description = "Default false. If true, `oldValue` will be interpreted as a [Regular Expression](https://en.wikipedia.org/wiki/Regular_expression), and capture group contents will be available in `newValue`.",
            required = false)
    @Nullable
    Boolean regex;

    String displayName = "Change XML tag value";

    String description = "Alters the value of XML tags matching the provided expression. " +
                "When regex is enabled the replacement happens only for text nodes provided the pattern matches.";

    @Override
    public Validated<Object> validate(ExecutionContext ctx) {
        //noinspection ConstantValue
        return super.validate(ctx).and(Validated.test("regex", "Regex usage requires an `oldValue`", regex,
                value -> value == null || oldValue != null && !oldValue.equals(newValue)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            private final XPathMatcher xPathMatcher = new XPathMatcher(elementName);

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (xPathMatcher.matches(getCursor())) {
                    if (Boolean.TRUE.equals(regex) && oldValue != null) {
                        doAfterVisit(new RegexReplaceVisitor<>(tag, oldValue, newValue));
                    } else if (oldValue == null || oldValue.equals(tag.getValue().orElse(null))) {
                        doAfterVisit(new ChangeTagValueVisitor<>(tag, newValue));
                    }
                }
                return super.visitTag(tag, ctx);
            }
        };
    }

    /**
     * This visitor finds and replaces text within a specified XML tag.
     * It supports both literal and regular expression replacements.
     * Use {@link ChangeTagValueVisitor} if you only wish change the value, irrespective of current data.
     *
     * @param <P>
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    static class RegexReplaceVisitor<P> extends XmlVisitor<P> {

        Xml.Tag scope;
        String oldValue;
        String newValue;

        @Override
        public Xml visitTag(Xml.Tag tag, P p) {
            Xml.Tag t = (Xml.Tag) super.visitTag(tag, p);
            if (scope.isScope(t) &&
                    t.getContent() != null &&
                    t.getContent().size() == 1 &&
                    t.getContent().get(0) instanceof Xml.CharData) {
                return updateUsingRegex(t, (Xml.CharData) t.getContent().get(0));
            }
            return t;
        }

        private Xml.Tag updateUsingRegex(Xml.Tag t, Xml.CharData content) {
            String text = content.getText();
            if (Pattern.compile(oldValue).matcher(text).find()) {
                Markers oldMarkers = content.getMarkers();
                if (oldMarkers
                        .findAll(AlreadyReplaced.class)
                        .stream()
                        .noneMatch(m -> oldValue.equals(m.getFind()) && newValue.equals(m.getReplace()))) {
                    return t.withContent(singletonList(content
                            .withText(text.replaceAll(oldValue, newValue))
                            .withMarkers(oldMarkers.add(new AlreadyReplaced(randomId(), oldValue, newValue)))));
                }
            }
            return t;
        }
    }
}
