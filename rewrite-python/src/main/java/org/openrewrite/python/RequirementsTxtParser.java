/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.python;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.internal.PythonDependencyParser;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.PackageManager;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

/**
 * Parser for requirements.txt files that delegates to {@link PlainTextParser} and attaches a
 * {@link PythonResolutionResult} marker with dependency metadata.
 */
public class RequirementsTxtParser implements Parser {

    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "requirements(-[\\w-]+)?\\.(txt|in)"
    );

    private static final Set<String> OPTION_PREFIXES = new LinkedHashSet<>(Arrays.asList(
            "-i", "--index-url",
            "-r", "--requirement",
            "--extra-index-url",
            "-e", "--editable",
            "-f", "--find-links",
            "--no-binary", "--only-binary",
            "--pre",
            "--trusted-host",
            "--no-deps",
            "-c", "--constraint"
    ));

    private final PlainTextParser plainTextParser = new PlainTextParser();

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return plainTextParser.parseInputs(sources, relativeTo, ctx).map(sf -> {
            if (!(sf instanceof PlainText)) {
                return sf;
            }
            PlainText text = (PlainText) sf;
            List<Dependency> deps = parseDependencies(text.getText());
            if (deps.isEmpty()) {
                return sf;
            }

            List<ResolvedDependency> resolvedDeps = Collections.emptyList();
            @Nullable PackageManager packageManager = PackageManager.Pip;

            // Try to resolve via uv pip freeze
            @Nullable Path originalFilePath = null;
            if (relativeTo != null) {
                originalFilePath = relativeTo.resolve(text.getSourcePath());
            }
            Path workspace = DependencyWorkspace.getOrCreateRequirementsWorkspace(
                    text.getText(), originalFilePath);
            if (workspace != null) {
                resolvedDeps = parseFreezeOutput(workspace);
                if (!resolvedDeps.isEmpty()) {
                    packageManager = PackageManager.Uv;
                }
            }

            PythonResolutionResult marker = new PythonResolutionResult(
                    randomId(),
                    null,
                    null,
                    null,
                    null,
                    text.getSourcePath().toString(),
                    null,
                    null,
                    Collections.emptyList(),
                    deps,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    resolvedDeps,
                    packageManager,
                    null
            );

            // Link declared deps to resolved deps by normalized name
            if (!resolvedDeps.isEmpty()) {
                marker = marker.withDependencies(linkResolved(deps, resolvedDeps));
            }

            return text.withMarkers(text.getMarkers().addIfAbsent(marker));
        });
    }

    static List<Dependency> parseDependencies(String text) {
        List<Dependency> deps = new ArrayList<>();
        String[] rawLines = text.split("\n", -1);

        // Handle line continuations
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String rawLine : rawLines) {
            if (rawLine.endsWith("\\")) {
                current.append(rawLine, 0, rawLine.length() - 1);
            } else {
                current.append(rawLine);
                lines.add(current.toString());
                current = new StringBuilder();
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (isOptionLine(trimmed)) {
                continue;
            }
            // Strip inline comments
            int commentIdx = trimmed.indexOf(" #");
            if (commentIdx >= 0) {
                trimmed = trimmed.substring(0, commentIdx).trim();
            }
            Dependency dep = PythonDependencyParser.parsePep508(trimmed);
            if (dep != null) {
                deps.add(dep);
            }
        }
        return deps;
    }

    private static boolean isOptionLine(String line) {
        for (String prefix : OPTION_PREFIXES) {
            if (line.startsWith(prefix + " ") || line.startsWith(prefix + "=") || line.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    static List<ResolvedDependency> parseFreezeOutput(Path workspace) {
        String freezeContent = DependencyWorkspace.readFreezeOutput(workspace);
        return parseFreezeLines(freezeContent);
    }

    static List<ResolvedDependency> parseFreezeLines(String freezeContent) {
        List<ResolvedDependency> resolved = new ArrayList<>();
        for (String line : freezeContent.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eqIdx = trimmed.indexOf("==");
            if (eqIdx > 0) {
                String name = trimmed.substring(0, eqIdx).trim();
                String version = trimmed.substring(eqIdx + 2).trim();
                resolved.add(new ResolvedDependency(name, version, null, null));
            }
        }
        return resolved;
    }

    private List<Dependency> linkResolved(List<Dependency> deps, List<ResolvedDependency> resolved) {
        return deps.stream().map(dep -> {
            String normalizedName = PythonResolutionResult.normalizeName(dep.getName());
            ResolvedDependency found = resolved.stream()
                    .filter(r -> PythonResolutionResult.normalizeName(r.getName()).equals(normalizedName))
                    .findFirst()
                    .orElse(null);
            return found != null ? dep.withResolved(found) : dep;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean accept(Path path) {
        String filename = path.getFileName().toString();
        return FILENAME_PATTERN.matcher(filename).matches();
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("requirements.txt");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        Builder() {
            super(PlainText.class);
        }

        @Override
        public RequirementsTxtParser build() {
            return new RequirementsTxtParser();
        }

        @Override
        public String getDslName() {
            return "requirements.txt";
        }
    }
}
