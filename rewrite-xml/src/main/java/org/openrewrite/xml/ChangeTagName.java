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
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTagName extends Recipe {
    @Option(displayName = "Element name",
            description = "The name of the element whose attribute's value is to be changed. Interpreted as an XPath expression.",
            example = "/settings/servers/server/username")
    String elementName;

    @Option(displayName = "New name",
            description = "The new name for the tag.",
            example = "user")
    String newName;

    @Override
    public String getDisplayName() {
        return "Change XML tag name";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", elementName, newName);
    }

    @Override
    public String getDescription() {
        return "Alters the name of XML tags matching the provided expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            private final XPathMatcher xPathMatcher = new XPathMatcher(elementName);

            @Override
            public Xml.Tag visitTag(final Xml.Tag tag, final ExecutionContext ctx) {
                if (xPathMatcher.matches(getCursor())) {
                    return tag.withName(newName);
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
