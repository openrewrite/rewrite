/*
 * Copyright 2021 the original author or authors.
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

import static org.openrewrite.Validated.required;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependencyGroupIdAndArtifactId extends AbstractChangeDependencyGroupIdAndArtifactId {
    @Option(displayName = "Override managed version",
            description = "If the new dependency has a managed version, this flag can be used to explicitly set the version on the dependency. The default for this flag is `false`.",
            required = false)
    @Nullable
    Boolean overrideManagedVersion;

    @Option(displayName = "Update dependency management",
            description = "Also update the dependency management section. The default for this flag is `true`.",
            required = false)
    @Nullable
    Boolean changeManagedDependency;

    public ChangeDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId,
            @Nullable String newVersion, @Nullable String versionPattern) {
        this(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, versionPattern, false, true);
    }

    @JsonCreator
    public ChangeDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId,
            @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean overrideManagedVersion,
            @Nullable Boolean changeManagedDependency) {
        super(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, versionPattern);
        this.overrideManagedVersion = overrideManagedVersion;
        this.changeManagedDependency = changeManagedDependency;
    }

    @Override
    public String getDisplayName() {
        return "Change Maven dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", oldGroupId, oldArtifactId);
    }

    @Override
    public String getDescription() {
        return "Change a Maven dependency coordinates. The `newGroupId` or `newArtifactId` **MUST** be different from before.";
    }

    @Override
    protected @NotNull Validated<Object> addExtraValidation(Validated<Object> validated) {
        return validated.and(required("newGroupId", newGroupId).or(required("newArtifactId", newArtifactId)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AbstractChangeDependencyGroupIdAndArtifactIdVisitor() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                // Any managed dependency change is unlikely to use the same version, so only update selectively.
                if ((changeManagedDependency == null || changeManagedDependency) && newVersion != null || versionPattern != null) {
                    doAfterVisit(new ChangeManagedDependencyGroupIdAndArtifactId(
                            oldGroupId, oldArtifactId,
                            Optional.ofNullable(newGroupId).orElse(oldGroupId),
                            Optional.ofNullable(newArtifactId).orElse(oldArtifactId),
                            newVersion, versionPattern).getVisitor());
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (isDependencyTag(oldGroupId, oldArtifactId)) {
                    String groupId = newGroupId;
                    if (groupId != null) {
                        t = changeChildTagValue(t, "groupId", groupId, ctx);
                    } else {
                        groupId = t.getChildValue("groupId").orElseThrow(NoSuchElementException::new);
                    }
                    String artifactId = newArtifactId;
                    if (artifactId != null) {
                        t = changeChildTagValue(t, "artifactId", artifactId, ctx);
                    } else {
                        artifactId = t.getChildValue("artifactId").orElseThrow(NoSuchElementException::new);
                    }
                    String currentVersion = t.getChildValue("version").orElse(null);
                    if (newVersion != null) {
                        try {
                            String resolvedNewVersion = resolveSemverVersion(ctx, groupId, artifactId, currentVersion);
                            Optional<Xml.Tag> scopeTag = t.getChild("scope");
                            Scope scope = scopeTag.map(xml -> Scope.fromName(xml.getValue().orElse("compile"))).orElse(Scope.Compile);
                            Optional<Xml.Tag> versionTag = t.getChild("version");

                            boolean configuredToOverrideManageVersion = overrideManagedVersion != null && overrideManagedVersion; // False by default
                            boolean configuredToChangeManagedDependency = changeManagedDependency == null || changeManagedDependency; // True by default

                            boolean versionTagPresent = versionTag.isPresent();
                            boolean oldDependencyManaged = isDependencyManaged(scope, oldGroupId, oldArtifactId);
                            boolean newDependencyManaged = isDependencyManaged(scope, groupId, artifactId);
                            if (versionTagPresent) {
                                // If the previous dependency had a version but the new artifact is managed, removed the version tag.
                                if (!configuredToOverrideManageVersion && newDependencyManaged || (oldDependencyManaged && configuredToChangeManagedDependency)) {
                                    t = (Xml.Tag) new RemoveContentVisitor<>(versionTag.get(), false).visit(t, ctx);
                                } else {
                                    // Otherwise, change the version to the new value.
                                    t = changeChildTagValue(t, "version", resolvedNewVersion, ctx);
                                }
                            } else if (configuredToOverrideManageVersion || !newDependencyManaged) {
                                //If the version is not present, add the version if we are explicitly overriding a managed version or if no managed version exists.
                                Xml.Tag newVersionTag = Xml.Tag.build("<version>" + resolvedNewVersion + "</version>");
                                //noinspection ConstantConditions
                                t = (Xml.Tag) new AddToTagVisitor<ExecutionContext>(t, newVersionTag, new MavenTagInsertionComparator(t.getChildren())).visitNonNull(t, ctx, getCursor().getParent());
                            }
                        } catch (MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }
                    if (t != tag) {
                        maybeUpdateModel();
                    }
                }

                //noinspection ConstantConditions
                return t;
            }

            private boolean isDependencyManaged(Scope scope, String groupId, String artifactId) {
        				@Nullable
                ResolvedManagedDependency dependency = getResolutionResult().getPom().getResolvedManagedDependencyWithMinimumProximity(dm ->
                        groupId.equals(dm.getGroupId()) && artifactId.equals(dm.getArtifactId()));
                return dependency != null && scope.isInClasspathOf(dependency.getScope());
            }
        };
    }
}
