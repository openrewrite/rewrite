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
package org.openrewrite.xml.security;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

public class IsOwaspSuppressionsFile extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find OWASP vulnerability suppression XML files";
    }

    @Override
    public String getDescription() {
        return "These files are used to suppress false positives in OWASP [Dependency Check](https://jeremylong.github.io/DependencyCheck).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document doc = super.visitDocument(document, ctx);
                if (doc.getRoot() == null) {
                    return doc;
                }
                Xml.Tag root = doc.getRoot();

                if (!root.getName().equals("suppressions")) {
                    return doc;
                }

                // check that root xmlns matches
                boolean isOwaspSuppressionFile = false;
                for (Xml.Attribute attribute : root.getAttributes()) {
                    if (attribute.getKeyAsString().equals("xmlns")) {
                        if (attribute.getValueAsString().matches("https://jeremylong.github.io/DependencyCheck/dependency-suppression(.*?).xsd")) {
                            isOwaspSuppressionFile = true;
                        }
                    }
                }
                if (isOwaspSuppressionFile) {
                    return doc.withRoot(SearchResult.found(doc.getRoot()));
                }
                return doc;
            }
        };
    }
}
