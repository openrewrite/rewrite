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

public class ReplaceModulesWithSubprojects extends Recipe {

    private static final XPathMatcher MODULES_MATCHER = new XPathMatcher("/project/modules");
    private static final XPathMatcher MODULE_MATCHER = new XPathMatcher("/project/modules/module");

    @Override
    public String getDisplayName() {
        return "Replace modules with subprojects";
    }

    @Override
    public String getDescription() {
        return "Maven 4 model version 4.1.0 deprecates the `<modules>` element in favor of `<subprojects>` " +
               "to eliminate confusion with Java's Platform Module System (JPMS). " +
               "This recipe renames `<modules>` to `<subprojects>` and `<module>` children to `<subproject>`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/pom.xml"), new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (MODULES_MATCHER.matches(getCursor())) {
                    t = t.withName("subprojects");
                } else if (MODULE_MATCHER.matches(getCursor())) {
                    t = t.withName("subproject");
                }

                return t;
            }
        });
    }
}
