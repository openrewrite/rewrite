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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.csharp.marker.MSBuildProject;
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
 * Handles three version definition patterns:
 * <pre>
 * 1. Literal version in .csproj:
 *    &lt;PackageReference Include="Foo" Version="1.0.0" /&gt;
 *    → Update the Version attribute directly
 *
 * 2. Property reference in .csproj:
 *    &lt;PackageReference Include="Foo" Version="$(FooVersion)" /&gt;
 *    → Find and update the &lt;FooVersion&gt; property
 *
 * 3. Central package management (Directory.Packages.props):
 *    &lt;PackageVersion Include="Foo" Version="1.0.0" /&gt;
 *    → Update the Version in the .props file
 * </pre>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeNuGetPackageVersion extends ScanningRecipe<UpgradeNuGetPackageVersion.Accumulator> {

    @Option(displayName = "Package name",
            description = "The NuGet package name to upgrade. Supports glob patterns.",
            example = "Newtonsoft.Json")
    String packageName;

    @Option(displayName = "New version",
            description = "The version to upgrade to.",
            example = "14.0.1")
    String newVersion;

    @Override
    public String getDisplayName() {
        return "Upgrade NuGet package version";
    }

    @Override
    public String getDescription() {
        return "Upgrades the version of a NuGet `<PackageReference>` or `<PackageVersion>` in .csproj " +
               "and Directory.Packages.props files. Handles property references by updating the property " +
               "value instead of the version attribute.";
    }

    static class Accumulator {
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
                            String requested = ref.getRequestedVersion();
                            if (requested != null && isPropertyReference(requested)) {
                                // Version uses a property — record the property update
                                String propertyName = extractPropertyName(requested);
                                MSBuildProject.PropertyValue pv = marker.getProperties().get(propertyName);
                                Path definedIn = pv != null && pv.getDefinedIn() != null
                                        ? pv.getDefinedIn()
                                        : sourcePath;
                                acc.propertyUpdates
                                        .computeIfAbsent(definedIn, k -> new LinkedHashMap<>())
                                        .put(propertyName, newVersion);
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
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                // Handle <PackageReference Include="..." Version="..." />
                if ("PackageReference".equals(t.getName())) {
                    String include = getAttributeValue(t, "Include");
                    if (include != null && matchesGlob(include, packageName)) {
                        String versionAttr = getAttributeValue(t, "Version");
                        if (versionAttr != null && !isPropertyReference(versionAttr)
                                && !newVersion.equals(versionAttr)) {
                            t = changeAttribute(t, "Version", newVersion);
                        }

                        // Handle <PackageReference Include="..."><Version>...</Version></PackageReference>
                        if (versionAttr == null) {
                            t.getChild("Version").ifPresent(versionTag -> {
                                String currentValue = versionTag.getValue().orElse("");
                                if (!newVersion.equals(currentValue)) {
                                    doAfterVisit(new ChangeTagValueVisitor<>(versionTag, newVersion));
                                }
                            });
                        }
                    }
                }

                // Handle <PackageVersion Include="..." Version="..." /> (central package management)
                if ("PackageVersion".equals(t.getName())) {
                    String include = getAttributeValue(t, "Include");
                    if (include != null && matchesGlob(include, packageName)) {
                        String versionAttr = getAttributeValue(t, "Version");
                        if (versionAttr != null && !newVersion.equals(versionAttr)) {
                            t = changeAttribute(t, "Version", newVersion);
                        }
                    }
                }

                // Handle property definitions (e.g., <FooVersion>1.0.0</FooVersion>)
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
