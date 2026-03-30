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
import org.openrewrite.csharp.rpc.CSharpRewriteRpc;
import org.openrewrite.csharp.rpc.ParseSolutionResult;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * A parser for .csproj files that wraps XmlParser and attaches MSBuildProject markers.
 * <p>
 * If a {@link ParseSolutionResult} is provided via the builder, uses pre-resolved
 * project metadata from a prior {@code parseSolution()} call. Otherwise, if a
 * CSharpRewriteRpc server is available (configured via {@link CSharpRewriteRpc#setFactory}),
 * uses real MSBuild evaluation to populate the marker with resolved metadata.
 * Falls back to extracting declared metadata directly from the XML structure.
 * <p>
 * This is analogous to how MavenParser automatically attaches MavenResolutionResult
 * markers to pom.xml files during parsing.
 */
public class CsprojParser implements Parser {
    private final XmlParser xmlParser;
    private final @Nullable ParseSolutionResult solutionResult;

    CsprojParser(XmlParser xmlParser, @Nullable ParseSolutionResult solutionResult) {
        this.xmlParser = xmlParser;
        this.solutionResult = solutionResult;
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return xmlParser.parseInputs(sources, relativeTo, ctx)
                .map(sourceFile -> {
                    if (sourceFile instanceof Xml.Document) {
                        Xml.Document doc = (Xml.Document) sourceFile;
                        MSBuildProject marker = resolveFromSolution(doc, relativeTo);
                        if (marker == null) {
                            marker = resolveViaRpc(doc, relativeTo, ctx);
                        }
                        if (marker == null) {
                            marker = Assertions.extractMSBuildProjectFromXml(doc);
                        }
                        if (marker != null) {
                            doc = doc.withMarkers(doc.getMarkers().add(marker));
                        }
                        return doc;
                    }
                    return sourceFile;
                });
    }

    private @Nullable MSBuildProject resolveFromSolution(Xml.Document doc, @Nullable Path relativeTo) {
        if (solutionResult == null || relativeTo == null) {
            return null;
        }
        Path absolutePath = relativeTo.resolve(doc.getSourcePath());
        MSBuildProject marker = solutionResult.getProject(absolutePath.toString());
        if (marker == null) {
            marker = solutionResult.getProject(doc.getSourcePath().toString());
        }
        return marker;
    }

    private @Nullable MSBuildProject resolveViaRpc(Xml.Document doc, @Nullable Path relativeTo, ExecutionContext ctx) {
        if (relativeTo == null) {
            return null;
        }
        try {
            Path csprojPath = relativeTo.resolve(doc.getSourcePath());
            if (!Files.exists(csprojPath)) {
                return null;
            }
            CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();
            ParseSolutionResult result = rpc.parseSolution(csprojPath, relativeTo, ctx);
            if (!result.projects().isEmpty()) {
                return result.projects().get(0);
            }
        } catch (Exception e) {
            // Fall back to XML extraction
        }
        return null;
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
        private @Nullable ParseSolutionResult solutionResult;

        Builder() {
            super(Xml.Document.class);
        }

        public Builder solutionResult(ParseSolutionResult result) {
            this.solutionResult = result;
            return this;
        }

        @Override
        public CsprojParser build() {
            return new CsprojParser(XmlParser.builder().build(), solutionResult);
        }

        @Override
        public String getDslName() {
            return "xml";
        }
    }
}
