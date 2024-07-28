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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTagValue extends Recipe {

    @Option(displayName = "Element name",
            description = "The name of the element whose value is to be changed. Interpreted as an XPath Expression.",
            example = "/settings/servers/server/username")
    String elementName;

    @Option(displayName = "Old value",
            description = "The old value of the tag.",
            required = false,
            example = "user")
    @Nullable
    String oldValue;

    @Option(displayName = "New value",
            description = "The new value for the tag.",
            example = "user")
    String newValue;

    @Override
    public String getDisplayName() {
        return "Change XML tag value";
    }

    @Override
    public String getDescription() {
        return "Alters the value of XML tags matching the provided expression.";
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
                    if (oldValue == null || oldValue.equals(tag.getValue().orElse(null))) {
                        doAfterVisit(new ChangeTagValueVisitor<>(tag, newValue));
                    }
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
