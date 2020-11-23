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

import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.refactor.Formatter;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

/**
 * Adds a dependency if there is no dependency matching <code>groupId</code> and <code>artifactId</code>.
 * A matching dependency with a different version or scope does NOT have its version or scope updated.
 * Use {@link ChangeDependencyVersion} or {@link UpgradeDependencyVersion} in the case of a different version.
 * Use {@link ChangeDependencyScope} in the case of a different scope.
 * <p>
 * Places a new dependency as physically "near" to a group of similar dependencies as possible.
 */
public class AddDependency extends MavenRefactorVisitor {
    private String groupId;
    private String artifactId;

    @Nullable
    private String version;

    @Nullable
    private String scope;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(@Nullable String version) {
        this.version = version;
    }

    public void setScope(@Nullable String scope) {
        this.scope = scope;
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId))
                .and(required("version", version));
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public Maven visitMaven(Maven maven) {
        if (maven.getModel().getDependencies().stream()
                .anyMatch(d -> d.getGroupId().equals(groupId) && d.getArtifactId().equals(artifactId))) {
            return maven;
        }

        andThen(new AddDependenciesTagIfNotPresent());
        andThen(new InsertDependencyInOrder());

        return maven;
    }

    private static class AddDependenciesTagIfNotPresent extends MavenRefactorVisitor {
        @Override
        public Maven visitMaven(Maven maven) {
            Maven m = super.visitMaven(maven);

            Xml.Tag root = maven.getRoot();
            if (!root.getChild("dependencies").isPresent()) {
                MavenTagInsertionComparator insertionComparator = new MavenTagInsertionComparator(root.getChildren());
                List<Xml.Tag> content = new ArrayList<>(root.getChildren());

                Formatting fmt = format(formatter.findIndent(0, root.getChildren().toArray(new Tree[0])).getPrefix());
                content.add(
                        new Xml.Tag(
                                randomId(),
                                "dependencies",
                                emptyList(),
                                emptyList(),
                                new Xml.Tag.Closing(randomId(), "dependencies", "", fmt),
                                "",
                                fmt
                        )
                );

                content.sort(insertionComparator);

                return maven.withRoot(maven.getRoot().withContent(content));
            }

            return m;
        }
    }

    private class InsertDependencyInOrder extends MavenRefactorVisitor {
        private final XPathMatcher dependenciesMatcher = new XPathMatcher("/project/dependencies");

        public InsertDependencyInOrder() {
            setCursoringOn();
        }

        @Override
        public Xml visitTag(Xml.Tag tag) {
            if (dependenciesMatcher.matches(getCursor())) {
                Formatter.Result indent = formatter.findIndent(0, tag);

                // TODO if the dependency is manageable, make it managed
                Xml.Tag dependencyTag = new XmlParser().parse(
                        "<dependency>" +
                                indent.getPrefix(2) + "<groupId>" + groupId + "</groupId>" +
                                indent.getPrefix(2) + "<artifactId>" + artifactId + "</artifactId>" +
                                (version == null ? "" :
                                        indent.getPrefix(2) + "<version>" + version + "</version>") +
                                (scope == null ? "" :
                                        indent.getPrefix(2) + "<scope>" + scope + "</scope>") +
                                indent.getPrefix(1) + "</dependency>"
                ).get(0).getRoot().withFormatting(format(indent.getPrefix(1)));

                List<Xml.Tag> ideallySortedDependencies = new ArrayList<>(tag.getChildren());
                if (tag.getChildren().isEmpty()) {
                    ideallySortedDependencies.add(dependencyTag);
                } else {
                    // if everything were ideally sorted, which dependency would the addable dependency
                    // come after?
                    List<Xml.Tag> sortedDependencies = new ArrayList<>(tag.getChildren());
                    sortedDependencies.add(dependencyTag);

                    ideallySortedDependencies.sort(dependencyComparator);

                    int addAfterIndex = -1;
                    for (int i = 0; i < sortedDependencies.size(); i++) {
                        Xml.Tag d = sortedDependencies.get(i);
                        if (dependencyTag == d) {
                            addAfterIndex = i - 1;
                            break;
                        }
                    }

                    ideallySortedDependencies.add(addAfterIndex + 1, dependencyTag);
                }
                return tag.withContent(ideallySortedDependencies);
            }

            return super.visitTag(tag);
        }

        Comparator<Xml.Tag> dependencyComparator = (d1, d2) -> {
            String groupId1 = d1.getChildValue("groupId").orElse("");
            String groupId2 = d2.getChildValue("groupId").orElse("");
            if (!groupId1.equals(groupId2)) {
                return comparePartByPart(groupId1, groupId2);
            }

            String artifactId1 = d1.getChildValue("artifactId").orElse("");
            String artifactId2 = d2.getChildValue("artifactId").orElse("");
            if (!artifactId1.equals(artifactId2)) {
                return comparePartByPart(artifactId1, artifactId2);
            }

            String classifier1 = d1.getChildValue("classifier").orElse(null);
            String classifier2 = d2.getChildValue("classifier").orElse(null);

            if (classifier1 == null && classifier2 != null) {
                return -1;
            } else if (classifier1 != null) {
                if (classifier2 == null) {
                    return 1;
                }
                if (!classifier1.equals(classifier2)) {
                    return classifier1.compareTo(classifier2);
                }
            }

            // in every case imagined so far, group and artifact comparison are enough,
            // so this is just for completeness
            return d1.getChildValue("version").orElse("")
                    .compareTo(d2.getChildValue("version").orElse(""));
        };

        private int comparePartByPart(String d1, String d2) {
            String[] d1Parts = d1.split("[.-]");
            String[] d2Parts = d2.split("[.-]");

            for (int i = 0; i < Math.min(d1Parts.length, d2Parts.length); i++) {
                if (!d1Parts[i].equals(d2Parts[i])) {
                    return d1Parts[i].compareTo(d2Parts[i]);
                }
            }

            return d1Parts.length - d2Parts.length;
        }
    }
}
