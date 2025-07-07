/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.maven.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

public class DependencyManagementDependencyRequiresVersion extends Recipe {

    @Override
    public String getDisplayName() {
        return "Dependency management dependencies should have a version";
    }

    @Override
    public String getDescription() {
        return "If they don't have a version, they can't possibly affect dependency resolution anywhere, and can be safely removed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isManagedDependencyTag() && tag.getChildValue("version").orElse(null) == null) {
                    doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
