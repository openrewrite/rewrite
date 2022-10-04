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
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

public class IsOwaspSuppressionsFile extends XmlIsoVisitor<ExecutionContext> {

    private static final String MATCHER = "https://jeremylong.github.io/DependencyCheck/dependency-suppression(.*?).xsd";

    @Override
    public void visit(@Nullable List<? extends Xml> nodes, ExecutionContext executionContext) {
        super.visit(nodes, executionContext);
    }

    @Override
    public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
        Xml.Document doc = super.visitDocument(document, executionContext);
        if (doc.getRoot() == null) {
            return doc;
        }
        Xml.Tag root = doc.getRoot();
        // root must be suppressions
        if (!root.getName().equals("suppressions")) {
            return doc;
        }
        // check that root xmlns matches
        boolean isOwaspSuppressionFile = false;
        for (Xml.Attribute attribute : root.getAttributes()) {
            if (attribute.getKeyAsString().equals("xmlns")) {
                if (attribute.getValueAsString().matches(MATCHER)) {
                    isOwaspSuppressionFile = true;
                }
            }
        }
        if (isOwaspSuppressionFile) {
            return doc.withRoot(doc.getRoot().withMarkers(doc.getRoot().getMarkers().addIfAbsent(new FoundSuppressionMarker("Found it"))));
        }
        return doc;
    }

    @SuppressWarnings("ConstantConditions")
    private static class FoundSuppressionMarker extends SearchResult {

        public FoundSuppressionMarker(String description) {
            super(Tree.randomId(), description);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FoundSuppressionMarker)) {
                return false;
            }
            return getDescription().equals(((FoundSuppressionMarker) obj).getDescription());
        }
    }
}
