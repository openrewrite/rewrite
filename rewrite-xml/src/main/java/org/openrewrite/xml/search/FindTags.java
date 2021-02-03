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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class FindTags extends Recipe {
    private final String xPath;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindTagsVisitor(new XPathMatcher(xPath));
    }

    public static Set<Xml.Tag> find(Xml x, String xPath) {
        //noinspection ConstantConditions
        return new FindTagsVisitor(new XPathMatcher(xPath)).visit(x, ExecutionContext.builder().build())
                .findMarkedWith(SearchResult.class);
    }

    private static class FindTagsVisitor extends XmlVisitor<ExecutionContext> {

        private final XPathMatcher xPathMatcher;

        public FindTagsVisitor(XPathMatcher xPathMatcher) {
            this.xPathMatcher = xPathMatcher;
            setCursoringOn();
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (xPathMatcher.matches(getCursor())) {
                return tag.withMarker(new SearchResult(null));
            }
            return super.visitTag(tag, ctx);
        }
    }
}
