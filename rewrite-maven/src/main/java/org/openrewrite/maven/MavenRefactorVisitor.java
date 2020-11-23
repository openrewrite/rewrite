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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlRefactorVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;

public class MavenRefactorVisitor extends XmlRefactorVisitor
        implements MavenSourceVisitor<Xml> {
    private static final XPathMatcher DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencies/dependency");
    private static final XPathMatcher MANAGED_DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies/dependency");
    private static final XPathMatcher PROPERTY_MATCHER = new XPathMatcher("/project/properties/*");
    private static final XPathMatcher PARENT_MATCHER = new XPathMatcher("/project/parent");

    protected Pom model;
    protected Collection<Pom> modules;

    @Override
    public Maven visitMaven(Maven maven) {
        this.model = maven.getModel();
        this.modules = maven.getModules();
        return (Maven) visitDocument(maven);
    }

    @Override
    public final Xml visitDocument(Xml.Document document) {
        Xml.Document refactored = refactor(document, super::visitDocument);
        if (refactored != document) {
            return new Maven(refactored);
        }
        return refactored;
    }

    protected boolean isPropertyTag() {
        return PROPERTY_MATCHER.matches(getCursor());
    }

    protected boolean isDependencyTag() {
        return DEPENDENCY_MATCHER.matches(getCursor());
    }

    protected boolean isDependencyTag(String groupId, @Nullable String artifactId) {
        return isDependencyTag() && hasGroupAndArtifact(groupId, artifactId);
    }

    protected boolean isManagedDependencyTag() {
        return MANAGED_DEPENDENCY_MATCHER.matches(getCursor());
    }

    protected boolean isManagedDependencyTag(String groupId, @Nullable String artifactId) {
        return isManagedDependencyTag() && hasGroupAndArtifact(groupId, artifactId);
    }

    protected boolean isParentTag() {
        return PARENT_MATCHER.matches(getCursor());
    }

    private boolean hasGroupAndArtifact(String groupId, @Nullable String artifactId) {
        Xml.Tag tag = getCursor().getTree();
        return groupId.equals(tag.getChildValue("groupId").orElse(model.getGroupId())) &&
                tag.getChildValue("artifactId")
                        .map(a -> a.equals(artifactId))
                        .orElse(artifactId == null);
    }
}
