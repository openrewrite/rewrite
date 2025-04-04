/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.xml.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.trait.Namespaced;
import org.openrewrite.xml.tree.Xml;

import java.util.LinkedHashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class HasNamespaceUri extends Recipe {

    @Option(displayName = "Namespace URI",
            description = "The Namespace URI to find.",
            example = "http://www.w3.org/2001/XMLSchema-instance")
    String namespaceUri;

    @Option(displayName = "XPath",
            description = "An XPath expression used to find namespace URIs.",
            example = "/dependencies/dependency",
            required = false)
    @Nullable
    String xPath;

    @Override
    public String getDisplayName() {
        return "Find XML namespace URIs";
    }

    @Override
    public String getDescription() {
        return "Find XML namespace URIs, optionally restricting the search by a XPath expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Namespaced.matcher()
                .xPath(xPath)
                .uri(namespaceUri)
                .asVisitor(n -> n.getTree() instanceof Xml.Tag ?
                        SearchResult.found(n.getTree()) :
                        n.getTree());
    }

    public static Set<Xml.Tag> find(Xml x, String namespaceUri, @Nullable String xPath) {
        Set<Xml.Tag> ts = new LinkedHashSet<>();
        Namespaced.matcher()
                .xPath(xPath)
                .uri(namespaceUri)
                .asVisitor(n ->
                {
                    if (n.getTree() instanceof Xml.Tag) {
                        ts.add((Xml.Tag) n.getTree());
                    }
                    return n.getTree();
                }).visit(x, 0);
        return ts;
    }
}
