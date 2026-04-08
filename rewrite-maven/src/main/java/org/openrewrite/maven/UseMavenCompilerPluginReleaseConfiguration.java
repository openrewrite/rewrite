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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseMavenCompilerPluginReleaseConfiguration extends ScanningRecipe<UseMavenCompilerPluginReleaseConfiguration.Accumulator> {

    private static final Pattern MAVEN_COMPILER_PROPERTY_PATTERN =
            Pattern.compile("\\$\\{(maven\\.compiler\\.(?:source|target))}");

    @Option(
            displayName = "Release version",
            description = "The new value for the release configuration. This recipe prefers ${java.version} if defined.",
            example = "11"
    )
    Integer releaseVersion;

    String displayName = "Use Maven compiler plugin release configuration";

    String description = "Replaces any explicit `source` or `target` configuration (if present) on the `maven-compiler-plugin` with " +
                "`release`, and updates the `release` value if needed. Will not downgrade the Java version if the current version is higher. " +
                "Also removes stale `maven.compiler.source` and `maven.compiler.target` properties that are no longer referenced.";

    public static class Accumulator {
        Map<String, Set<ResolvedGroupArtifactVersion>> propertyUsages = new HashMap<>();
        Set<ResolvedGroupArtifactVersion> pomsWithRelease = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                // Track ${maven.compiler.source} and ${maven.compiler.target} usages
                // outside of the <properties> section
                if (!isPropertyTag()) {
                    Optional<String> value = t.getValue();
                    if (value.isPresent()) {
                        Matcher matcher = MAVEN_COMPILER_PROPERTY_PATTERN.matcher(value.get());
                        while (matcher.find()) {
                            acc.propertyUsages.computeIfAbsent(matcher.group(1), k -> new HashSet<>())
                                    .add(getResolutionResult().getPom().getGav());
                        }
                    }
                }

                // Track POMs that have <release> in the compiler plugin configuration
                if (isPluginTag("org.apache.maven.plugins", "maven-compiler-plugin")) {
                    Optional<Xml.Tag> config = t.getChild("configuration");
                    if (config.isPresent() && config.get().getChildValue("release").isPresent()) {
                        acc.pomsWithRelease.add(getResolutionResult().getPom().getGav());
                    }
                }

                return t;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                // Handle compiler plugin source/target → release replacement (existing logic)
                if (isPluginTag("org.apache.maven.plugins", "maven-compiler-plugin")) {
                    t = handleCompilerPlugin(t);
                }

                // Handle stale maven.compiler.source/target property removal
                if (isPropertyTag() &&
                    ("maven.compiler.source".equals(t.getName()) || "maven.compiler.target".equals(t.getName()))) {
                    if (isPropertyStale(t.getName(), acc)) {
                        doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                        maybeUpdateModel();
                    }
                }

                return t;
            }

            private Xml.Tag handleCompilerPlugin(Xml.Tag t) {
                Optional<Xml.Tag> maybeCompilerPluginConfig = t.getChild("configuration");
                if (!maybeCompilerPluginConfig.isPresent()) {
                    return t;
                }
                Xml.Tag compilerPluginConfig = maybeCompilerPluginConfig.get();
                Optional<String> source = compilerPluginConfig.getChildValue("source");
                Optional<String> target = compilerPluginConfig.getChildValue("target");
                Optional<String> release = compilerPluginConfig.getChildValue("release");
                if (!source.isPresent() && !target.isPresent() && !release.isPresent()) {
                    return t;
                }
                if (currentNewerThanProposed(source) ||
                        currentNewerThanProposed(target) ||
                        currentNewerThanProposed(release)) {
                    return t;
                }

                Xml.Tag updated = filterTagChildren(t, compilerPluginConfig,
                        child -> !("source".equals(child.getName()) || "target".equals(child.getName())));
                String existingPropertyRef = getExistingPropertyReference(release, source, target);
                String releaseVersionValue;
                if (existingPropertyRef != null) {
                    releaseVersionValue = existingPropertyRef;
                } else if (hasJavaVersionProperty(getCursor().firstEnclosingOrThrow(Xml.Document.class))) {
                    releaseVersionValue = "${java.version}";
                } else {
                    releaseVersionValue = releaseVersion.toString();
                }
                return addOrUpdateChild(updated, compilerPluginConfig,
                        Xml.Tag.build("<release>" + releaseVersionValue + "</release>"), getCursor().getParentOrThrow());
            }

            private boolean isPropertyStale(String propertyName, Accumulator acc) {
                ResolvedGroupArtifactVersion currentGav = getResolutionResult().getPom().getGav();

                // Check if any POM references this property
                Set<ResolvedGroupArtifactVersion> usages = acc.propertyUsages.get(propertyName);
                if (usages != null) {
                    for (ResolvedGroupArtifactVersion usingGav : usages) {
                        if (isAncestorOrSelf(currentGav, usingGav)) {
                            return false;
                        }
                    }
                }

                // Only remove if this POM (or an ancestor in the project) has <release> configured
                if (acc.pomsWithRelease.contains(currentGav)) {
                    return true;
                }
                // Check if any module in the project that uses this POM as ancestor has release
                for (ResolvedGroupArtifactVersion releaseGav : acc.pomsWithRelease) {
                    if (isAncestorOrSelf(currentGav, releaseGav)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean isAncestorOrSelf(ResolvedGroupArtifactVersion possibleAncestor, ResolvedGroupArtifactVersion gav) {
                if (possibleAncestor.equals(gav)) {
                    return true;
                }
                // Walk up from the current resolution result to check ancestor chain
                MavenResolutionResult ancestor = getResolutionResult();
                while (ancestor != null) {
                    if (ancestor.getPom().getGav().equals(possibleAncestor)) {
                        return true;
                    }
                    ancestor = ancestor.getParent();
                }
                return false;
            }
        };
    }

    private boolean currentNewerThanProposed(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> config) {
        if (!config.isPresent()) {
            return false;
        }
        try {
            float currentVersion = Float.parseFloat(config.get());
            float proposedVersion = Float.parseFloat(releaseVersion.toString());
            return proposedVersion < currentVersion;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static @Nullable String getExistingPropertyReference(
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String>... configs) {
        for (Optional<String> config : configs) {
            if (config.isPresent()) {
                String value = config.get();
                if (value.startsWith("${") && value.endsWith("}") && !isDefaultMavenCompilerProperty(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private static boolean isDefaultMavenCompilerProperty(String value) {
        return "${maven.compiler.source}".equals(value) ||
               "${maven.compiler.target}".equals(value) ||
               "${maven.compiler.release}".equals(value);
    }

    private boolean hasJavaVersionProperty(Xml.Document xml) {
        return xml.getMarkers().findFirst(MavenResolutionResult.class)
                .map(r -> r.getPom().getProperties().get("java.version") != null)
                .orElse(false);
    }
}
