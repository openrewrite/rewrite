/*
 * Copyright 2022 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.openrewrite.Validated.test;
import static org.openrewrite.internal.StringUtils.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeVersionValue extends Recipe {
    private static final Logger log = LoggerFactory.getLogger(ChangeVersionValue.class);
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    // there are several implicitly defined version properties that we should never attempt to update
    private static final Set<String> implicitlyDefinedVersionProperties = new HashSet<>(Arrays.asList(
            "${version}", "${project.version}", "${pom.version}", "${project.parent.version}"
    ));

    public enum Changes {
        DEPENDENCY ("DEPENDENCY"),
        MANAGED_DEPENDENCY ("MANAGED_DEPENDENCY"),
        PLUGIN_DEPENDENCY ("PLUGIN_DEPENDENCY");

        private final String name;

        Changes(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }


    @Option(displayName = "New groupId",
            description = "The new groupId to use.",
            example = "corp.internal.openrewrite.recipe")
    String groupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use.",
            example = "rewrite-testing-frameworks")
    String artifactId;

    @Option(displayName = "New version",
            description = "The new version to use.",
            example = "2.0.0",
            required = false)
    @Nullable
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Change property version names",
            description = "Allows property version names to be changed to best practice naming convention",
            example = "-jre",
            required = false)
    @Nullable
    Boolean changePropertyVersionNames;

    @Option(displayName = "Change property version names",
            description = "Allows property version names to be changed to best practice naming convention",
            example = "-jre",
            required = false)
    @Nullable
    Changes versionChangePlace;

    public ChangeVersionValue(String groupId, String artifactId, @Nullable String newVersion, @Nullable String versionChangePlace) {
        this(groupId, artifactId, newVersion, null, false, versionChangePlace);
    }

    public ChangeVersionValue(String groupId, String artifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable String versionChangePlace) {
        this(groupId, artifactId, newVersion, versionPattern, false, versionChangePlace);
    }

    @JsonCreator
    public ChangeVersionValue(String groupId, String artifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean changePropertyVersionNames, @Nullable String versionChangePlace) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.changePropertyVersionNames = changePropertyVersionNames;
        this.versionChangePlace = versionChangePlace != null ? Changes.valueOf(versionChangePlace) : null;
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        validated =
                validated.and(test(
                        "coordinates",
                        "newGroupId OR newArtifactId must be different from before",
                        this,
                        r -> {
                            boolean sameGroupId = isBlank(r.groupId);
                            boolean sameArtifactId = isBlank(r.artifactId);
                            return !(sameGroupId && sameArtifactId);
                        }
                ));
        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Change the version or the referenced property version of dependencies or plugins";
    }

    @Override
    public String getDescription() {
        return "Change the version or the referenced property version of dependencies or plugins.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            final VersionComparator versionComparator = newVersion != null ? Semver.validate(newVersion, versionPattern).getValue() : null;
            @Nullable
            private Collection<String> availableVersions;

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {

                Xml.Tag t = super.visitTag(tag, ctx);

                String artifact = artifactId == null ? t.getChildValue("artifactId").orElse(null) : artifactId;

                if (matchesGlob(tag.getChildValue("groupId").orElse(null), groupId) && (artifactId == null || matchesGlob(tag.getChildValue("artifactId").orElse(null), artifactId))) {
                    if (versionChangePlace == null){
                        return changeVersion(t, ctx, groupId, artifact, newVersion, changePropertyVersionNames);
                    }
                    switch (Objects.requireNonNull(versionChangePlace)) {
                        case DEPENDENCY:
                        if (isDependencyTag() || PROFILE_DEPENDENCY_MATCHER.matches(getCursor())){
                                return changeVersion(t, ctx, groupId, artifact, newVersion, changePropertyVersionNames);
                            }
                        break;
                        case MANAGED_DEPENDENCY:
                        if (isManagedDependencyTag() || PROFILE_MANAGED_DEPENDENCY_MATCHER.matches(getCursor())){
                                return changeVersion(t, ctx, groupId, artifact, newVersion, changePropertyVersionNames);
                            }
                        break;
                        case PLUGIN_DEPENDENCY:
                        if (isPluginDependencyTag(groupId, artifact) || PROFILE_PLUGIN_DEPENDENCY_MATCHER.matches(getCursor())){
                                return changeVersion(t, ctx, groupId, artifact, newVersion, changePropertyVersionNames);
                            }
                        break;
                    }
                }
                return t;
            }

            private Xml.Tag changeChildTagValue(Xml.Tag tag, String childTagName, String newValue, ExecutionContext ctx) {
                Optional<Xml.Tag> childTag = tag.getChild(childTagName);
                if (childTag.isPresent() && !newValue.equals(childTag.get().getValue().orElse(null))) {
                    tag = (Xml.Tag) new ChangeTagValueVisitor<>(childTag.get(), newValue).visitNonNull(tag, ctx);
                }
                return tag;
            }

            @SuppressWarnings("ConstantConditions")
            private String resolveSemverVersion(ExecutionContext ctx, String groupId, String artifactId, @Nullable String currentVersion) throws MavenDownloadingException {
                if (currentVersion.contains("$")) {
                    String propertyVariable = currentVersion.substring(2, currentVersion.length() - 1);
                    currentVersion = getResolutionResult().getPom().getProperties().get(propertyVariable);
                }

                if (versionComparator == null) {
                    return newVersion;
                }
                String finalCurrentVersion = currentVersion != null ? currentVersion : newVersion;
                if (availableVersions == null) {
                    availableVersions = new ArrayList<>();
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                    for (String v : mavenMetadata.getVersioning().getVersions()) {
                        if (versionComparator.isValid(finalCurrentVersion, v)) {
                            availableVersions.add(v);
                        }
                    }

                }
                return availableVersions.isEmpty() ? newVersion : Collections.max(availableVersions, versionComparator);
            }

            public Xml.Tag changeVersion(Xml.Tag t, ExecutionContext ctx, String groupId, String artifactId, String newVersion, Boolean changePropertyVersionNames) {

                boolean changed = false;
                if (newVersion != null) {
                    try {
                        Optional<Xml.Tag> versionTag = t.getChild("version");
                        if (versionTag.isPresent()) {
                            String version = versionTag.get().getValue().orElse(null);
                            String resolvedNewVersion = resolveSemverVersion(ctx, groupId, artifactId == null ? t.getChildValue("artifactId").orElse(null) : artifactId, version);
                            if (Objects.requireNonNull(version).contains("$")) {
                                String propertyVariable = version.substring(2, version.length() - 1);
                                String newPropertyVariable = propertyVariable;
                                if (!matchesGlob(getResolutionResult().getPom().getProperties().get(propertyVariable), resolvedNewVersion)) {
                                    if (Boolean.TRUE.equals(changePropertyVersionNames)) {
                                        newPropertyVariable = artifactId + ".version";
                                        doAfterVisit(new RenamePropertyKey(propertyVariable, newPropertyVariable).getVisitor());
                                        t = changeChildTagValue(t, "version", "${" + newPropertyVariable + "}", ctx);
                                    }
                                    doAfterVisit(new ChangePropertyValue(newPropertyVariable, resolvedNewVersion, false, false).getVisitor());
                                    changed = true;
                                }
                            } else {
                                if (!matchesGlob(t.getChildValue("version").orElse(null), resolvedNewVersion)){
                                    t = changeChildTagValue(t, "version", resolvedNewVersion, ctx);
                                    changed = true;
                                }
                            }

                        }

                    } catch (MavenDownloadingException e) {
                        return e.warn(t);
                    }
                }
                if (changed) {
                    maybeUpdateModel();
                }

                return t;
            }
        };
    }
}
