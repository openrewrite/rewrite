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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

/**
 * A parser for .csproj files that parses them as XML and attaches
 * an MSBuildProject marker with metadata extracted from the XML structure.
 */
public class CsprojParser implements Parser {
    private final XmlParser xmlParser;

    CsprojParser(XmlParser xmlParser) {
        this.xmlParser = xmlParser;
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return xmlParser.parseInputs(sources, relativeTo, ctx)
                .map(sf -> {
                    if (sf instanceof Xml.Document) {
                        Xml.Document doc = (Xml.Document) sf;
                        MSBuildProject marker = createMarkerFromXml(doc);
                        if (marker != null) {
                            return (SourceFile) doc.withMarkers(doc.getMarkers().add(marker));
                        }
                    }
                    return sf;
                });
    }

    /**
     * Creates an MSBuildProject marker by extracting metadata from the XML structure.
     * Extracts the Sdk attribute, target frameworks, and package references.
     */
    static @Nullable MSBuildProject createMarkerFromXml(Xml.Document doc) {
        Xml.Tag root = doc.getRoot();
        if (root == null) {
            return null;
        }

        // Extract Sdk attribute from <Project Sdk="...">
        String sdk = getAttributeValue(root, "Sdk");

        // Extract target frameworks and package references
        List<MSBuildProject.TargetFramework> targetFrameworks = extractTargetFrameworks(root);

        return MSBuildProject.builder()
                .id(randomId())
                .sdk(sdk)
                .targetFrameworks(targetFrameworks)
                .build();
    }

    private static List<MSBuildProject.TargetFramework> extractTargetFrameworks(Xml.Tag root) {
        // Find TargetFramework or TargetFrameworks in PropertyGroup
        String tfmValue = null;
        String tfmsValue = null;

        for (Xml.Tag propertyGroup : findChildTags(root, "PropertyGroup")) {
            for (Xml.Tag child : findChildTags(propertyGroup, "TargetFramework")) {
                tfmValue = getTagTextValue(child);
            }
            for (Xml.Tag child : findChildTags(propertyGroup, "TargetFrameworks")) {
                tfmsValue = getTagTextValue(child);
            }
        }

        List<String> tfmNames = new ArrayList<>();
        if (tfmsValue != null) {
            for (String tfm : tfmsValue.split(";")) {
                String trimmed = tfm.trim();
                if (!trimmed.isEmpty()) {
                    tfmNames.add(trimmed);
                }
            }
        } else if (tfmValue != null) {
            tfmNames.add(tfmValue.trim());
        }

        if (tfmNames.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract package references from ItemGroup
        List<MSBuildProject.PackageReference> packageReferences = new ArrayList<>();
        for (Xml.Tag itemGroup : findChildTags(root, "ItemGroup")) {
            for (Xml.Tag pkgRef : findChildTags(itemGroup, "PackageReference")) {
                String include = getAttributeValue(pkgRef, "Include");
                if (include != null) {
                    String version = getAttributeValue(pkgRef, "Version");
                    packageReferences.add(MSBuildProject.PackageReference.builder()
                            .include(include)
                            .requestedVersion(version)
                            .build());
                }
            }
        }

        // Create a TargetFramework entry for each TFM with the same package references
        List<MSBuildProject.TargetFramework> result = new ArrayList<>();
        for (String tfm : tfmNames) {
            result.add(MSBuildProject.TargetFramework.builder()
                    .targetFramework(tfm)
                    .packageReferences(packageReferences)
                    .build());
        }
        return result;
    }

    private static List<Xml.Tag> findChildTags(Xml.Tag parent, String name) {
        List<Xml.Tag> result = new ArrayList<>();
        if (parent.getContent() != null) {
            for (Content content : parent.getContent()) {
                if (content instanceof Xml.Tag && name.equals(((Xml.Tag) content).getName())) {
                    result.add((Xml.Tag) content);
                }
            }
        }
        return result;
    }

    private static @Nullable String getAttributeValue(Xml.Tag tag, String name) {
        if (tag.getAttributes() != null) {
            for (Xml.Attribute attr : tag.getAttributes()) {
                if (name.equals(attr.getKeyAsString())) {
                    return attr.getValueAsString();
                }
            }
        }
        return null;
    }

    private static @Nullable String getTagTextValue(Xml.Tag tag) {
        if (tag.getContent() != null) {
            for (Content content : tag.getContent()) {
                if (content instanceof Xml.CharData) {
                    return ((Xml.CharData) content).getText().trim();
                }
            }
        }
        return tag.getValue().orElse(null);
    }

    @Override
    public boolean accept(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".csproj") || name.endsWith(".vbproj") || name.endsWith(".fsproj");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("project.csproj");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        Builder() {
            super(Xml.Document.class);
        }

        @Override
        public CsprojParser build() {
            return new CsprojParser(XmlParser.builder().build());
        }

        @Override
        public String getDslName() {
            return "xml";
        }
    }
}
