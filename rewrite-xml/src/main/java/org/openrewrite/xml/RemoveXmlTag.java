/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveXmlTag extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove XML Tag";
    }

    @Override
    public String getDescription() {
        return "Removes XML tags matching the provided expression.";
    }

    @Option(displayName = "Element name",
            description = "The name of the element which is to be removed. Interpreted as an XPath Expression.",
            example = "/settings/servers/server/username")
    String elementName;

    @Option(displayName = "File matcher",
            description = "If provided only matching files will be modified. This is a glob expression.",
            required = false,
            example = "'**/application-*.xml'")
    @Nullable
    String fileMatcher;

    @Override
    public TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            private final XPathMatcher xPathMatcher = new XPathMatcher(elementName);

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (xPathMatcher.matches(getCursor())) {
                    doAfterVisit(new RemoveContentVisitor<>(tag, true));
                }
                return super.visitTag(tag, ctx);
            }
        };
    }

}
