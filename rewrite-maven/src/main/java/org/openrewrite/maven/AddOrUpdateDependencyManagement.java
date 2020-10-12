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
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;
import org.openrewrite.refactor.Formatter;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

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
public class AddOrUpdateDependencyManagement extends MavenRefactorVisitor {
    private String groupId;
    private String artifactId;
    private String description;

    public String getDescription() {
        return description;
    }

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
                .and(required("artifactId", artifactId));
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public Maven visitPom(Maven.Pom pom) {
        if (dependencyManagementSectionExists(pom) && managedDependencyExists(pom)) {
            return pom;
        }
        andThen(new AddDependenciesTagIfNotPresent());
        andThen(new AddOrUpdateDependency());
        return pom;
    }

    private boolean managedDependencyExists(Maven.Pom pom) {
        if (pom.getDependencyManagement() != null) {
            return pom.getDependencyManagement().getDependencies().stream()
                    .anyMatch(this::sameDependencyExists);
        }
        return false;
    }

    private boolean dependencyManagementSectionExists(Maven.Pom pom) {
        return pom.getModel().getDependencyManagement() != null;
    }

    private boolean sameDependencyExists(Maven.Dependency d) {
        return artifactId.equals(d.getArtifactId()) &&
                groupId.equals(d.getGroupId()) &&
                version.equals(d.getVersion()) &&
                scope.equals(d.getScope());
        // TODO: check for exclusions
    }

    private Xml.Tag createDependencyTag(Formatter.Result indent) {
        return new XmlParser().parse(
                "<dependency>" +
                        indent.getPrefix(3) + "<groupId>" + groupId + "</groupId>" +
                        indent.getPrefix(3) + "<artifactId>" + artifactId + "</artifactId>" +
                        (version == null ? "" :
                                indent.getPrefix(3) + "<version>" + version + "</version>") +
                        (scope == null ? "" :
                                indent.getPrefix(3) + "<scope>" + scope + "</scope>") +
                        indent.getPrefix(2) + "</dependency>"
        ).get(0).getRoot().withFormatting(format(indent.getPrefix(2)));
    }

    private static class AddDependenciesTagIfNotPresent extends MavenRefactorVisitor {
        @Override
        public Maven visitPom(Maven.Pom pom) {
            Maven.Pom p = refactor(pom, super::visitPom);

            Formatter.Result indentation = formatter.wholeSourceIndent();
            Formatting fmt1 = Formatting.format(indentation.getPrefix(0));
            Formatting fmt2 = Formatting.format(indentation.getPrefix(1));
            // Add a <dependencyManagement> if one does not already exist
            if (p.getDependencyManagement() == null) {
                p = p.withDependencyManagement(new Maven.DependencyManagement(
                        new org.openrewrite.maven.tree.MavenModel.DependencyManagement(new ArrayList<>()),
                        new Xml.Tag(
                                randomId(),
                                "dependencyManagement",
                                emptyList(),
                                emptyList(),
                                new Xml.Tag.Closing(randomId(), "dependencyManagement", "", fmt1),
                                "",
                                fmt1
                        )
                ));
            }
            // Add a <dependencies> tag beneath <dependencyManagement> if one does not already exist
            boolean hasDependenciesTag = p.getDependencyManagement()
                    .getTag()
                    .getChildren()
                    .stream()
                    .anyMatch(it -> it.getName().equals("dependencies"));
            if (!hasDependenciesTag) {
                Maven.DependencyManagement currentDepMan = p.getDependencyManagement();
                Xml.Tag tag = currentDepMan.getTag();
                List<Content> contents = new ArrayList<>(tag.getContent());
                Xml.Tag dependenciesTag = new Xml.Tag(
                        randomId(),
                        "dependencies",
                        emptyList(),
                        emptyList(),
                        new Xml.Tag.Closing(randomId(), "dependencies", "", fmt2),
                        "",
                        fmt2);
                contents.add(dependenciesTag);
                currentDepMan = new Maven.DependencyManagement(currentDepMan.getModel(), tag.withContent(contents));
                p = p.withDependencyManagement(currentDepMan);
            }
            return p;
        }
    }

    private class InsertDependencyInOrder extends MavenRefactorVisitor {
        @Override
        public Maven visitPom(Maven.Pom pom) {
            Maven.Pom p = refactor(pom, super::visitPom);
            List<Maven.Dependency> dependencies = new ArrayList<>(pom.getDependencyManagement().getDependencies());

            // TODO if the dependency is manageable, make it managed
            Xml.Tag dependencyTag = createDependencyTag(formatter.wholeSourceIndent());

            Maven.Dependency toAdd = new Maven.DependencyManagement.Dependency(false,
                    new MavenModel.Dependency(
                            new MavenModel.ModuleVersionId(groupId, artifactId, null, version, "jar"),
                            version,
                            scope
                    ),
                    dependencyTag
            );

            if (dependencies.isEmpty()) {
                dependencies.add(toAdd);
            } else {
                // if everything were ideally sorted, which dependency would the addable dependency
                // come after?
                List<Maven.Dependency> sortedDependencies = new ArrayList<>(pom.getDependencyManagement().getDependencies());
                sortedDependencies.add(toAdd);
                dependencies.sort(Comparator.comparing(d -> d.getModel().getModuleVersion()));

                int addAfterIndex = -1;
                for (int i = 0; i < sortedDependencies.size(); i++) {
                    Maven.Dependency d = sortedDependencies.get(i);
                    if (toAdd == d) {
                        addAfterIndex = i - 1;
                        break;
                    }
                }
                dependencies.add(addAfterIndex + 1, toAdd);
            }
            p = p.withDependencyManagement(p.getDependencyManagement().withDependencies(dependencies));
            return p;
        }
    }

    private class UpdateDependency extends MavenRefactorVisitor {
        @Override
        public Maven visitPom(Maven.Pom pom) {
            Maven.Pom p = refactor(pom, super::visitPom);
            List<Maven.Dependency> dependencies = new ArrayList<>(pom.getDependencyManagement().getDependencies());
            Maven.Dependency dependency = findDependencyWithGroupIdAndArtifactId(dependencies, groupId, artifactId);

            final Xml.Tag tag = createDependencyTag(formatter.wholeSourceIndent());
            final Maven.Dependency updatedDependency = new Maven.DependencyManagement.Dependency(false,
                    new MavenModel.Dependency(
                            new MavenModel.ModuleVersionId(groupId, artifactId, null, version, "jar"),
                            version,
                            scope
                    ),
                    tag
            );

            dependencies.set(dependencies.indexOf(dependency), updatedDependency);
            p = p.withDependencyManagement(p.getDependencyManagement().withDependencies(dependencies));
            return p;
        }

        private Maven.Dependency findDependencyWithGroupIdAndArtifactId(List<Maven.Dependency> dependencies, String groupId, String artifactId) {
            return dependencies.stream()
                    .filter(d -> groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()))
                    .findFirst().get();
        }

        private int getIndexOfDependencyToBeRemoved(List<Maven.Dependency> dependencies) {
            final Optional<Maven.Dependency> optionalDependency = dependencies.stream()
                    .filter(d -> groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()))
                    .findFirst();
            return dependencies.indexOf(optionalDependency.get());
        }

        private void removeDependency(List<Maven.Dependency> dependencies) {
            final Optional<Maven.Dependency> optionalDependency = dependencies.stream()
                    .filter(d -> groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()))
                    .findFirst();
            final Maven.Dependency dependency = optionalDependency.get();
            dependencies.remove(dependency);
        }
    }

    private class AddOrUpdateDependency extends MavenRefactorVisitor {
        @Override
        public Maven visitPom(Maven.Pom pom) {
            if (dependencyWithGroupIdAndArtifactIdExists(pom)) {
                andThen(new AddDependenciesTagIfNotPresent());
                andThen(new UpdateDependency());
            } else if (dependencyDoesNotExist(pom)) {
                andThen(new AddDependenciesTagIfNotPresent());
                andThen(new InsertDependencyInOrder());
            }
            return pom;
        }

        private boolean dependencyDoesNotExist(Maven.Pom pom) {
            if (pom.getDependencyManagement() != null) {
                return !dependencyWithGroupIdAndArtifactIdExists(pom);
            }
            return false;
        }

        private boolean dependencyWithGroupIdAndArtifactIdExists(Maven.Pom pom) {
            if (pom.getDependencyManagement() != null) {
                return pom.getDependencyManagement().getDependencies().stream()
                        .anyMatch(d -> groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()));
            }
            return false;
        }
    }
}
