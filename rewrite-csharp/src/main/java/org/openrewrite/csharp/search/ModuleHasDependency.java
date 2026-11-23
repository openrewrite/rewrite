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
package org.openrewrite.csharp.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.csharp.marker.DotNetProject;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class ModuleHasDependency extends ScanningRecipe<ModuleHasDependency.Accumulator> {

    private static final Path ROOT = Paths.get("");

    String displayName = "Module has dependency";

    String description = "Searches for .NET projects that have a NuGet package dependency matching the specified package id. " +
                         "Places a `SearchResult` marker on all sources within a project with a matching dependency. " +
                         "A source belongs to the project file (`.csproj`, `.vbproj`, `.fsproj`) in its nearest enclosing directory. " +
                         "This recipe is intended to be used as a precondition for other recipes. " +
                         "For example this could be used to limit the application of a migration to only projects " +
                         "that use a specific version of a package. " +
                         "If the search result you want is instead just the project file referencing the package, " +
                         "use the `FindNuGetPackageReference` recipe instead.";

    @Option(displayName = "Package pattern",
            description = "Package id glob pattern used to match dependencies. Matching is case-insensitive.",
            example = "Microsoft.EntityFrameworkCore*")
    String packageIdPattern;

    @Option(displayName = "Version",
            description = "Match only dependencies with the specified resolved version. " +
                          "Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used. " +
                          "All versions are searched by default.",
            example = "8.x",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Only direct",
            description = "If enabled, transitive dependencies will not be considered. All dependencies are searched by default.",
            required = false,
            example = "true")
    @Nullable
    Boolean onlyDirect;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s%s`", packageIdPattern, version == null ? "" : ":" + version);
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate();
        if (version != null) {
            v = v.and(Semver.validate(version, null));
        }
        return v;
    }

    @Value
    public static class Accumulator {
        Map<Path, Map<String, Boolean>> projectsByDirectory;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(new HashMap<>());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile && isProjectFile(((SourceFile) tree).getSourcePath())) {
                    SourceFile sourceFile = (SourceFile) tree;
                    sourceFile.getMarkers().findFirst(MSBuildProject.class).ifPresent(project ->
                            acc.getProjectsByDirectory()
                                    .computeIfAbsent(directoryOf(sourceFile.getSourcePath()), d -> new HashMap<>())
                                    .merge(projectName(sourceFile.getSourcePath()), hasDependency(project), Boolean::logicalOr));
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile && belongsToMatchingProject((SourceFile) tree, acc)) {
                    return SearchResult.found(tree, "Module has dependency: " + packageIdPattern + (version == null ? "" : ":" + version));
                }
                return tree;
            }
        };
    }

    private boolean hasDependency(MSBuildProject project) {
        VersionComparator versionComparator = version == null ? null : Semver.validate(version, null).getValue();
        for (MSBuildProject.TargetFramework targetFramework : project.getTargetFrameworks()) {
            if (targetFramework.getResolvedPackages().isEmpty()) {
                for (MSBuildProject.PackageReference reference : targetFramework.getPackageReferences()) {
                    String referenceVersion = reference.getResolvedVersion() != null ? reference.getResolvedVersion() : reference.getRequestedVersion();
                    if (matches(reference.getInclude(), referenceVersion, versionComparator)) {
                        return true;
                    }
                }
                continue;
            }
            for (MSBuildProject.ResolvedPackage resolved : targetFramework.getResolvedPackages()) {
                if (!"package".equalsIgnoreCase(resolved.getType()) ||
                    (Boolean.TRUE.equals(onlyDirect) && resolved.getDepth() != 0)) {
                    continue;
                }
                if (matches(resolved.getName(), resolved.getResolvedVersion(), versionComparator)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matches(String packageId, @Nullable String packageVersion, @Nullable VersionComparator versionComparator) {
        if (!StringUtils.matchesGlob(packageId.toLowerCase(Locale.ROOT), packageIdPattern.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return versionComparator == null || (packageVersion != null && versionComparator.isValid(null, packageVersion));
    }

    private static boolean belongsToMatchingProject(SourceFile sourceFile, Accumulator acc) {
        if (acc.getProjectsByDirectory().isEmpty()) {
            return false;
        }
        Path directory = directoryOf(sourceFile.getSourcePath());
        while (true) {
            Map<String, Boolean> projects = acc.getProjectsByDirectory().get(directory);
            if (projects != null) {
                if (projects.size() > 1) {
                    String owner = isProjectFile(sourceFile.getSourcePath()) ?
                            projectName(sourceFile.getSourcePath()) :
                            sourceFile.getMarkers().findFirst(DotNetProject.class).map(DotNetProject::getProjectName).orElse(null);
                    Boolean ownerMatches = owner == null ? null : projects.get(owner);
                    if (ownerMatches != null) {
                        return ownerMatches;
                    }
                }
                return projects.containsValue(true);
            }
            if (directory.equals(ROOT)) {
                return false;
            }
            directory = directoryOf(directory);
        }
    }

    private static boolean isProjectFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".csproj") || fileName.endsWith(".vbproj") || fileName.endsWith(".fsproj");
    }

    private static Path directoryOf(Path path) {
        Path parent = path.getParent();
        return parent == null ? ROOT : parent;
    }

    private static String projectName(Path projectFile) {
        String fileName = projectFile.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        return extension < 0 ? fileName : fileName.substring(0, extension);
    }
}
