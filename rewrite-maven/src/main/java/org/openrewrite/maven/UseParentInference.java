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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

public class UseParentInference extends Recipe {

    private static final XPathMatcher PARENT_MATCHER = new XPathMatcher("/project/parent");

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
        return Preconditions.check(new FindSourceFiles("**/pom.xml"), new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (PARENT_MATCHER.matches(getCursor())) {
                    // Only process if relativePath is explicitly ".." or not present (which defaults to "..")
                    java.util.Optional<Xml.Tag> relativePathTag = t.getChild("relativePath");
                    String relativePath = relativePathTag.map(rp -> rp.getValue().orElse("")).orElse(null);

                    // Check if relativePath is explicitly ".." or not present
                    // Empty string ("") means <relativePath/> which explicitly disables filesystem lookup - skip this
                    if (relativePath != null && !relativePath.isEmpty() && !"..".equals(relativePath)) {
                        // Non-default relativePath (like "../../parent"), don't change
                        return t;
                    }

                    if (relativePath == null || "..".equals(relativePath)) {
                        // Check if the parent has groupId, artifactId, and version
                        boolean hasGroupId = t.getChild("groupId").isPresent();
                        boolean hasArtifactId = t.getChild("artifactId").isPresent();
                        boolean hasVersion = t.getChild("version").isPresent();

                        // Only apply if all coordinates are present (otherwise it's already using inference)
                        if (hasGroupId && hasArtifactId && hasVersion) {
                            // Replace with parent tag containing only relativePath
                            t = autoFormat(Xml.Tag.build("<parent><relativePath/></parent>"), ctx, getCursor().getParentOrThrow());
                        }
                    }
                }

                return t;
            }
        });
    }
}
