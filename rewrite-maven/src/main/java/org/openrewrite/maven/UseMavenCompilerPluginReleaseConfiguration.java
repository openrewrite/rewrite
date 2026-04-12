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
            Pattern.compile("\\$\\{(maven\\.compiler\\.(?:source|target|testSource|testTarget))}");

    private static final Set<String> DEFAULT_MAVEN_COMPILER_PROPERTIES = new HashSet<>(Arrays.asList(
            "${maven.compiler.source}", "${maven.compiler.target}", "${maven.compiler.release}",
            "${maven.compiler.testSource}", "${maven.compiler.testTarget}", "${maven.compiler.testRelease}"
    ));

    private static final Set<String> COMPILER_SOURCE_TARGET_TAG_NAMES = new HashSet<>(Arrays.asList(
            "source", "target", "testSource", "testTarget"
    ));

    @Option(
            displayName = "Release version",
            description = "The new value for the release configuration. This recipe prefers ${java.version} if defined.",
            example = "11"
    )
    Integer releaseVersion;

    String displayName = "Use Maven compiler plugin release configuration";

    String description = "Replaces any explicit `source` or `target` configuration (if present) on the `maven-compiler-plugin` with " +
                "`release`, and updates the `release` value if needed. When `testSource` or `testTarget` differ from the main " +
                "version, introduces `testRelease`. Will not downgrade the Java version if the current version is higher. " +
                "Also removes stale `maven.compiler.source`, `maven.compiler.target`, `maven.compiler.testSource`, and " +
                "`maven.compiler.testTarget` properties that are no longer referenced.";

    public static class Accumulator {
        // Only tracks usages from OUTSIDE the compiler plugin's source/target/testSource/testTarget tags,
        // since those tags will be replaced by the visitor
        Map<String, Set<ResolvedGroupArtifactVersion>> propertyUsages = new HashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            private boolean insideCompilerPlugin;

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                boolean wasInsideCompilerPlugin = insideCompilerPlugin;
                if (isPluginTag("org.apache.maven.plugins", "maven-compiler-plugin")) {
                    insideCompilerPlugin = true;
                }

                Xml.Tag t = super.visitTag(tag, ctx);

                // Track ${maven.compiler.*} property usages outside of <properties>,
                // but skip usages from compiler plugin source/target tags (those will be replaced)
                if (!isPropertyTag()) {
                    Optional<String> value = t.getValue();
                    if (value.isPresent()) {
                        boolean isCompilerSourceTargetTag = insideCompilerPlugin &&
                                COMPILER_SOURCE_TARGET_TAG_NAMES.contains(t.getName());
                        if (!isCompilerSourceTargetTag) {
                            Matcher matcher = MAVEN_COMPILER_PROPERTY_PATTERN.matcher(value.get());
                            while (matcher.find()) {
                                acc.propertyUsages.computeIfAbsent(matcher.group(1), k -> new HashSet<>())
                                        .add(getResolutionResult().getPom().getGav());
                            }
                        }
                    }
                }

                insideCompilerPlugin = wasInsideCompilerPlugin;
                return t;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            private final Set<String> propertiesToRemove = new HashSet<>();

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                propertiesToRemove.clear();
                Xml.Document d = super.visitDocument(document, ctx);

                // Schedule property removal as doAfterVisit so it runs in the same cycle,
                // even though property tags appear before the compiler plugin in the POM
                if (!propertiesToRemove.isEmpty()) {
                    Set<String> toRemove = new HashSet<>(propertiesToRemove);
                    doAfterVisit(new MavenIsoVisitor<ExecutionContext>() {
                        @Override
                        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                            Xml.Tag t = super.visitTag(tag, ctx);
                            if (isPropertyTag() && toRemove.contains(t.getName())) {
                                doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                                maybeUpdateModel();
                            }
                            return t;
                        }
                    });
                }

                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                // Handle compiler plugin source/target → release replacement
                if (isPluginTag("org.apache.maven.plugins", "maven-compiler-plugin")) {
                    t = handleCompilerPlugin(t);
                }

                return t;
            }

            private Xml.Tag handleCompilerPlugin(Xml.Tag t) {
                Optional<Xml.Tag> maybeConfig = t.getChild("configuration");
                if (!maybeConfig.isPresent()) {
                    return t;
                }
                Xml.Tag config = maybeConfig.get();
                Optional<String> source = config.getChildValue("source");
                Optional<String> target = config.getChildValue("target");
                Optional<String> release = config.getChildValue("release");
                Optional<String> testSource = config.getChildValue("testSource");
                Optional<String> testTarget = config.getChildValue("testTarget");
                Optional<String> testRelease = config.getChildValue("testRelease");

                boolean hasMainConfig = source.isPresent() || target.isPresent() || release.isPresent();
                boolean hasTestConfig = testSource.isPresent() || testTarget.isPresent() || testRelease.isPresent();

                if (!hasMainConfig && !hasTestConfig) {
                    return t;
                }

                // Determine whether to process main source/target → release
                boolean processMain = hasMainConfig &&
                        !versionNewerThanProposed(source) &&
                        !versionNewerThanProposed(target) &&
                        !versionNewerThanProposed(release);

                // Determine test handling: whether testRelease is needed after removing testSource/testTarget
                boolean testNeedsOwnRelease = false;
                @Nullable String testVersionValue = null;
                if (hasTestConfig) {
                    testVersionValue = resolveVersion(testRelease, testSource, testTarget);
                    if (testVersionValue != null) {
                        if (DEFAULT_MAVEN_COMPILER_PROPERTIES.contains("${" + extractPropertyName(testVersionValue) + "}")) {
                            testNeedsOwnRelease = false;
                        } else if (testVersionValue.startsWith("${")) {
                            testNeedsOwnRelease = true;
                        } else {
                            testNeedsOwnRelease = isHigherVersion(testVersionValue, releaseVersion.toString());
                        }
                    }
                }

                if (!processMain && !hasTestConfig) {
                    return t;
                }

                // Build the set of tags to remove from configuration
                Set<String> tagsToRemove = new HashSet<>();
                if (processMain) {
                    tagsToRemove.add("source");
                    tagsToRemove.add("target");
                }
                if (hasTestConfig) {
                    tagsToRemove.add("testSource");
                    tagsToRemove.add("testTarget");
                    if (!testNeedsOwnRelease) {
                        tagsToRemove.add("testRelease");
                    }
                }

                if (tagsToRemove.isEmpty()) {
                    return t;
                }

                Xml.Tag updated = filterTagChildren(t, config,
                        child -> !tagsToRemove.contains(child.getName()));

                // Add/update <release>
                if (processMain) {
                    String existingPropertyRef = getExistingPropertyReference(release, source, target);
                    String releaseVal;
                    if (existingPropertyRef != null) {
                        releaseVal = existingPropertyRef;
                    } else if (hasJavaVersionProperty(getCursor().firstEnclosingOrThrow(Xml.Document.class))) {
                        releaseVal = "${java.version}";
                    } else {
                        releaseVal = releaseVersion.toString();
                    }
                    updated = addOrUpdateChild(updated, config,
                            Xml.Tag.build("<release>" + releaseVal + "</release>"), getCursor().getParentOrThrow());
                }

                // Add/update <testRelease> if test version is higher than proposed release
                if (hasTestConfig && testNeedsOwnRelease && testVersionValue != null) {
                    String testExistingRef = getExistingPropertyReference(testRelease, testSource, testTarget);
                    String testReleaseVal = testExistingRef != null ? testExistingRef : testVersionValue;
                    updated = addOrUpdateChild(updated, config,
                            Xml.Tag.build("<testRelease>" + testReleaseVal + "</testRelease>"), getCursor().getParentOrThrow());
                }

                // Determine which maven.compiler.* properties are now stale and should be removed
                boolean releaseConfigured = processMain || release.isPresent();
                boolean testReleaseConfigured = (hasTestConfig && testNeedsOwnRelease) || testRelease.isPresent();

                if (releaseConfigured) {
                    markPropertyForRemovalIfUnused("maven.compiler.source", acc);
                    markPropertyForRemovalIfUnused("maven.compiler.target", acc);
                }
                if (releaseConfigured || testReleaseConfigured) {
                    markPropertyForRemovalIfUnused("maven.compiler.testSource", acc);
                    markPropertyForRemovalIfUnused("maven.compiler.testTarget", acc);
                }

                return updated;
            }

            private void markPropertyForRemovalIfUnused(String propertyName, Accumulator acc) {
                ResolvedGroupArtifactVersion currentGav = getResolutionResult().getPom().getGav();

                Set<ResolvedGroupArtifactVersion> usages = acc.propertyUsages.get(propertyName);
                if (usages != null) {
                    for (ResolvedGroupArtifactVersion usingGav : usages) {
                        if (isAncestorOrSelf(currentGav, usingGav)) {
                            return;
                        }
                    }
                }

                propertiesToRemove.add(propertyName);
            }

            private boolean isAncestorOrSelf(ResolvedGroupArtifactVersion possibleAncestor, ResolvedGroupArtifactVersion gav) {
                if (possibleAncestor.equals(gav)) {
                    return true;
                }
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

    private boolean versionNewerThanProposed(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> config) {
        if (!config.isPresent()) {
            return false;
        }
        return isHigherVersion(config.get(), releaseVersion.toString());
    }

    private static boolean isHigherVersion(String current, String proposed) {
        try {
            return Float.parseFloat(current) > Float.parseFloat(proposed);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static @Nullable String resolveVersion(
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String>... configs) {
        for (Optional<String> config : configs) {
            if (config.isPresent()) {
                return config.get();
            }
        }
        return null;
    }

    private static @Nullable String getExistingPropertyReference(
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String>... configs) {
        for (Optional<String> config : configs) {
            if (config.isPresent()) {
                String value = config.get();
                if (value.startsWith("${") && value.endsWith("}") && !DEFAULT_MAVEN_COMPILER_PROPERTIES.contains(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Extracts the property name from a value that may or may not be a property reference.
     * For "${foo.bar}" returns "foo.bar", for "11" returns "11".
     */
    private static String extractPropertyName(String value) {
        if (value.startsWith("${") && value.endsWith("}")) {
            return value.substring(2, value.length() - 1);
        }
        return value;
    }

    private boolean hasJavaVersionProperty(Xml.Document xml) {
        return xml.getMarkers().findFirst(MavenResolutionResult.class)
                .map(r -> r.getPom().getProperties().get("java.version") != null)
                .orElse(false);
    }
}
