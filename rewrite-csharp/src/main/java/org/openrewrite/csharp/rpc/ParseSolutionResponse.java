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

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * Response from ParseSolution RPC call.
 * Contains parsed source file items and per-project MSBuild metadata.
 */
class ParseSolutionResponse {
    List<Item> items = emptyList();
    List<ProjectMetadata> projects = emptyList();

    int itemCount() {
        return items.size();
    }

    Item getItem(int index) {
        return items.get(index);
    }

    /**
     * A single parsed source file item.
     */
    @Value
    static class Item {
        /**
         * The object ID that can be used to retrieve the parsed source file.
         */
        String id;

        /**
         * The fully qualified class name of the source file type.
         * Example: org.openrewrite.csharp.tree.Cs$CompilationUnit
         */
        String sourceFileType;

        /**
         * The path of the project this file belongs to.
         */
        String projectPath;
    }

    /**
     * MSBuild-evaluated project metadata for a single .csproj.
     * Returned by the C# side after MSBuildWorkspace evaluation.
     */
    @Value
    static class ProjectMetadata {
        String projectPath;

        @Nullable
        String sdk;

        Map<String, PropertyEntry> properties;

        List<TargetFrameworkEntry> targetFrameworks;

        List<PackageSourceEntry> packageSources;
    }

    @Value
    static class PackageSourceEntry {
        String key;
        String url;
    }

    @Value
    static class PropertyEntry {
        String value;
        @Nullable String definedIn;
    }

    @Value
    static class TargetFrameworkEntry {
        String targetFramework;
        List<PackageReferenceEntry> packageReferences;
        List<ResolvedPackageEntry> resolvedPackages;
        List<ProjectReferenceEntry> projectReferences;
    }

    @Value
    static class PackageReferenceEntry {
        String include;
        @Nullable String requestedVersion;
        @Nullable String resolvedVersion;
    }

    @Value
    static class ResolvedPackageEntry {
        String name;
        String resolvedVersion;
        List<ResolvedPackageEntry> dependencies;
        int depth;
    }

    @Value
    static class ProjectReferenceEntry {
        String include;
    }
}
