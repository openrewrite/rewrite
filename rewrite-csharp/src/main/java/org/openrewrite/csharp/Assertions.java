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

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.csharp.rpc.CSharpRewriteRpc;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.openrewrite.test.SourceSpecs.dir;

public class Assertions {

    private Assertions() {
    }

    // ---- C# source files ----

    public static SourceSpecs csharp(@Nullable String before) {
        return csharp(before, s -> {
        });
    }

    public static SourceSpecs csharp(@Nullable String before, Consumer<SourceSpec<Cs.CompilationUnit>> spec) {
        SourceSpec<Cs.CompilationUnit> cs = new SourceSpec<>(
                Cs.CompilationUnit.class, null, CSharpParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        cs.path(System.nanoTime() + ".cs");
        spec.accept(cs);
        Consumer<Cs.CompilationUnit> userAfterRecipe = cs.getAfterRecipe();
        cs.afterRecipe(cu -> {
            try {
                userAfterRecipe.accept(cu);
            } finally {
                CSharpRewriteRpc.shutdownCurrent();
            }
        });
        return cs;
    }

    public static SourceSpecs csharp(@Nullable String before, @Nullable String after) {
        return csharp(before, after, s -> {
        });
    }

    public static SourceSpecs csharp(@Nullable String before, @Nullable String after,
                                     Consumer<SourceSpec<Cs.CompilationUnit>> spec) {
        SourceSpec<Cs.CompilationUnit> cs = new SourceSpec<>(
                Cs.CompilationUnit.class, null, CSharpParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        cs.path(System.nanoTime() + ".cs");
        cs.after(s -> after);
        spec.accept(cs);
        Consumer<Cs.CompilationUnit> userAfterRecipe = cs.getAfterRecipe();
        cs.afterRecipe(cu -> {
            try {
                userAfterRecipe.accept(cu);
            } finally {
                CSharpRewriteRpc.shutdownCurrent();
            }
        });
        return cs;
    }

    // ---- .csproj files (auto-attach MSBuildProject marker) ----

    public static SourceSpecs csproj(@Nullable @Language("xml") String before) {
        return csproj(before, s -> {
        });
    }

    public static SourceSpecs csproj(@Nullable @Language("xml") String before, Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> xml = new SourceSpec<>(
                Xml.Document.class, null, CsprojParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        xml.path("project.csproj");
        spec.accept(xml);
        Consumer<Xml.Document> userAfterRecipe = xml.getAfterRecipe();
        xml.afterRecipe(doc -> {
            try {
                userAfterRecipe.accept(doc);
            } finally {
                CSharpRewriteRpc.shutdownCurrent();
            }
        });
        return xml;
    }

    public static SourceSpecs csproj(@Nullable @Language("xml") String before, @Nullable @Language("xml") String after) {
        return csproj(before, after, s -> {
        });
    }

    public static SourceSpecs csproj(@Nullable @Language("xml") String before, @Nullable @Language("xml") String after,
                                     Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> xml = new SourceSpec<>(
                Xml.Document.class, null, CsprojParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                ctx -> {
                }
        );
        xml.path("project.csproj");
        xml.after(s -> after);
        spec.accept(xml);
        Consumer<Xml.Document> userAfterRecipe = xml.getAfterRecipe();
        xml.afterRecipe(doc -> {
            try {
                userAfterRecipe.accept(doc);
            } finally {
                CSharpRewriteRpc.shutdownCurrent();
            }
        });
        return xml;
    }

    // ---- Project wrapper (analogous to mavenProject) ----

    /**
     * Wraps child source specs in a project directory, similar to
     * {@code mavenProject()} in rewrite-java's Assertions.
     * <p>
     * Usage:
     * <pre>
     * dotnetProject("MyApp",
     *     csproj("..."),
     *     csharp("...")
     * )
     * </pre>
     */
    public static SourceSpecs dotnetProject(String project, SourceSpecs... sources) {
        return dir(project, spec -> {
        }, sources);
    }

    public static SourceSpecs dotnetProject(String project, Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... sources) {
        return dir(project, spec, sources);
    }

    // ---- MSBuildProject marker extraction from XML ----

    /**
     * Extracts MSBuild metadata from the parsed Xml.Document by walking the XML tags.
     * Returns an MSBuildProject marker with declared metadata (SDK, TFMs,
     * package references, project references) without needing the C# RPC.
     * Used as a fallback when the RPC server is not available.
     */
    static @Nullable MSBuildProject extractMSBuildProjectFromXml(Xml.Document doc) {
        Xml.Tag root = doc.getRoot();
        String sdk = root.getAttributes().stream()
                .filter(a -> "Sdk".equalsIgnoreCase(a.getKeyAsString()))
                .map(a -> a.getValue().getValue())
                .findFirst()
                .orElse(null);

        // Collect target frameworks
        List<String> targetFrameworks = new ArrayList<>();
        findTagValue(root, "TargetFramework").ifPresent(targetFrameworks::add);
        findTagValue(root, "TargetFrameworks").ifPresent(value -> {
            for (String tfm : value.split(";")) {
                String trimmed = tfm.trim();
                if (!trimmed.isEmpty()) {
                    targetFrameworks.add(trimmed);
                }
            }
        });
        if (targetFrameworks.isEmpty()) {
            targetFrameworks.add("net8.0"); // sensible default for tests
        }

        // Collect package references and project references from all ItemGroups
        List<MSBuildProject.PackageReference> packageRefs = new ArrayList<>();
        List<MSBuildProject.ProjectReference> projectRefs = new ArrayList<>();
        collectReferences(root, packageRefs, projectRefs);

        // Collect properties from PropertyGroup elements
        Map<String, MSBuildProject.PropertyValue> properties = new LinkedHashMap<>();
        collectProperties(root, doc.getSourcePath(), properties);

        // Build per-TFM metadata (in test context, all TFMs share the same declared refs)
        List<MSBuildProject.TargetFramework> tfmList = new ArrayList<>();
        for (String tfm : targetFrameworks) {
            tfmList.add(MSBuildProject.TargetFramework.builder()
                    .targetFramework(tfm)
                    .packageReferences(packageRefs)
                    .projectReferences(projectRefs)
                    .build());
        }

        return MSBuildProject.builder()
                .sdk(sdk)
                .properties(properties)
                .targetFrameworks(tfmList)
                .build();
    }

    private static Optional<String> findTagValue(Xml.Tag parent, String tagName) {
        if (parent.getContent() == null) {
            return Optional.empty();
        }
        for (Content content : parent.getContent()) {
            if (content instanceof Xml.Tag) {
                Xml.Tag child = (Xml.Tag) content;
                if (tagName.equals(child.getName())) {
                    return child.getValue();
                }
                // Search in PropertyGroup children
                if ("PropertyGroup".equals(child.getName())) {
                    Optional<String> nested = findTagValue(child, tagName);
                    if (nested.isPresent()) {
                        return nested;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static void collectReferences(Xml.Tag parent,
                                           List<MSBuildProject.PackageReference> packageRefs,
                                           List<MSBuildProject.ProjectReference> projectRefs) {
        if (parent.getContent() == null) {
            return;
        }
        for (Content content : parent.getContent()) {
            if (!(content instanceof Xml.Tag)) {
                continue;
            }
            Xml.Tag tag = (Xml.Tag) content;
            if ("PackageReference".equals(tag.getName())) {
                String include = getAttr(tag, "Include");
                String version = getAttr(tag, "Version");
                if (include != null) {
                    packageRefs.add(new MSBuildProject.PackageReference(include, version, version));
                }
            } else if ("ProjectReference".equals(tag.getName())) {
                String include = getAttr(tag, "Include");
                if (include != null) {
                    projectRefs.add(new MSBuildProject.ProjectReference(include));
                }
            } else {
                // Recurse into ItemGroup, etc.
                collectReferences(tag, packageRefs, projectRefs);
            }
        }
    }

    private static void collectProperties(Xml.Tag parent, Path sourcePath,
                                            Map<String, MSBuildProject.PropertyValue> properties) {
        if (parent.getContent() == null) {
            return;
        }
        for (Content content : parent.getContent()) {
            if (!(content instanceof Xml.Tag)) {
                continue;
            }
            Xml.Tag tag = (Xml.Tag) content;
            if ("PropertyGroup".equals(tag.getName()) && tag.getContent() != null) {
                for (Content propContent : tag.getContent()) {
                    if (propContent instanceof Xml.Tag) {
                        Xml.Tag prop = (Xml.Tag) propContent;
                        String value = prop.getValue().orElse("");
                        if (!properties.containsKey(prop.getName())) {
                            properties.put(prop.getName(), new MSBuildProject.PropertyValue(value, sourcePath));
                        }
                    }
                }
            }
        }
    }

    private static @Nullable String getAttr(Xml.Tag tag, String name) {
        for (Xml.Attribute attr : tag.getAttributes()) {
            if (name.equalsIgnoreCase(attr.getKeyAsString())) {
                return attr.getValue().getValue();
            }
        }
        return null;
    }
}
