/*
 * Copyright 2026 the original author or authors.
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

import static org.openrewrite.internal.ListUtils.map;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTagAttributeKey extends Recipe {

    String displayName = "Change XML attribute key";
    String description = "Change an attributes key on XML elements matching the given XPath expression.";

    @Option(displayName = "Element XPath",
            description = "XPath expression to match elements.",
            example = "//a4j:ajax")
    String elementXPath;

    @Option(displayName = "Old attribute name",
            description = "The current name of the attribute to rename.",
            example = "reRender")
    String oldAttributeName;

    @Option(displayName = "New attribute name",
            description = "The new name for the attribute.",
            example = "render")
    String newAttributeName;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher xPathMatcher = new XPathMatcher(elementXPath);
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!xPathMatcher.matches(getCursor())) {
                    return t;
                }

                return t.withAttributes(map(t.getAttributes(),
                        attr -> oldAttributeName.equals(attr.getKeyAsString()) ?
                                attr.withKey(attr.getKey().withName(newAttributeName)) : attr));
            }
        };
    }
}
