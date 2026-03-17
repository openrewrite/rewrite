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
package org.openrewrite.csharp.marker;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;

/**
 * Metadata about a .NET project (.csproj) extracted from MSBuild evaluation.
 * Attached as a marker to the Xml.Document representing the .csproj file in the LST.
 * Analogous to MavenResolutionResult for Maven and GradleProject for Gradle.
 */
@Value
@Builder
@AllArgsConstructor
public class MSBuildProject implements Marker, Serializable {

    @With
    @Builder.Default
    UUID id = randomId();

    /**
     * The SDK attribute from the Project element (e.g., "Microsoft.NET.Sdk", "Microsoft.NET.Sdk.Web").
     */
    @With
    @Nullable
    String sdk;

    /**
     * MSBuild properties with provenance tracking.
     * The key is the property name, the value includes the resolved value
     * and the file where the property is defined.
     */
    @With
    @Builder.Default
    Map<String, PropertyValue> properties = emptyMap();

    /**
     * NuGet package sources configured for this project, extracted from nuget.config.
     * Used by version upgrade recipes to resolve available package versions.
     * Analogous to MavenRepository list on MavenResolutionResult/GradleProject.
     */
    @With
    @Builder.Default
    List<PackageSource> packageSources = emptyList();

    /**
     * Per-target-framework metadata. Each TFM has its own set of
     * package references, resolved packages, and project references,
     * since MSBuild evaluates the project independently per TFM.
     */
    @With
    @Builder.Default
    List<TargetFramework> targetFrameworks = emptyList();

    /**
     * Metadata for a single target framework within this project.
     */
    @Value
    @Builder
    @AllArgsConstructor
    public static class TargetFramework implements Serializable {
        String targetFramework;

        @Builder.Default
        List<PackageReference> packageReferences = emptyList();

        @Builder.Default
        List<ResolvedPackage> resolvedPackages = emptyList();

        @Builder.Default
        List<ProjectReference> projectReferences = emptyList();
    }

    /**
     * A declared NuGet package reference from the .csproj or Directory.Packages.props.
     * Tracks both the raw (requested) and MSBuild-evaluated (resolved) version,
     * enabling recipes to decide whether to update a property or a literal.
     */
    @Value
    @Builder
    @AllArgsConstructor
    public static class PackageReference implements Serializable {
        String include;

        @Nullable
        String requestedVersion;

        @Nullable
        String resolvedVersion;
    }

    /**
     * A resolved NuGet package from the transitive dependency tree.
     * Built from project.assets.json after dotnet restore.
     */
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    @Value
    @Builder
    @AllArgsConstructor
    public static class ResolvedPackage implements Serializable {
        String name;

        String resolvedVersion;

        @Builder.Default
        List<ResolvedPackage> dependencies = emptyList();

        int depth;
    }

    /**
     * A project-to-project reference within a solution.
     */
    @Value
    @Builder
    @AllArgsConstructor
    public static class ProjectReference implements Serializable {
        String include;
    }

    /**
     * An MSBuild property value with provenance tracking.
     * Recipes use the definedIn path to determine which file to edit
     * when updating a property value.
     */
    @Value
    @Builder
    @AllArgsConstructor
    public static class PropertyValue implements Serializable {
        String value;

        @Nullable
        Path definedIn;
    }

    /**
     * A NuGet package source from nuget.config.
     * Analogous to MavenRepository in the Maven/Gradle ecosystems.
     */
    @Value
    @Builder
    @AllArgsConstructor
    public static class PackageSource implements Serializable {
        /**
         * The key/name of the package source (e.g., "nuget.org", "mycompany-feed").
         */
        String key;

        /**
         * The NuGet V3 service index URL
         * (e.g., "https://api.nuget.org/v3/index.json").
         */
        String url;
    }
}
