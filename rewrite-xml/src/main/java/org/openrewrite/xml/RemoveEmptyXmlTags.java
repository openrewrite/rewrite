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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

public class RemoveEmptyXmlTags extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove empty XML Tag";
    }

    @Override
    public String getDescription() {
        return "Removes XML tags that do not have attributes or children, including self closing tags.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                //noinspection ConstantValue
                if (t != null && (t.getContent() == null || t.getContent().isEmpty()) && t.getAttributes().isEmpty()) {
                    doAfterVisit(new RemoveContentVisitor<>(t, true));
                }
                return t;
            }
        };
    }
}
