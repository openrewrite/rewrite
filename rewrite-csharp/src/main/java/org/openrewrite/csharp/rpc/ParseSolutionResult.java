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
package org.openrewrite.csharp.rpc;

import org.openrewrite.SourceFile;
import org.openrewrite.csharp.marker.MSBuildProject;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

/**
 * Result of a ParseSolution RPC call.
 * Provides lazy access to parsed source files and eager access to project metadata.
 */
public class ParseSolutionResult {
    private final Stream<SourceFile> sourceFiles;
    private final List<MSBuildProject> projects;
    private final Map<String, MSBuildProject> projectsByPath;

    ParseSolutionResult(Stream<SourceFile> sourceFiles, List<ParseSolutionResponse.ProjectMetadata> rawProjects) {
        this.sourceFiles = sourceFiles;
        this.projects = new ArrayList<>(rawProjects.size());
        this.projectsByPath = new LinkedHashMap<>(rawProjects.size());
        for (ParseSolutionResponse.ProjectMetadata raw : rawProjects) {
            MSBuildProject marker = toMarker(raw);
            projects.add(marker);
            projectsByPath.put(normalizePathKey(raw.getProjectPath()), marker);
        }
    }

    /**
     * Lazily-fetched stream of parsed source files (Cs.CompilationUnit, Xml.Document, etc.).
     * Each file is retrieved from the C# side one at a time via getObject().
     */
    public Stream<SourceFile> sourceFiles() {
        return sourceFiles;
    }

    /**
     * Eagerly-available project metadata for all projects in the solution.
     * Built from MSBuild evaluation on the C# side.
     */
    public List<MSBuildProject> projects() {
        return projects;
    }

    /**
     * The set of .csproj paths for all projects in the solution.
     * Paths are normalized strings as returned by the C# side.
     */
    public Set<String> projectPaths() {
        return projectsByPath.keySet();
    }

    /**
     * Find the project metadata for a specific .csproj path.
     *
     * @param projectPath path to the .csproj file
     * @return the MSBuildProject marker, or null if not found
     */
    public MSBuildProject getProject(String projectPath) {
        return projectsByPath.get(normalizePathKey(projectPath));
    }

    private static String normalizePathKey(String path) {
        return Paths.get(path).normalize().toString();
    }

    private static MSBuildProject toMarker(ParseSolutionResponse.ProjectMetadata raw) {
        Map<String, MSBuildProject.PropertyValue> properties = new LinkedHashMap<>();
        if (raw.getProperties() != null) {
            for (Map.Entry<String, ParseSolutionResponse.PropertyEntry> entry : raw.getProperties().entrySet()) {
                ParseSolutionResponse.PropertyEntry pe = entry.getValue();
                properties.put(entry.getKey(), new MSBuildProject.PropertyValue(
                        pe.getValue(),
                        pe.getDefinedIn() != null ? Paths.get(pe.getDefinedIn()) : null
                ));
            }
        }

        List<MSBuildProject.TargetFramework> targetFrameworks = new ArrayList<>();
        if (raw.getTargetFrameworks() != null) {
            for (ParseSolutionResponse.TargetFrameworkEntry tfEntry : raw.getTargetFrameworks()) {
                List<MSBuildProject.PackageReference> packageRefs = new ArrayList<>();
                if (tfEntry.getPackageReferences() != null) {
                    for (ParseSolutionResponse.PackageReferenceEntry prEntry : tfEntry.getPackageReferences()) {
                        packageRefs.add(new MSBuildProject.PackageReference(
                                prEntry.getInclude(),
                                prEntry.getRequestedVersion(),
                                prEntry.getResolvedVersion()
                        ));
                    }
                }

                List<MSBuildProject.ResolvedPackage> resolvedPackages = new ArrayList<>();
                if (tfEntry.getResolvedPackages() != null) {
                    for (ParseSolutionResponse.ResolvedPackageEntry rpEntry : tfEntry.getResolvedPackages()) {
                        resolvedPackages.add(toResolvedPackage(rpEntry));
                    }
                }

                List<MSBuildProject.ProjectReference> projectRefs = new ArrayList<>();
                if (tfEntry.getProjectReferences() != null) {
                    for (ParseSolutionResponse.ProjectReferenceEntry projEntry : tfEntry.getProjectReferences()) {
                        projectRefs.add(new MSBuildProject.ProjectReference(projEntry.getInclude()));
                    }
                }

                targetFrameworks.add(new MSBuildProject.TargetFramework(
                        tfEntry.getTargetFramework(),
                        packageRefs,
                        resolvedPackages,
                        projectRefs
                ));
            }
        }

        List<MSBuildProject.PackageSource> packageSources = new ArrayList<>();
        if (raw.getPackageSources() != null) {
            for (ParseSolutionResponse.PackageSourceEntry psEntry : raw.getPackageSources()) {
                packageSources.add(new MSBuildProject.PackageSource(psEntry.getKey(), psEntry.getUrl()));
            }
        }

        return MSBuildProject.builder()
                .id(randomId())
                .sdk(raw.getSdk())
                .properties(properties)
                .packageSources(packageSources)
                .targetFrameworks(targetFrameworks)
                .build();
    }

    private static MSBuildProject.ResolvedPackage toResolvedPackage(ParseSolutionResponse.ResolvedPackageEntry entry) {
        List<MSBuildProject.ResolvedPackage> deps = new ArrayList<>();
        if (entry.getDependencies() != null) {
            for (ParseSolutionResponse.ResolvedPackageEntry dep : entry.getDependencies()) {
                deps.add(toResolvedPackage(dep));
            }
        }
        return new MSBuildProject.ResolvedPackage(
                entry.getName(),
                entry.getResolvedVersion(),
                deps,
                entry.getDepth()
        );
    }
}
