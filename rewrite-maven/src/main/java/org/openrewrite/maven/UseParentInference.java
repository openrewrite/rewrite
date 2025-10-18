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
package org.openrewrite.maven;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

public class UseParentInference extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use Maven 4 parent inference";
    }

    @Override
    public String getDescription() {
        return "Maven 4.1.0 supports automatic parent version inference when using a relative path. " +
                "This recipe simplifies parent declarations by using the shorthand `<parent/>` form " +
                "when the parent is in the default location (`..`), removing the explicit `<relativePath>`, " +
                "`<groupId>`, `<artifactId>`, and `<version>` elements. Maven automatically infers these " +
                "values from the parent POM.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isParentTag() && tag.getContent() != null && !tag.getContent().isEmpty()) {
                    // Only process if relativePath is explicitly ".." or not present (which defaults to "..")
                    Optional<Xml.Tag> relativePathTag = t.getChild("relativePath");
                    String relativePath = relativePathTag.map(rp -> rp.getValue().orElse("")).orElse(null);
                    if ("..".equals(relativePath) || "../".equals(relativePath)) {
                        return Xml.Tag.build("<parent/>").withPrefix(tag.getPrefix());
                    }
                }

                return t;
            }
        };
    }
}
