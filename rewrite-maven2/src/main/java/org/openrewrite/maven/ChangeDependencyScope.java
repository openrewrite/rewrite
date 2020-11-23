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

import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.AddToTag;
import org.openrewrite.xml.ChangeTagValue;
import org.openrewrite.xml.RemoveContent;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.Validated.required;

public class ChangeDependencyScope extends MavenRefactorVisitor {
    private static final XPathMatcher dependencyMatcher = new XPathMatcher("/project/dependencies/dependency");

    private String groupId;
    private String artifactId;

    public ChangeDependencyScope() {
        setCursoringOn();
    }

    /**
     * If null, strips the scope from an existing dependency.
     */
    @Nullable
    private String toScope;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setToScope(@Nullable String toScope) {
        this.toScope = toScope;
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId));
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        if (dependencyMatcher.matches(getCursor())) {
            if (groupId.equals(tag.getChildValue("groupId").orElse(model.getGroupId())) &&
                    artifactId.equals(tag.getChildValue("artifactId").orElse(null))) {
                Optional<Xml.Tag> scope = tag.getChild("scope");
                if(scope.isPresent()) {
                    if(toScope == null) {
                        andThen(new RemoveContent.Scoped(scope.get()));
                    }
                    else if(!toScope.equals(scope.get().getValue().orElse(null))) {
                        andThen(new ChangeTagValue.Scoped(scope.get(), toScope));
                    }
                } else {
                    andThen(new AddToTag.Scoped(tag, "<scope>" + toScope + "</scope>"));
                }
            }
        }

        return super.visitTag(tag);
    }
}
