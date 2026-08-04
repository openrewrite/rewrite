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
package org.openrewrite.python.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.metadata.MetadataParser;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * Reads the resolved dependencies of an already-installed environment (a virtual env): one
 * {@link ResolvedDependency} per {@code site-packages/*.dist-info}. The dist-info metadata is
 * equivalent to {@code pip freeze} output without running a subprocess.
 */
public final class InstalledEnvParser {

    private static final Pattern EXTRA_MARKER_PATTERN = Pattern.compile("\\bextra\\s*==");

    private InstalledEnvParser() {
    }

    public static List<ResolvedDependency> parse(Path env) {
        Path sitePackages = findSitePackages(env);
        if (sitePackages == null) {
            return emptyList();
        }
        List<PythonResolutionLinker.UnlinkedPackage> packages = scanDistInfos(sitePackages);
        packages.sort(Comparator.comparing(PythonResolutionLinker.UnlinkedPackage::getName));
        return PythonResolutionLinker.buildGraph(packages);
    }

    /**
     * One package per {@code site-packages/*.dist-info}: name and version from the
     * directory name, dependency names from its METADATA.
     */
    private static List<PythonResolutionLinker.UnlinkedPackage> scanDistInfos(Path sitePackages) {
        List<PythonResolutionLinker.UnlinkedPackage> packages = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(sitePackages, "*.dist-info")) {
            for (Path distInfo : ds) {
                String dir = distInfo.getFileName().toString();
                String base = dir.substring(0, dir.length() - ".dist-info".length());
                int dash = base.lastIndexOf('-');
                if (dash > 0) {
                    packages.add(new PythonResolutionLinker.UnlinkedPackage(
                            base.substring(0, dash), base.substring(dash + 1), null,
                            readRequiresDist(distInfo.resolve("METADATA"))));
                }
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
        return packages;
    }

    /** {@code <env>/lib/pythonX.Y/site-packages} (POSIX) or {@code <env>/Lib/site-packages} (Windows). */
    public static @Nullable Path findSitePackages(Path env) {
        Path lib = env.resolve("lib");
        if (Files.isDirectory(lib)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(lib, "python*")) {
                for (Path p : ds) {
                    Path sp = p.resolve("site-packages");
                    if (Files.isDirectory(sp)) {
                        return sp;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        Path winSp = env.resolve("Lib").resolve("site-packages");
        return Files.isDirectory(winSp) ? winSp : null;
    }

    /**
     * Link transitive dependencies by reading each package's {@code dist-info/METADATA}
     * ({@code Requires-Dist} entries) from {@code sitePackages}, building the graph via
     * {@link PythonResolutionLinker#buildGraph}. Packages are matched to dist-info
     * directories by normalized name and exact version, so freeze-output spellings
     * that differ from the canonical directory name still resolve.
     *
     * @param resolved flat list of resolved dependencies installed in {@code sitePackages}
     * @return new list with dependencies linked
     */
    public static List<ResolvedDependency> linkDependenciesFromMetadata(List<ResolvedDependency> resolved, Path sitePackages) {
        Map<String, List<String>> requiresDistByPackage = new HashMap<>();
        for (PythonResolutionLinker.UnlinkedPackage pkg : scanDistInfos(sitePackages)) {
            requiresDistByPackage.putIfAbsent(
                    PythonResolutionResult.normalizeName(pkg.getName()) + "@" + pkg.getVersion(),
                    pkg.getDependencyNames());
        }
        List<PythonResolutionLinker.UnlinkedPackage> packages = new ArrayList<>(resolved.size());
        for (ResolvedDependency r : resolved) {
            List<String> requiresDist = requiresDistByPackage.getOrDefault(
                    PythonResolutionResult.normalizeName(r.getName()) + "@" + r.getVersion(), emptyList());
            packages.add(new PythonResolutionLinker.UnlinkedPackage(
                    r.getName(), r.getVersion(), r.getSource(), requiresDist));
        }
        return PythonResolutionLinker.buildGraph(packages);
    }

    /**
     * Read the names of a package's required (non-extra) dependencies: the
     * {@code Requires-Dist} headers of its METADATA file, minus entries gated by
     * {@code extra ==} markers (optional extras, not always installed).
     *
     * @return list of required package names (not normalized)
     */
    private static List<String> readRequiresDist(Path metadataFile) {
        List<String> requiresDist;
        try {
            requiresDist = MetadataParser.parse(Files.readAllBytes(metadataFile)).getRequiresDist();
        } catch (IOException e) {
            return emptyList();
        }
        List<String> names = new ArrayList<>();
        for (String value : requiresDist) {
            if (EXTRA_MARKER_PATTERN.matcher(value).find()) {
                continue;
            }
            String name = PyProjectHelper.extractPackageName(value);
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }
}
