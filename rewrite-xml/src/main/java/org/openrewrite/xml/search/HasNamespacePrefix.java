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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class HasNamespacePrefix extends Recipe {

    @Option(displayName = "Namespace Prefix",
            description = "The Namespace Prefix to find.",
            example = "http://www.w3.org/2001/XMLSchema-instance")
    String namespacePrefix;

    @Option(displayName = "XPath",
            description = "An XPath expression used to find namespace URIs.",
            example = "/dependencies/dependency",
            required = false)
    @Nullable
    String xPath;

    @Override
    public String getDisplayName() {
        return "Find XML namespace prefixes";
    }

    @Override
    public String getDescription() {
        return "Find XML namespace prefixes, optionally restricting the search by a XPath expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher matcher = StringUtils.isBlank(xPath) ? null : new XPathMatcher(xPath);
        return new XmlVisitor<ExecutionContext>() {

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (tag.getNamespaces().containsPrefix(namespacePrefix) && (matcher == null || matcher.matches(getCursor()))) {
                    t = SearchResult.found(t);
                }
                return t;
            }
        };
    }

    public static Set<Xml.Tag> find(Xml x, String namespacePrefix, @Nullable String xPath) {
        XPathMatcher matcher = StringUtils.isBlank(xPath) ? null : new XPathMatcher(xPath);
        Set<Xml.Tag> ts = new HashSet<>();
        new XmlVisitor<Set<Xml.Tag>>() {
            @Override
            public Xml visitTag(Xml.Tag tag, Set<Xml.Tag> ts) {
                if (tag.getNamespaces().containsPrefix(namespacePrefix) && (matcher == null || matcher.matches(getCursor()))) {
                    ts.add(tag);
                }
                return super.visitTag(tag, ts);
            }
        }.visit(x, ts);
        return ts;
    }
}
