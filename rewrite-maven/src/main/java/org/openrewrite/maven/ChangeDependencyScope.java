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
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeDependencyScope extends Recipe {

    @Option(displayName = "Group ID")
    String groupId;

    @Option(displayName = "Artifact ID")
    String artifactId;

    /**
     * If null, strips the scope from an existing dependency.
     */
    @Option(displayName = "New scope", description = "Scope to apply to specified Maven dependency. " +
            "May be null, which indicates that no scope should be added and any existing scope be removed from the dependency.")
    @Nullable
    String newScope;

    @Override
    public String getDisplayName() {
        return "Change Maven dependency scope";
    }

    @Override
    public String getDescription() {
        return "Add or alter the scope of the specified dependency";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeDependencyScopeVisitor();
    }

    private class ChangeDependencyScopeVisitor extends MavenVisitor {

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isDependencyTag()) {
                if (groupId.equals(tag.getChildValue("groupId").orElse(model.getGroupId())) &&
                        artifactId.equals(tag.getChildValue("artifactId").orElse(null))) {
                    Optional<Xml.Tag> scope = tag.getChild("scope");
                    if (scope.isPresent()) {
                        if (newScope == null) {
                            doAfterVisit(new RemoveContentVisitor<>(scope.get(), false));
                        } else if (!newScope.equals(scope.get().getValue().orElse(null))) {
                            doAfterVisit(new ChangeTagValueVisitor<>(scope.get(), newScope));
                        }
                    } else {
                        doAfterVisit(new AddToTagVisitor<>(tag, Xml.Tag.build("<scope>" + newScope + "</scope>")));
                    }
                }
            }

            return super.visitTag(tag, ctx);
        }
    }
}
