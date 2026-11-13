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
package org.openrewrite.maven;

import lombok.Getter;
import org.apache.commons.text.StringEscapeUtils;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

public class RemoveInvalidParentRelativePath extends Recipe {

    private static final XPathMatcher PARENT_RELATIVE_PATH_MATCHER = new XPathMatcher("/project/parent/relativePath");

    private static final String ILLEGAL_RELATIVE_PATH_CHARS = ":\"<>|?*";

    @Getter
    final String displayName = "Remove invalid parent `relativePath`";

    @Getter
    final String description = "Maven 3.10 and 4 fail the build when a parent `<relativePath>` contains any of the " +
        "characters `: \" < > | ? *`, which typically means a `groupId:artifactId:version` was entered instead of a " +
        "path. Earlier Maven versions found no POM at such a path and resolved the parent from the repository. " +
        "This recipe replaces the value with an empty `<relativePath/>`, which tells Maven to do exactly that.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (PARENT_RELATIVE_PATH_MATCHER.matches(getCursor()) &&
                    t.getValue().map(StringEscapeUtils::unescapeXml).filter(this::isInvalid).isPresent()) {
                    return Xml.Tag.build("<relativePath/>").withPrefix(t.getPrefix());
                }
                return t;
            }

            private boolean isInvalid(String relativePath) {
                return relativePath.chars().anyMatch(c -> ILLEGAL_RELATIVE_PATH_CHARS.indexOf(c) >= 0);
            }
        };
    }
}
