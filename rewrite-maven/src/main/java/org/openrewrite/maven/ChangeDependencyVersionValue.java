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
public class ChangeDependencyVersionValue extends Recipe {
    private static final Logger log = LoggerFactory.getLogger(ChangeDependencyVersionValue.class);

    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    // there are several implicitly defined version properties that we should never attempt to update
    private static final Set<String> implicitlyDefinedVersionProperties = new HashSet<>(Arrays.asList(
            "${version}", "${project.version}", "${pom.version}", "${project.parent.version}"
    ));

    public enum VersionLocation {
        DEPENDENCY ("DEPENDENCY"),
        MANAGED_DEPENDENCY ("MANAGED_DEPENDENCY"),
        PLUGIN_DEPENDENCY ("PLUGIN_DEPENDENCY");

        private final String name;

        VersionLocation(String name) {
            this.name = name;
        }

        @Override
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

    @Option(displayName = "Change property version name",
            description = "Allows property version name to be changed to best practice naming convention or what ever is in the newPropertyVersionName option. The default for this flag is `false`.",
            required = false)
    @Nullable
    Boolean changePropertyVersionName;

    @Option(displayName = "New property version name",
            description = "The new property version name used as property variable name. If it is set it will be changed. No need to set the changePropertyVersionName to true.",
            example = "example.property.version",
            required = false)
    @Nullable
    String newPropertyVersionName;

    @Option(displayName = "Declare the location where the dependency should be changed.",
            description = "Changes dependency version right where you want it. The default is set to change it everywhere." +
                    "But you can also specifically target `DEPENDENCY`, `MANAGED_DEPENDENCY` or `PLUGIN_DEPENDENCY`." +
                    "It also doesn't matter if they are in a profile or not.",
            example = "DEPENDENCY",
            required = false)
    @Nullable
    ChangeDependencyVersionValue.VersionLocation versionLocation;

    public ChangeDependencyVersionValue(String groupId, String artifactId, @Nullable String newVersion, @Nullable String versionLocation) {
        this(groupId, artifactId, newVersion, null, false, null, versionLocation);
    }

    public ChangeDependencyVersionValue(String groupId, String artifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean changePropertyVersionName, @Nullable String versionLocation) {
        this(groupId, artifactId, newVersion, versionPattern, changePropertyVersionName, null, versionLocation);
    }

    @JsonCreator
    public ChangeDependencyVersionValue(String groupId, String artifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean changePropertyVersionName, @Nullable String newPropertyVersionName,  @Nullable String versionLocation) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.changePropertyVersionName = changePropertyVersionName != null && changePropertyVersionName; // False by default
        this.newPropertyVersionName = newPropertyVersionName;
        this.versionLocation = versionLocation != null ? VersionLocation.valueOf(versionLocation) : null;
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
        return "Change Maven dependency version";
    }

    @Override
    public String getDescription() {
        return "Change a Maven dependency version. Declare `groupId` and `artifactId` of the dependency in which the version should be changed. " +
                "By adding `versionLocation`, a set of dependencies can be targeted such as managed Dependencies with `MANAGED_DEPENDENCY`.";
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

                if (artifactId != null){
                    if (matchesGlob(tag.getChildValue("groupId").orElse(null), groupId) && (artifactId == null || matchesGlob(tag.getChildValue("artifactId").orElse(null), artifactId))) {
                        String artifact = artifactId == null ? t.getChildValue("artifactId").orElse(null) : artifactId;

                        if (newVersion != null){
                            if (versionLocation == null){
                                return changeVersion(t, ctx);
                            }
                            switch (Objects.requireNonNull(versionLocation)) {
                                case DEPENDENCY:
                                    if (isDependencyTag() || PROFILE_DEPENDENCY_MATCHER.matches(getCursor())){
                                        return changeVersion(t, ctx);
                                    }
                                    break;
                                case MANAGED_DEPENDENCY:
                                    if (isManagedDependencyTag() || PROFILE_MANAGED_DEPENDENCY_MATCHER.matches(getCursor())){
                                        return changeVersion(t, ctx);
                                    }
                                    break;
                                case PLUGIN_DEPENDENCY:
                                    if (isPluginDependencyTag(groupId, artifact) || PROFILE_PLUGIN_DEPENDENCY_MATCHER.matches(getCursor())){
                                        return changeVersion(t, ctx);
                                    }
                                    break;
                            }
                        }

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

            public Xml.Tag changeVersion(Xml.Tag t, ExecutionContext ctx) {

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
                                    doAfterVisit(new ChangePropertyValue(newPropertyVariable, resolvedNewVersion, false, false).getVisitor());
                                    changed = true;
                                }
                                newPropertyVariable = artifactId + ".version";
                                if (newPropertyVersionName != null) {
                                    newPropertyVariable = newPropertyVersionName;
                                }
                                if (!getResolutionResult().getPom().getProperties().containsKey(newPropertyVariable) && (Boolean.TRUE.equals(changePropertyVersionName) || newPropertyVersionName != null)) {
                                    doAfterVisit(new RenamePropertyKey(propertyVariable, newPropertyVariable).getVisitor());
                                    t = changeChildTagValue(t, "version", "${" + newPropertyVariable + "}", ctx);
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
