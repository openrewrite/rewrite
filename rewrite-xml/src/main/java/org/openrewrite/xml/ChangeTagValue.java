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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

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
                          " If literal $,\\ characters are needed in newValue, with regex true, then it should be escaped.",
            example = "user")
    String newValue;

    @Option(displayName = "Regex",
            description = "Default false. If true, `oldValue` will be interpreted as a [Regular Expression](https://en.wikipedia.org/wiki/Regular_expression), and capture group contents will be available in `newValue`.",
            required = false)
    @Nullable
    Boolean regex;

    @Override
    public String getDisplayName() {
        return "Change XML tag value";
    }

    @Override
    public String getDescription() {
        return "Alters the value of XML tags matching the provided expression. " +
               "When regex is enabled the replacement happens only for text nodes provided the pattern matches.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            private final XPathMatcher xPathMatcher = new XPathMatcher(elementName);

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (xPathMatcher.matches(getCursor())) {
                    // if either no oldValue is supplied OR oldValue equals the value of this tag
                    // then change the value to the newValue supplied
                    if (!Boolean.TRUE.equals(regex) &&
                            (oldValue == null || oldValue.equals(tag.getValue().orElse(null)))) {
                        doAfterVisit(new ChangeTagValueVisitor<>(tag, newValue));
                    } else if (Boolean.TRUE.equals(regex) && oldValue != null) {
                        doAfterVisit(new FindAndReplaceTagTextVisitor<>(tag, oldValue, newValue, Boolean.TRUE));
                    }
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
