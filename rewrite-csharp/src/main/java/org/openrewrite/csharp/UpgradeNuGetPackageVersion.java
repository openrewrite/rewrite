/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * Upgrades a NuGet package reference version in .csproj files.
 * <p>
 * Supports the same version selectors as Maven and Gradle upgrade recipes:
 * exact versions, {@code latest.release}, {@code latest.patch},
 * tilde ({@code ~1.2.3}), caret ({@code ^1.2.3}), and other semver selectors.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeNuGetPackageVersion extends ScanningRecipe<UpgradeNuGetPackageVersion.Accumulator> {

    @Option(displayName = "Package name",
            description = "The NuGet package name to upgrade. Supports glob patterns.",
            example = "Newtonsoft.Json")
    String packageName;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors).",
            example = "latest.release")
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. " +
                          "So for example, setting 'newVersion' to \"25-29\" can be paired with a metadata pattern " +
                          "of \"-jre\" to select version 29.0-jre.",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Upgrade NuGet package version";
    }

    @Override
    public String getDescription() {
        return "Upgrades the version of a NuGet `<PackageReference>` or `<PackageVersion>` in .csproj " +
               "and Directory.Packages.props files. Handles property references by updating the property " +
               "value instead of the version attribute. Supports semver version selectors.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(
                Semver.validate(newVersion, versionPattern));
    }

    static class Accumulator {
        /**
         * The resolved target version for each package (keyed by Include name).
         * Null if no suitable version was found.
         */
        Map<String, String> resolvedVersions = new LinkedHashMap<>();

        /**
         * Properties that need to be updated (property name to new value),
         * keyed by the source path of the file where the property is defined.
         */
        Map<Path, Map<String, String>> propertyUpdates = new LinkedHashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                document.getMarkers().findFirst(MSBuildProject.class).ifPresent(marker -> {
                    Path sourcePath = document.getSourcePath();
                    for (MSBuildProject.TargetFramework tfm : marker.getTargetFrameworks()) {
                        for (MSBuildProject.PackageReference ref : tfm.getPackageReferences()) {
                            if (!matchesGlob(ref.getInclude(), packageName)) {
                                continue;
                            }

                            String currentVersion = ref.getResolvedVersion();

                            // Resolve the target version using semver selector
                            String targetVersion = resolveTargetVersion(
                                    ref.getInclude(), currentVersion, marker, versionComparator, ctx);
                            if (targetVersion == null) {
                                continue;
                            }
                            acc.resolvedVersions.put(ref.getInclude(), targetVersion);

                            String requested = ref.getRequestedVersion();
                            if (requested != null && isPropertyReference(requested)) {
                                String propertyName = extractPropertyName(requested);
                                MSBuildProject.PropertyValue pv = marker.getProperties().get(propertyName);
                                Path definedIn = pv != null && pv.getDefinedIn() != null
                                        ? pv.getDefinedIn()
                                        : sourcePath;
                                acc.propertyUpdates
                                        .computeIfAbsent(definedIn, k -> new LinkedHashMap<>())
                                        .put(propertyName, targetVersion);
                            }
                        }
                    }
                });
                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if ("PackageReference".equals(t.getName())) {
                    String include = getAttributeValue(t, "Include");
                    String resolvedVersion = include != null ? acc.resolvedVersions.get(include) : null;
                    // Fall back to exact version when scan phase didn't populate resolved versions
                    // (e.g., when called via RPC proxy without scan phase)
                    if (resolvedVersion == null && include != null && matchesGlob(include, packageName)
                            && versionComparator instanceof org.openrewrite.semver.ExactVersion) {
                        resolvedVersion = ((org.openrewrite.semver.ExactVersion) versionComparator).getVersion();
                    }
                    String targetVersion = resolvedVersion;
                    if (targetVersion != null) {
                        String versionAttr = getAttributeValue(t, "Version");
                        if (versionAttr != null && !isPropertyReference(versionAttr)
                                && !targetVersion.equals(versionAttr)) {
                            t = changeAttribute(t, "Version", targetVersion);
                        }

                        if (versionAttr == null) {
                            t.getChild("Version").ifPresent(versionTag -> {
                                String currentValue = versionTag.getValue().orElse("");
                                if (!targetVersion.equals(currentValue)) {
                                    doAfterVisit(new ChangeTagValueVisitor<>(versionTag, targetVersion));
                                }
                            });
                        }
                    }
                }

                if ("PackageVersion".equals(t.getName())) {
                    String include = getAttributeValue(t, "Include");
                    if (include != null && matchesGlob(include, packageName)) {
                        // For PackageVersion (central pkg mgmt), use resolved version if available,
                        // otherwise fall back to exact version for ExactVersion selectors
                        String targetVersion = acc.resolvedVersions.get(include);
                        if (targetVersion == null && versionComparator instanceof org.openrewrite.semver.ExactVersion) {
                            targetVersion = ((org.openrewrite.semver.ExactVersion) versionComparator).getVersion();
                        }
                        if (targetVersion != null) {
                            String versionAttr = getAttributeValue(t, "Version");
                            if (versionAttr != null && !targetVersion.equals(versionAttr)) {
                                t = changeAttribute(t, "Version", targetVersion);
                            }
                        }
                    }
                }

                // Handle property definitions
                Path sourcePath = getCursor().firstEnclosingOrThrow(Xml.Document.class).getSourcePath();
                Map<String, String> propsForFile = acc.propertyUpdates.get(sourcePath);
                if (propsForFile != null && propsForFile.containsKey(t.getName())) {
                    String currentValue = t.getValue().orElse("");
                    String targetVersion = propsForFile.get(t.getName());
                    if (!targetVersion.equals(currentValue)) {
                        doAfterVisit(new ChangeTagValueVisitor<>(t, targetVersion));
                    }
                }

                return t;
            }
        };
    }

    /**
     * Resolve the target version for a package using the semver version comparator.
     * For exact version selectors, returns the exact version directly.
     * For selectors like latest.release, queries available versions from NuGet package sources.
     */
    private @Nullable String resolveTargetVersion(
            String packageInclude,
            @Nullable String currentVersion,
            MSBuildProject marker,
            VersionComparator versionComparator,
            ExecutionContext ctx) {

        // For exact version selectors, the comparator's isValid checks the exact version
        if (versionComparator instanceof org.openrewrite.semver.ExactVersion) {
            return ((org.openrewrite.semver.ExactVersion) versionComparator).getVersion();
        }

        // For semver selectors, we need available versions to select from.
        // Try to get them from NuGet package sources on the marker.
        List<String> availableVersions = NuGetVersionResolver.resolveAvailableVersions(
                packageInclude, marker.getPackageSources(), ctx);

        if (availableVersions.isEmpty()) {
            return null;
        }

        return versionComparator.upgrade(
                currentVersion != null ? currentVersion : "0.0.0",
                availableVersions
        ).orElse(null);
    }

    private static boolean isPropertyReference(@Nullable String value) {
        return value != null && value.startsWith("$(") && value.endsWith(")");
    }

    private static String extractPropertyName(String propertyRef) {
        return propertyRef.substring(2, propertyRef.length() - 1);
    }

    private static @Nullable String getAttributeValue(Xml.Tag tag, String attrName) {
        for (Xml.Attribute attr : tag.getAttributes()) {
            if (attrName.equalsIgnoreCase(attr.getKeyAsString())) {
                return attr.getValue().getValue();
            }
        }
        return null;
    }

    private static Xml.Tag changeAttribute(Xml.Tag tag, String attrName, String newValue) {
        List<Xml.Attribute> attrs = new ArrayList<>(tag.getAttributes());
        for (int i = 0; i < attrs.size(); i++) {
            Xml.Attribute attr = attrs.get(i);
            if (attrName.equalsIgnoreCase(attr.getKeyAsString())) {
                attrs.set(i, attr.withValue(
                        attr.getValue().withValue(newValue)));
                return tag.withAttributes(attrs);
            }
        }
        return tag;
    }
}
