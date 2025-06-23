/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.maven.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.tree.Xml;

public class FindScm extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find SCM tag";
    }

    @Override
    public String getDescription() {
        return "Finds any `<scm>` tag directly inside the `<project>` root of a Maven pom.xml file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if ("project".equals(tag.getName())) {
                    return super.visitTag(tag, ctx);
                } else if ("scm".equals(tag.getName())) {
                    return SearchResult.found(tag);
                }
                return tag;
            }
        };
    }
}
