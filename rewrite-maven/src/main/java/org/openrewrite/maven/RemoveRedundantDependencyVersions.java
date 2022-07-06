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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveRedundantDependencyVersions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove redundant explicit dependency versions";
    }

    @Override
    public String getDescription() {
        return "Remove explicitly-specified dependency versions when a parent POM's dependencyManagement " +
                "specifies the same explicit version.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (!isManagedDependencyTag()) {
                    ResolvedDependency d = findDependency(tag);
                    if (d != null && matchesVersion(d) && matchesScope(d, tag)) {
                            Xml.Tag version = tag.getChild("version").orElse(null);
                            return tag.withContent(ListUtils.map(tag.getContent(), c -> c == version ? null : c));
                    }
                }
                return super.visitTag(tag, ctx);
            }

            private boolean matchesVersion(ResolvedDependency d) {
                return d.getRequested().getVersion() != null
                        && d.getRequested().getVersion().equals(getResolutionResult().getPom().getManagedVersion(d.getGroupId(), d.getArtifactId(),
                        d.getRequested().getType(), d.getRequested().getClassifier()));
            }

            private boolean matchesScope(ResolvedDependency d, Xml.Tag dependencyTag) {
                return Objects.equals(
                        Scope.fromName(dependencyTag.getChildValue("scope").orElse(null)),
                        getResolutionResult().getPom().getManagedScope(d.getGroupId(), d.getArtifactId(), d.getRequested().getType(),
                                d.getRequested().getClassifier()));
            }
        };
    }
}
