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
import org.openrewrite.*;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeTagAttributeKey extends Recipe {

    String displayName = "Change XML attribute key";
    String description = "Change an attributes key on XML elements using an XPath expression.";

    @Option(displayName = "Attribute XPath",
            description = "XPath expression to match the attribute.",
            example = "//a4j:ajax/@reRender")
    String elementXPath;

    @Option(displayName = "New attribute name",
            description = "The new name for the attribute.",
            example = "render")
    String newAttributeName;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher xPathMatcher = new XPathMatcher(elementXPath);
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Attribute visitAttribute(Xml.Attribute attribute, ExecutionContext executionContext) {
                Xml.Attribute a = super.visitAttribute(attribute, executionContext);

                if (!xPathMatcher.matches(getCursor())) {
                    return a;
                }

                return a.withKey(a.getKey().withName(newAttributeName));
            }
        };
    }
}
