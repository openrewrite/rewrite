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
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MavenVisitor<P> extends XmlVisitor<P> {
    private static final XPathMatcher DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencies/dependency");
    private static final XPathMatcher MANAGED_DEPENDENCY_MATCHER = new XPathMatcher("/project/dependencyManagement/dependencies/dependency");
    private static final XPathMatcher PROPERTY_MATCHER = new XPathMatcher("/project/properties/*");
    private static final XPathMatcher PARENT_MATCHER = new XPathMatcher("/project/parent");

    protected Pom model;
    protected Collection<Pom> modules;
    protected MavenSettings settings;

    public Maven visitMaven(Maven maven, P p) {
        this.model = maven.getModel();
        this.modules = maven.getModules();
        this.settings = maven.getSettings();
        return (Maven) visitDocument(maven, p);
    }

    @Override
    public final Xml visitDocument(Xml.Document document, P p) {
        Xml.Document refactored = (Xml.Document) super.visitDocument(document, p);
        if (refactored != document) {
            return new Maven(refactored, settings);
        }
        return refactored;
    }

    public boolean isPropertyTag() {
        return PROPERTY_MATCHER.matches(getCursor());
    }

    public boolean isDependencyTag() {
        return DEPENDENCY_MATCHER.matches(getCursor());
    }

    public boolean isDependencyTag(String groupId, @Nullable String artifactId) {
        return isDependencyTag() && hasGroupAndArtifact(groupId, artifactId);
    }

    public boolean isManagedDependencyTag() {
        return MANAGED_DEPENDENCY_MATCHER.matches(getCursor());
    }

    public boolean isManagedDependencyTag(String groupId, @Nullable String artifactId) {
        return isManagedDependencyTag() && hasGroupAndArtifact(groupId, artifactId);
    }

    public boolean isParentTag() {
        return PARENT_MATCHER.matches(getCursor());
    }

    private boolean hasGroupAndArtifact(String groupId, @Nullable String artifactId) {
        Xml.Tag tag = getCursor().getValue();
        return groupId.equals(tag.getChildValue("groupId").orElse(model.getGroupId())) &&
                tag.getChildValue("artifactId")
                        .map(a -> a.equals(artifactId))
                        .orElse(artifactId == null);
    }

    @Nullable
    public Pom.Dependency findDependency(Xml.Tag tag) {
        return model.getDependencies().stream()
                .filter(d -> tag.getChildValue("groupId").orElse(model.getGroupId()).equals(d.getGroupId()) &&
                        tag.getChildValue("artifactId").orElse(model.getArtifactId()).equals(d.getArtifactId()))
                .findAny()
                .orElse(null);
    }

    /**
     * Finds dependencies in the model that match the provided group and artifact ids.
     *
     * @param groupId    The groupId to match
     * @param artifactId The artifactId to match.
     * @return dependencies (including transitive dependencies) with any version matching the provided group and artifact id, if any.
     */
    public Collection<Pom.Dependency> findDependencies(String groupId, String artifactId) {
        return findDependencies(d -> d.getGroupId().equals(groupId) && d.getArtifactId().equals(artifactId));
    }

    /**
     * Finds dependencies in the model that match the given predicate.
     *
     * @param matcher A dependency test
     * @return dependencies (including transitive dependencies) with any version matching the given predicate.
     */
    public Collection<Pom.Dependency> findDependencies(Predicate<Pom.Dependency> matcher) {
        return Stream.concat(
                model.getDependencies().stream().filter(matcher),
                model.getDependencies().stream()
                        .flatMap(d -> d.findDependencies(matcher).stream())
        ).collect(toList());
    }

    public void maybeAddDependency(String groupId, String artifactId, String version,
                                   @Nullable String classifier, @Nullable String scope) {
        AddDependencyVisitor<P> op = new AddDependencyVisitor<>(
                groupId,
                artifactId,
                version,
                null,
                true,
                classifier,
                scope,
                true,
                null);

        if (!getAfterVisit().contains(op)) {
            doAfterVisit(op);
        }
    }
}
