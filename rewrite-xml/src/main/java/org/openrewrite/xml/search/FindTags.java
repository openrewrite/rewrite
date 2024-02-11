/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindTags extends Recipe {
    @Option(displayName = "XPath",
            description = "An XPath expression used to find matching tags.",
            example = "/dependencies/dependency")
    String xPath;

    @Override
    public String getDisplayName() {
        return "Find XML tags";
    }

    @Override
    public String getDescription() {
        return "Find XML tags by XPath expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher matcher = new XPathMatcher(xPath);
        return new XmlVisitor<ExecutionContext>() {

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (matcher.matches(getCursor())) {
                    t = SearchResult.found(t);
                }
                return t;
            }
        };
    }

    public static Set<Xml.Tag> find(Xml x, String xPath) {
        XPathMatcher matcher = new XPathMatcher(xPath);
        Set<Xml.Tag> ts = new HashSet<>();
        new XmlVisitor<Set<Xml.Tag>>() {
            @Override
            public Xml visitTag(Xml.Tag tag, Set<Xml.Tag> ts) {
                if (matcher.matches(getCursor())) {
                    ts.add(tag);
                }
                return super.visitTag(tag, ts);
            }
        }.visit(x, ts);
        return ts;
    }

    /**
     * Returns <code>null</code> if there is not exactly one tag matching this xPath
     */
    @Nullable
    @Incubating(since = "7.33.0")
    public static Xml.Tag findSingle(Xml x, String xPath) {
        final Set<Xml.Tag> tags = find(x, xPath);
        if (tags.size() != 1) {
            return null;
        }
        for (final Xml.Tag tag : tags) {
            return tag;
        }
        return null;
    }
}
