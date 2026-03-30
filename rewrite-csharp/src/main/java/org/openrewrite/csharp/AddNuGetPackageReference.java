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
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

/**
 * Adds a NuGet PackageReference to .csproj files if not already present.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddNuGetPackageReference extends Recipe {

    @Option(displayName = "Package name",
            description = "The NuGet package name to add.",
            example = "Newtonsoft.Json")
    String packageName;

    @Option(displayName = "Version",
            description = "The package version to add. If omitted, no Version attribute is set " +
                          "(useful when versions are managed centrally via Directory.Packages.props).",
            example = "13.0.3",
            required = false)
    @Nullable
    String version;

    @Override
    public String getDisplayName() {
        return "Add NuGet package reference";
    }

    @Override
    public String getDescription() {
        return "Adds a `<PackageReference>` element to .csproj files if not already present.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindMSBuildProject(),
                new XmlIsoVisitor<ExecutionContext>() {
                    private boolean alreadyPresent;
                    private Xml.@Nullable Tag lastItemGroup;

                    @Override
                    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                        alreadyPresent = false;
                        lastItemGroup = null;

                        // Check if already present via marker
                        document.getMarkers().findFirst(MSBuildProject.class).ifPresent(marker -> {
                            for (MSBuildProject.TargetFramework tfm : marker.getTargetFrameworks()) {
                                for (MSBuildProject.PackageReference ref : tfm.getPackageReferences()) {
                                    if (packageName.equals(ref.getInclude())) {
                                        alreadyPresent = true;
                                        return;
                                    }
                                }
                            }
                        });

                        if (alreadyPresent) {
                            return document;
                        }

                        Xml.Document d = super.visitDocument(document, ctx);

                        // Re-check after visiting tags (catches second cycle)
                        if (alreadyPresent) {
                            return d;
                        }

                        // Build the new PackageReference tag
                        @Language("xml")
                        String tag = version != null ? String.format(
                                "<PackageReference Include=\"%s\" Version=\"%s\" />",
                                packageName, version) : String.format(
                                "<PackageReference Include=\"%s\" />",
                                packageName);
                        Xml.Tag newRef = Xml.Tag.build(tag);

                        if (lastItemGroup != null) {
                            // Add to the last ItemGroup that contains PackageReferences
                            doAfterVisit(new AddToTagVisitor<>(lastItemGroup, newRef));
                        } else {
                            // No ItemGroup with PackageReferences exists — add one to the Project root
                            Xml.Tag itemGroup = Xml.Tag.build(
                                    "<ItemGroup>\n    " + tag + "\n  </ItemGroup>");
                            doAfterVisit(new AddToTagVisitor<>(d.getRoot(), itemGroup));
                        }

                        return d;
                    }

                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = super.visitTag(tag, ctx);

                        // Also check XML directly for idempotency (marker is static)
                        if ("PackageReference".equals(t.getName())) {
                            String include = t.getAttributes().stream()
                                    .filter(a -> "Include".equalsIgnoreCase(a.getKeyAsString()))
                                    .map(a -> a.getValue().getValue())
                                    .findFirst()
                                    .orElse(null);
                            if (packageName.equals(include)) {
                                alreadyPresent = true;
                            }
                        }

                        // Track the last ItemGroup that contains PackageReference elements
                        if ("ItemGroup".equals(t.getName())) {
                            boolean hasPackageRef = t.getChildren().stream()
                                    .anyMatch(c -> "PackageReference".equals(c.getName()));
                            if (hasPackageRef) {
                                lastItemGroup = t;
                            }
                        }

                        return t;
                    }
                }
        );
    }
}
