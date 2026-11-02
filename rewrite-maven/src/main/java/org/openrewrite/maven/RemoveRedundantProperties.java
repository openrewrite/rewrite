/*
 * Copyright 2025 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Version;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.openrewrite.internal.StringUtils.isNullOrEmpty;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveRedundantProperties extends Recipe {
    private static final Map<String, String> SUPER_POM_DEFAULTS = new HashMap<>();
    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+)*([.-][\\w.-]*)?");

    static {
        SUPER_POM_DEFAULTS.put("project.build.sourceEncoding", "UTF-8");
        SUPER_POM_DEFAULTS.put("project.reporting.outputEncoding", "UTF-8");
        SUPER_POM_DEFAULTS.put("project.build.outputTimestamp", "1980-02-01T00:00:00Z");
    }

    @Option(displayName = "Property name",
            description = "Property name glob expression pattern used to match properties that should be checked.",
            example = "*.version",
            required = false)
    @Nullable
    String namePattern;

    @Option(displayName = "Only if values match",
            description = "Deprecated; use `onlyIfParentValueIs` instead. " +
                    "Only remove the property if its value exactly matches the property value in the parent pom. " +
                    "Default `false`.",
            required = false)
    @Nullable
    @Deprecated
    Boolean onlyIfValuesMatch;

    @Option(displayName = "Only if parent value is ...",
            description = "Only remove the property if the value it would inherit from the parent POM has the specified " +
                    "comparative relationship to its own value. For example, `gte` only removes properties pinned to a " +
                    "version that is the same as or older than the parent's. Ordering comparisons only apply when both " +
                    "values are versions; other values only match when they are equal. Default `any`.",
            valid = {"ANY", "EQ", "LT", "LTE", "GT", "GTE"},
            required = false)
    @Nullable
    Comparator onlyIfParentValueIs;

    public RemoveRedundantProperties(@Nullable String namePattern, @Nullable Comparator onlyIfParentValueIs) {
        this(namePattern, null, onlyIfParentValueIs);
    }

    @JsonCreator
    private RemoveRedundantProperties(@Nullable String namePattern, @Nullable Boolean onlyIfValuesMatch,
                                      @Nullable Comparator onlyIfParentValueIs) {
        this.namePattern = namePattern;
        this.onlyIfValuesMatch = onlyIfValuesMatch;
        this.onlyIfParentValueIs = onlyIfParentValueIs;
    }

    public enum Comparator {
        ANY,
        EQ,
        LT,
        LTE,
        GT,
        GTE
    }

    String displayName = "Remove redundant properties";

    String description = "Remove properties when a parent POM specifies the same property. When the project is built " +
            "with Maven 3.10 or later, according to its `BuildTool` marker, the super POM's defaults count as well: " +
            "`project.build.sourceEncoding` and `project.reporting.outputEncoding` set to `UTF-8`, and " +
            "`project.build.outputTimestamp` set to `1980-02-01T00:00:00Z` are removed when no parent POM sets them.";

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (onlyIfValuesMatch != null && onlyIfParentValueIs != null) {
            validated = validated.and(Validated.invalid("onlyIfValuesMatch", onlyIfValuesMatch,
                    "is deprecated in favor of onlyIfParentValueIs, and they cannot be used together"));
        }
        return validated;
    }

    private Comparator determineComparator() {
        if (onlyIfValuesMatch != null) {
            return onlyIfValuesMatch ? Comparator.EQ : Comparator.ANY;
        }
        return onlyIfParentValueIs == null ? Comparator.ANY : onlyIfParentValueIs;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Comparator comparator = determineComparator();
        return new MavenIsoVisitor<ExecutionContext>() {
            private final Map<ResolvedGroupArtifactVersion, @Nullable Map<String, String>> externalParentProperties = new HashMap<>();

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isMatchingPropertyTag(tag) && hasMatchingValue(tag, ctx)) {
                    doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                    maybeUpdateModel();
                }

                return t;
            }

            private boolean isMatchingPropertyTag(Xml.Tag tag) {
                return isPropertyTag() && (isNullOrEmpty(namePattern) || matchesGlob(tag.getName(), namePattern));
            }

            private boolean hasMatchingValue(Xml.Tag tag, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                MavenResolutionResult rootProject = mrr;
                while (rootProject.getParent() != null) {
                    rootProject = rootProject.getParent();
                }
                if (externalParentProperties(rootProject, ctx) == null) {
                    return false;
                }
                String parentPropertyValue = inheritedValue(mrr, tag.getName(), ctx);
                if (parentPropertyValue == null) {
                    return equalsSuperPomDefault(tag, mrr);
                }
                return comparator == Comparator.ANY || tag.getValue()
                        .map(value -> matchesComparator(parentPropertyValue, value))
                        .orElse(false);
            }

            /// The value `mrr` would inherit for `name` once this recipe has removed every
            /// redundant ancestor property of the same name, so that a stale pin repeated in
            /// a local parent does not stand in for the value the external parent provides.
            private @Nullable String inheritedValue(MavenResolutionResult mrr, String name, ExecutionContext ctx) {
                MavenResolutionResult parent = mrr.getParent();
                if (parent == null) {
                    Map<String, String> properties = externalParentProperties(mrr, ctx);
                    return properties == null ? null : properties.get(name);
                }
                String parentOwnValue = parent.getPom().getRequested().getProperties().get(name);
                String grandparentValue = inheritedValue(parent, name, ctx);
                if (parentOwnValue == null || grandparentValue != null && matchesComparator(grandparentValue, parentOwnValue)) {
                    return grandparentValue != null ? grandparentValue : parent.getPom().getProperties().get(name);
                }
                return parentOwnValue;
            }

            private @Nullable Map<String, String> externalParentProperties(MavenResolutionResult mrr, ExecutionContext ctx) {
                if (mrr.getPom().getRequested().getParent() == null) {
                    return emptyMap();
                }
                return externalParentProperties.computeIfAbsent(mrr.getPom().getGav(), gav -> {
                    MavenPomDownloader downloader = new MavenPomDownloader(
                            mrr.getProjectPoms(),
                            ctx,
                            MavenExecutionContextView.view(ctx).effectiveSettings(mrr),
                            mrr.getActiveProfiles());
                    try {
                        // Resolve the external parent POM properties
                        return mrr
                                .getPom()
                                .getRequested()
                                .withProperties(emptyMap())
                                .withDependencies(emptyList())
                                .withDependencyManagement(emptyList())
                                .withPlugins(emptyList())
                                .withPluginManagement(emptyList())
                                .resolve(mrr.getActiveProfiles(), downloader, ctx)
                                .getProperties();
                    } catch (MavenDownloadingException e) {
                        return null;
                    }
                });
            }

            private boolean matchesComparator(String parentValue, String value) {
                if (comparator == Comparator.ANY) {
                    return true;
                }
                if (parentValue.equals(value)) {
                    return comparator == Comparator.EQ || comparator == Comparator.LTE || comparator == Comparator.GTE;
                }
                if (!isVersion(parentValue) || !isVersion(value)) {
                    return false;
                }
                int comparison = new Version(parentValue).compareTo(new Version(value));
                if (comparison < 0) {
                    return comparator == Comparator.LT || comparator == Comparator.LTE;
                } else if (comparison > 0) {
                    return comparator == Comparator.GT || comparator == Comparator.GTE;
                }
                return comparator == Comparator.EQ || comparator == Comparator.LTE || comparator == Comparator.GTE;
            }

            private boolean isVersion(String value) {
                return VERSION.matcher(value).matches();
            }

            private boolean equalsSuperPomDefault(Xml.Tag tag, MavenResolutionResult mrr) {
                String superPomDefault = SUPER_POM_DEFAULTS.get(tag.getName());
                if (superPomDefault == null || !isBuiltWithSuperPomDefaults()) {
                    return false;
                }
                return tag.getValue()
                        .map(value -> superPomDefault.equalsIgnoreCase(mrr.getPom().getValue(value)))
                        .orElse(false);
            }

            private boolean isBuiltWithSuperPomDefaults() {
                return getCursor().firstEnclosingOrThrow(Xml.Document.class).getMarkers()
                        .findFirst(BuildTool.class)
                        .filter(buildTool -> buildTool.getType() == BuildTool.Type.Maven)
                        .map(buildTool -> new LatestRelease(null).compare(null, buildTool.getVersion(), "3.10.0") >= 0)
                        .orElse(false);
            }
        };
    }
}
