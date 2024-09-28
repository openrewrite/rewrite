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
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddOrUpdateChildTag extends Recipe {

    @Option(displayName = "Parent XPath",
            description = "XPath identifying the parent to which a child tag must be added",
            example = "/project//plugin//configuration")
    @Language("xpath")
    String parentXPath;

    @Option(displayName = "New child tag",
            description = "The XML of the new child to add or update on the parent tag.",
            example = "<skip>true</skip>")
    @Language("xml")
    String newChildTag;

    @Override
    public String getDisplayName() {
        return "Add or update child tag";
    }

    @Override
    public String getDescription() {
        return "Adds or updates a child element below the parent(s) matching the provided `parentXPath` expression. " +
               "If a child with the same name exists, it will be replaced, otherwise a new child will be added. " +
               "This ensures idempotent behaviour.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate()
                .and(Validated.notBlank("parentXPath", parentXPath))
                .and(Validated.notBlank("newChildTag", newChildTag));
        try {
            Xml.Tag.build(newChildTag);
        } catch (Exception e) {
            validated = validated.and(Validated.invalid("newChildTag", newChildTag, "Invalid XML for child tag", e));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            private final XPathMatcher xPathMatcher = new XPathMatcher(parentXPath);

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (xPathMatcher.matches(getCursor())) {
                    Xml.Tag newChild = Xml.Tag.build(newChildTag);
                    return AddOrUpdateChild.addOrUpdateChild(tag, newChild, getCursor().getParentOrThrow());
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
