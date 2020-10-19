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

import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;
import org.openrewrite.refactor.Formatter;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.Formatting.format;
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

    @Setter
    private String groupId;

    @Setter
    private String artifactId;

    @Setter
    @Nullable
    private String version;

    @Setter
    @Nullable
    private String scope;

    @Setter
    @Nullable
    private String type;

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId));
    }

    @Override
    public Maven visitPom(Maven.Pom pom) {

        if (dependencyManagementSectionExists(pom) && managedDependencyExists(pom)) {
            return pom;
        }

        andThen(new AddDependenciesTagIfNotPresent());

        if (dependencyWithGroupIdAndArtifactIdExists(pom)) {
            andThen(new UpdateDependency());
        } else {
            andThen(new InsertDependencyInOrder());
        }

        return pom;
    }

    private boolean dependencyManagementSectionExists(Maven.Pom pom) {
        return pom.getModel().getDependencyManagement() != null;
    }

    private boolean managedDependencyExists(Maven.Pom pom) {
        if (pom.getDependencyManagement() != null) {
            return pom.getDependencyManagement().getDependencies().stream()
                    .anyMatch(this::sameDependencyExists);
        }
        return false;
    }

    private boolean sameDependencyExists(Maven.Dependency d) {
        return artifactId.equals(d.getArtifactId()) &&
                groupId.equals(d.getGroupId()) &&
                (version != null && d.getVersion() != null ? version.equals(d.getVersion()) :
                        version == null && d.getVersion() == null) &&
                (scope != null &&  d.getScope() != null ? scope.equals(d.getScope()) :
                        scope == null && d.getScope() == null) &&
                (type != null &&  d.getScope() != null ? type.equals(d.getScope()) :
                        type == null && d.getScope() == null);
    }

    private boolean dependencyWithGroupIdAndArtifactIdExists(Maven.Pom pom) {
        if (pom.getDependencyManagement() != null) {
            return pom.getDependencyManagement().getDependencies().stream()
                    .anyMatch(d -> groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()));
        }
        return false;
    }

    private static class AddDependenciesTagIfNotPresent extends MavenRefactorVisitor {
        @Override
        public Maven visitPom(Maven.Pom pom) {
            Maven.Pom p = refactor(pom, super::visitPom);
            if ( ! hasDependencyManagementTag(p)) {
                p = addDependencyManagementTag(p);
            }
            if ( ! hasDependenciesTag(p)) {
                p = addDependenciesTag(p);
            }
            return p;
        }

        private boolean hasDependencyManagementTag(Maven.Pom p) {
            return p.getDependencyManagement() != null;
        }

        private Maven.Pom addDependencyManagementTag(Maven.Pom p) {
            p = p.withDependencyManagement(new Maven.DependencyManagement(
                    new MavenModel.DependencyManagement(new ArrayList<>()),
                    createDependencyManagementTag(formatter.wholeSourceIndent())
            ));
            return p;
        }

        @NotNull
        private Xml.Tag createDependencyManagementTag(Formatter.Result indent) {
            int offset = 0;
            return new XmlParser().parse(
                    "<dependencyManagement>" + indent.getPrefix(offset) + "</dependencyManagement>"
            ).get(0).getRoot().withFormatting(format(indent.getPrefix(offset)));
        }

        private boolean hasDependenciesTag(Maven.Pom p) {
            return  p.getDependencyManagement() != null && p.getDependencyManagement().getDependencies() != null &&
                    ! p.getDependencyManagement().getDependencies().isEmpty();
        }

        private Maven.Pom addDependenciesTag(Maven.Pom p) {
            Maven.DependencyManagement dm = p.getDependencyManagement();
            if(dm == null) {
                return p;
            }
            Xml.Tag tag = dm.getTag();
            List<Content> contents = new ArrayList<>(tag.getContent());
            Xml.Tag dependenciesTag = createDependenciesTag(formatter.wholeSourceIndent());
            contents.add(dependenciesTag);
            dm = new Maven.DependencyManagement(dm.getModel(), tag.withContent(contents));
            p = p.withDependencyManagement(dm);
            return p;
        }

        private Xml.Tag createDependenciesTag(Formatter.Result indent) {
            int offset = 1;
            return new XmlParser().parse(
                    "<dependencies>" + indent.getPrefix(offset) + "</dependencies>"
            ).get(0).getRoot().withFormatting(format(indent.getPrefix(offset)));
        }
    }

    private class UpdateDependency extends MavenRefactorVisitor {
        @Override
        public Maven visitPom(Maven.Pom pom) {
            Maven.Pom p = refactor(pom, super::visitPom);
            Maven.DependencyManagement dm = pom.getDependencyManagement();
            if(dm == null) {
                return p;
            }
            List<Maven.Dependency> dependencies = new ArrayList<>(dm.getDependencies());
            Maven.Dependency dependency = findDependencyWithGroupIdAndArtifactId(dependencies);
            if(dependency == null) {
                return p;
            }
            final Maven.Dependency updatedDependency = createMavenDependencyManagementDependency(formatter.wholeSourceIndent());
            dependencies.set(dependencies.indexOf(dependency), updatedDependency);
            p = p.withDependencyManagement(dm.withDependencies(dependencies));
            return p;
        }

        private @Nullable Maven.Dependency findDependencyWithGroupIdAndArtifactId(List<Maven.Dependency> dependencies) {
            return dependencies.stream()
                    .filter(d -> groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()))
                    .findFirst()
                    .orElse(null);
        }

    }

    private class InsertDependencyInOrder extends MavenRefactorVisitor {
        @Override
        public Maven visitPom(Maven.Pom pom) {
            Maven.Pom p = refactor(pom, super::visitPom);
            Maven.DependencyManagement dm = pom.getDependencyManagement();
            if(dm == null) {
                return p;
            }
            List<Maven.Dependency> dependencies = new ArrayList<>(dm.getDependencies());
            Maven.Dependency toAdd = createMavenDependencyManagementDependency(formatter.wholeSourceIndent());
            int indexToAdd = 0;
            if ( ! dependencies.isEmpty() ) {
                List<Maven.Dependency> dependenciesWithSameGroupid = getDependenciesWithSameGroupId(pom, toAdd);
                if(dependenciesWithSameGroupid.isEmpty()) {
                    indexToAdd = findIndexWithoutExistingGroupId(dependencies, toAdd);
                } else {
                    indexToAdd = findIndexWithExistingGroupId(dependencies, toAdd, dependenciesWithSameGroupid);
                }
            }
            dependencies.add(indexToAdd, toAdd);
            p = p.withDependencyManagement(dm.withDependencies(dependencies));
            return p;
        }

        private int findIndexWithoutExistingGroupId(List<Maven.Dependency> dependencies, Maven.Dependency toAdd) {
            List<Maven.Dependency> sortedDependencies = new ArrayList<>(dependencies);
            sortedDependencies.add(toAdd);
            sortedDependencies.sort(Comparator
                    .comparing(Maven.Dependency::getGroupId)
                    .thenComparing(Maven.Dependency::getArtifactId));
            int indexInSortedList = sortedDependencies.indexOf(toAdd);
            if(indexInSortedList > 0) {
                Maven.Dependency predecessor = sortedDependencies.get(indexInSortedList -1);
                return dependencies.indexOf(predecessor) + 1;
            }
            return 0;
        }

        private int findIndexWithExistingGroupId(List<Maven.Dependency> dependencies, Maven.Dependency toAdd, List<Maven.Dependency> dependenciesWithSameGroupid) {
            int indexInSortedList;
            dependenciesWithSameGroupid.add(toAdd);
            dependenciesWithSameGroupid.sort(Comparator
                    .comparing(Maven.Dependency::getGroupId)
                    .thenComparing(Maven.Dependency::getArtifactId));
            indexInSortedList = dependenciesWithSameGroupid.indexOf(toAdd);

            if(indexInSortedList > 0) {
                Maven.Dependency predecessor = dependenciesWithSameGroupid.get(indexInSortedList - 1);
                indexInSortedList = dependencies.indexOf(predecessor) +1;
            } else {
                Maven.Dependency successor = dependenciesWithSameGroupid.get(indexInSortedList + 1);
                indexInSortedList = dependencies.indexOf(successor);
            }
            return indexInSortedList;
        }

        @NotNull
        private List<Maven.Dependency> getDependenciesWithSameGroupId(Maven.Pom pom, Maven.Dependency toAdd) {
            if(pom.getDependencyManagement() == null) {
                return Collections.emptyList();
            }
            return pom.getDependencyManagement().getDependencies().stream()
                    .filter(d -> d.getGroupId() != null && d.getGroupId().equals(toAdd.getGroupId())).collect(Collectors.toList());
        }
    }

    @NotNull
    private Maven.Dependency createMavenDependencyManagementDependency(Formatter.Result ident) {
        final Xml.Tag tag = createDependencyTag(ident);
        return new Maven.DependencyManagement.Dependency(false,
                new MavenModel.Dependency(
                        new MavenModel.ModuleVersionId(groupId, artifactId, null, version, "jar"),
                        version,
                        scope
                ),
                tag
        );
    }

    private Xml.Tag createDependencyTag(Formatter.Result indent) {
        int offset =  2;
        return new XmlParser().parse(
                "<dependency>" +
                        indent.getPrefix(offset + 1) + "<groupId>" + groupId + "</groupId>" +
                        indent.getPrefix(offset + 1) + "<artifactId>" + artifactId + "</artifactId>" +
                        (version == null ? "" :
                                indent.getPrefix(offset + 1) + "<version>" + version + "</version>") +
                        (scope == null ? "" :
                                indent.getPrefix(offset + 1) + "<scope>" + scope + "</scope>") +
                        (type == null ? "" :
                                indent.getPrefix(offset + 1) + "<type>" + type + "</type>") +
                        indent.getPrefix(offset) + "</dependency>"
                    ).get(0).getRoot().withFormatting(format(indent.getPrefix(offset)));
    }
}
