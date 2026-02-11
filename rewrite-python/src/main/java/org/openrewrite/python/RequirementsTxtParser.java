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
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.PackageManager;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

/**
 * Parser for requirements.txt files that delegates to {@link PlainTextParser} and attaches a
 * {@link PythonResolutionResult} marker with dependency metadata resolved via {@code uv pip freeze}.
 */
public class RequirementsTxtParser implements Parser {

    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "requirements(-[\\w-]+)?\\.(txt|in)"
    );

    private final PlainTextParser plainTextParser = new PlainTextParser();

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return plainTextParser.parseInputs(sources, relativeTo, ctx).map(sf -> {
            if (!(sf instanceof PlainText)) {
                return sf;
            }
            PlainText text = (PlainText) sf;

            @Nullable Path originalFilePath = null;
            if (relativeTo != null) {
                originalFilePath = relativeTo.resolve(text.getSourcePath());
            }
            Path workspace = DependencyWorkspace.getOrCreateRequirementsWorkspace(
                    text.getText(), originalFilePath);
            if (workspace == null) {
                return sf;
            }

            List<ResolvedDependency> resolvedDeps = parseFreezeOutput(workspace);
            if (resolvedDeps.isEmpty()) {
                return sf;
            }

            List<Dependency> deps = dependenciesFromResolved(resolvedDeps);

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
                    PackageManager.Uv,
                    null
            );

            return text.withMarkers(text.getMarkers().addIfAbsent(marker));
        });
    }

    public static List<ResolvedDependency> parseFreezeOutput(Path workspace) {
        String freezeContent = DependencyWorkspace.readFreezeOutput(workspace);
        List<ResolvedDependency> resolved = parseFreezeLines(freezeContent);
        return linkDependenciesFromMetadata(resolved, workspace);
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

    /**
     * Convert resolved dependencies into declared (direct) dependencies.
     * When the dependency graph has been linked (via {@link #linkDependenciesFromMetadata}),
     * only root packages (those not depended on by any other package) are included.
     * When the graph is unlinked (all {@code dependencies} fields are null), all packages
     * are treated as direct so that client code traversing {@code getDependencies()} finds every package.
     */
    public static List<Dependency> dependenciesFromResolved(List<ResolvedDependency> resolved) {
        // Collect all packages that appear as a transitive dependency of another package
        Set<String> transitive = new HashSet<>();
        for (ResolvedDependency r : resolved) {
            if (r.getDependencies() != null) {
                for (ResolvedDependency dep : r.getDependencies()) {
                    transitive.add(PythonResolutionResult.normalizeName(dep.getName()));
                }
            }
        }

        List<Dependency> deps = new ArrayList<>();
        for (ResolvedDependency r : resolved) {
            if (transitive.isEmpty() || !transitive.contains(PythonResolutionResult.normalizeName(r.getName()))) {
                deps.add(new Dependency(r.getName(), "==" + r.getVersion(), null, null, r));
            }
        }
        return deps;
    }

    /**
     * Link transitive dependencies by reading installed package METADATA files from site-packages.
     * Uses a two-pass approach: first builds a name→entry map, then reads each package's
     * {@code Requires-Dist} entries to link the graph.
     *
     * @param resolved  flat list of resolved dependencies from freeze output
     * @param workspace workspace directory containing {@code .venv/}
     * @return new list with dependencies linked (or original list if site-packages not found)
     */
    static List<ResolvedDependency> linkDependenciesFromMetadata(List<ResolvedDependency> resolved, Path workspace) {
        Path sitePackages = findSitePackages(workspace);
        if (sitePackages == null) {
            return resolved;
        }

        // Pass 1: build name→entry map
        Map<String, ResolvedDependency> byNormalizedName = new LinkedHashMap<>();
        for (ResolvedDependency r : resolved) {
            byNormalizedName.put(PythonResolutionResult.normalizeName(r.getName()), r);
        }

        // Pass 2: read METADATA for each package, link dependencies
        List<ResolvedDependency> linked = new ArrayList<>(resolved.size());
        for (ResolvedDependency r : resolved) {
            List<String> requiredNames = readRequiresDist(sitePackages, r.getName(), r.getVersion());
            if (requiredNames.isEmpty()) {
                linked.add(r);
                continue;
            }

            List<ResolvedDependency> deps = new ArrayList<>();
            for (String reqName : requiredNames) {
                ResolvedDependency dep = byNormalizedName.get(PythonResolutionResult.normalizeName(reqName));
                if (dep != null) {
                    deps.add(dep);
                }
            }
            linked.add(r.withDependencies(deps.isEmpty() ? null : deps));
        }

        return linked;
    }

    private static @Nullable Path findSitePackages(Path workspace) {
        Path lib = workspace.resolve(".venv/lib");
        if (!Files.isDirectory(lib)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(lib, "python*")) {
            for (Path pythonDir : stream) {
                Path sp = pythonDir.resolve("site-packages");
                if (Files.isDirectory(sp)) {
                    return sp;
                }
            }
        } catch (IOException e) {
            // fall through
        }
        return null;
    }

    private static List<String> readRequiresDist(Path sitePackages, String packageName, String version) {
        Path metadataFile = findMetadataFile(sitePackages, packageName, version);
        if (metadataFile == null) {
            return Collections.emptyList();
        }
        try {
            String content = new String(Files.readAllBytes(metadataFile), StandardCharsets.UTF_8);
            return parseRequiresDist(content);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private static @Nullable Path findMetadataFile(Path sitePackages, String packageName, String version) {
        // Try common naming conventions: package_name-version.dist-info
        // The directory name uses the package's canonical form which may differ from freeze output
        String normalized = packageName.replace('-', '_');
        Path direct = sitePackages.resolve(normalized + "-" + version + ".dist-info/METADATA");
        if (Files.exists(direct)) {
            return direct;
        }
        // Try with original name (dashes preserved)
        direct = sitePackages.resolve(packageName + "-" + version + ".dist-info/METADATA");
        if (Files.exists(direct)) {
            return direct;
        }
        // Fallback: glob for matching dist-info directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sitePackages, "*.dist-info")) {
            String normalizedLower = PythonResolutionResult.normalizeName(packageName);
            for (Path distInfo : stream) {
                String dirName = distInfo.getFileName().toString();
                // dirName is like "requests-2.31.0.dist-info"
                int dashIdx = dirName.indexOf('-');
                if (dashIdx > 0) {
                    String dirPkgName = dirName.substring(0, dashIdx);
                    if (PythonResolutionResult.normalizeName(dirPkgName).equals(normalizedLower)) {
                        Path metadata = distInfo.resolve("METADATA");
                        if (Files.exists(metadata)) {
                            return metadata;
                        }
                    }
                }
            }
        } catch (IOException e) {
            // fall through
        }
        return null;
    }

    /**
     * Parse {@code Requires-Dist} entries from package METADATA content.
     * Filters out entries gated by {@code extra ==} markers (optional extras).
     *
     * @return list of required package names (not normalized)
     */
    static List<String> parseRequiresDist(String metadataContent) {
        List<String> names = new ArrayList<>();
        for (String line : metadataContent.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("Requires-Dist:")) {
                continue;
            }
            String value = trimmed.substring("Requires-Dist:".length()).trim();

            // Skip entries with "extra ==" markers (optional extras, not always installed)
            if (value.contains("extra ==") || value.contains("extra=")) {
                continue;
            }

            // Extract package name: everything before the first version specifier or marker
            // PEP 508: name followed by optional extras [..], version specs, or ; markers
            int end = value.length();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '<' || c == '>' || c == '=' || c == '!' || c == ';' || c == '[' || c == ' ') {
                    end = i;
                    break;
                }
            }
            String name = value.substring(0, end).trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
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
