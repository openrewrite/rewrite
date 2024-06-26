/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.TagNameComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddParentPom extends Recipe {
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group ID",
            description = "The group ID of the maven parent pom to be adopted.",
            example = "org.springframework.boot")
    String groupId;

    @Option(displayName = "Artifact ID",
            description = "The artifact ID of the maven parent pom to be adopted.",
            example = "spring-boot-starter-parent")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X")
    String version;

    @Option(displayName = "Relative path",
            description = "New relative path attribute for parent lookup.",
            example = "../pom.xml")
    @Nullable
    String relativePath;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "File pattern",
            description = "A glob expression that can be used to constrain which directories or source files should be searched. " +
                          "Multiple patterns may be specified, separated by a semicolon `;`. " +
                          "If multiple patterns are supplied any of the patterns matching will be interpreted as a match. " +
                          "When not set, all source files are searched. ",
            required = false,
            example = "**/*-parent/grpc-*/pom.xml")
    @Nullable
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Change Maven parent";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    @Override
    public String getDescription() {
        return "Change the parent pom of a Maven pom.xml. Identifies the parent pom to be changed by its groupId and artifactId.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        //noinspection ConstantConditions
        if (version != null) {
            validated = validated.and(Semver.validate(version, versionPattern));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = Semver.validate(version, versionPattern).getValue();
        assert versionComparator != null;

        return Preconditions.check(new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                if (filePattern == null || PathUtils.matchesGlob(document.getSourcePath(), filePattern)) {
                    return SearchResult.found(document);
                }
                return document;

            }
        }, new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            private Collection<String> availableVersions;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag root = document.getRoot();
                // TODO What to do when a parent is already present?
                if (!root.getChild("parent").isPresent()) {
                    document = (Xml.Document) new AddToTagVisitor<>(root, Xml.Tag.build("<parent/>"))
                            .visitNonNull(document, ctx, getCursor().getParentOrThrow());
                }
                return document;
            }

            @SuppressWarnings("OptionalGetWithoutIsPresent")
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                List<TreeVisitor<?, ExecutionContext>> changeParentTagVisitors = new ArrayList<>();

                changeParentTagVisitors.add(new ChangeTagValueVisitor<>(t.getChild("groupId").get(), groupId));
                changeParentTagVisitors.add(new ChangeTagValueVisitor<>(t.getChild("artifactId").get(), artifactId));
                changeParentTagVisitors.add(new ChangeTagValueVisitor<>(t.getChild("version").get(), version));

                final Xml.Tag relativePathTag;
                if (StringUtils.isBlank(relativePath)) {
                    relativePathTag = Xml.Tag.build("<relativePath />");
                } else {
                    relativePathTag = Xml.Tag.build("<relativePath>" + relativePath + "</relativePath>");
                }
                doAfterVisit(new AddToTagVisitor<>(t, relativePathTag, new MavenTagInsertionComparator(t.getChildren())));
                maybeUpdateModel();

                for (TreeVisitor<?, ExecutionContext> visitor : changeParentTagVisitors) {
                    doAfterVisit(visitor);
                }
                maybeUpdateModel();
                doAfterVisit(new RemoveRedundantDependencyVersions(null, null,
                        RemoveRedundantDependencyVersions.Comparator.GTE, null).getVisitor());
                return t;
            }

        });
    }

    private static Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private static Map<String, String> getPropertiesInUse(Xml.Document pomXml, ExecutionContext ctx) {
        Map<String, String> properties = new HashMap<>();
        new MavenIsoVisitor<ExecutionContext>() {

            @Nullable
            ResolvedPom resolvedPom = null;

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (t.getContent() != null && t.getContent().size() == 1 && t.getContent().get(0) instanceof Xml.CharData) {
                    String text = ((Xml.CharData) t.getContent().get(0)).getText().trim();
                    Matcher m = PROPERTY_PATTERN.matcher(text);
                    while (m.find()) {
                        if (resolvedPom == null) {
                            resolvedPom = getResolutionResult().getPom();
                        }
                        String propertyName = m.group(1).trim();
                        if (resolvedPom.getProperties().containsKey(propertyName) && !isGlobalProperty(propertyName)) {
                            properties.put(m.group(1).trim(), resolvedPom.getProperties().get(propertyName));
                        }
                    }
                }
                return t;
            }

            private boolean isGlobalProperty(String propertyName) {
                return propertyName.startsWith("project.") || propertyName.startsWith("env.")
                       || propertyName.startsWith("settings.") || propertyName.equals("basedir");
            }
        }.visit(pomXml, ctx);
        return properties;
    }

    private List<ResolvedManagedDependency> getDependenciesUnmanagedByNewParent(MavenResolutionResult mrr, ResolvedPom newParent) {
        ResolvedPom resolvedPom = mrr.getPom();

        // Dependencies managed by the current pom's own dependency management are irrelevant to parent upgrade
        List<ManagedDependency> locallyManaged = mrr.getPom().getRequested().getDependencyManagement();

        Set<GroupArtifactVersion> requestedWithoutExplicitVersion = resolvedPom.getRequested().getDependencies().stream()
                .filter(dep -> dep.getVersion() == null)
                // Dependencies explicitly managed by the current pom require no changes
                .filter(dep -> locallyManaged.stream()
                        .noneMatch(localManagedDep -> localManagedDep.getGroupId().equals(dep.getGroupId()) && localManagedDep.getArtifactId().equals(dep.getArtifactId())))
                .map(dep -> new GroupArtifactVersion(dep.getGroupId(), dep.getArtifactId(), null))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedWithoutExplicitVersion.isEmpty()) {
            return emptyList();
        }

        List<ResolvedManagedDependency> depsWithoutExplicitVersion = resolvedPom.getDependencyManagement().stream()
                .filter(dep -> requestedWithoutExplicitVersion.contains(dep.getGav().withVersion(null)))
                // Exclude dependencies managed by a bom imported by the current pom
                .filter(dep -> dep.getBomGav() == null || locallyManaged.stream()
                        .noneMatch(localManagedDep -> localManagedDep.getGroupId().equals(dep.getBomGav().getGroupId()) && localManagedDep.getArtifactId().equals(dep.getBomGav().getArtifactId())))
                .collect(Collectors.toList());

        if (depsWithoutExplicitVersion.isEmpty()) {
            return emptyList();
        }

        // Remove from the list any that would still be managed under the new parent
        Set<GroupArtifact> newParentManagedGa = newParent.getDependencyManagement().stream()
                .map(dep -> new GroupArtifact(dep.getGav().getGroupId(), dep.getGav().getArtifactId()))
                .collect(Collectors.toSet());

        depsWithoutExplicitVersion = depsWithoutExplicitVersion.stream()
                .filter(it -> !newParentManagedGa.contains(new GroupArtifact(it.getGav().getGroupId(), it.getGav().getArtifactId())))
                .collect(Collectors.toList());
        return depsWithoutExplicitVersion;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class UnconditionalAddProperty extends MavenIsoVisitor<ExecutionContext> {
        String key;
        String value;

        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
            Xml.Document d = super.visitDocument(document, ctx);
            Xml.Tag root = d.getRoot();
            Optional<Xml.Tag> properties = root.getChild("properties");
            if (!properties.isPresent()) {
                Xml.Tag propertiesTag = Xml.Tag.build("<properties>\n<" + key + ">" + value + "</" + key + ">\n</properties>");
                d = (Xml.Document) new AddToTagVisitor<ExecutionContext>(root, propertiesTag, new MavenTagInsertionComparator(root.getChildren())).visitNonNull(d, ctx);
            } else if (!properties.get().getChildValue(key).isPresent()) {
                Xml.Tag propertyTag = Xml.Tag.build("<" + key + ">" + value + "</" + key + ">");
                d = (Xml.Document) new AddToTagVisitor<>(properties.get(), propertyTag, new TagNameComparator()).visitNonNull(d, ctx);
            }
            if (d != document) {
                maybeUpdateModel();
            }
            return d;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if (isPropertyTag() && key.equals(tag.getName())
                && !value.equals(tag.getValue().orElse(null))) {
                t = (Xml.Tag) new ChangeTagValueVisitor<>(tag, value).visitNonNull(t, ctx);
            }
            return t;
        }
    }
}
