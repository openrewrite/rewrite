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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/workspace.projects-reader src/index.ts {@code findWorkspaceProjectsNoCheck} and src/findPackages.ts
 * (pnpm 11.2.2), with @pnpm/lockfile.fs {@code getLockfileImporterId}, over the directories holding a package.json.
 */
public final class FindPackages {

    private static final List<String> DEFAULT_IGNORE = Arrays.asList("**/node_modules/**", "**/bower_components/**",
            "**/test/**", "**/tests/**");
    private static final List<String> IGNORE = Arrays.asList("**/node_modules/**", "**/bower_components/**");

    private FindPackages() {
    }

    /**
     * {@code findWorkspaceProjectsNoCheck}: the project directories among the directories holding a package.json,
     * sorted as pnpm sorts them ({@code ""} is the root).
     */
    static List<String> findWorkspaceProjects(Collection<String> manifestDirs, List<String> patterns) {
        List<String> projects = findPackages(manifestDirs, patterns, IGNORE, true);
        projects.sort(KeySorting::lexCompare);
        return projects;
    }

    /**
     * The projects of the workspace whose pnpm-workspace.yaml {@link WorkspaceManifest#readWorkspaceManifest} read as
     * {@code workspaceManifest}, among the directories holding a manifest, as getConfig reads its packages.
     */
    public static List<String> workspaceProjects(@Nullable Map<String, @Nullable Object> workspaceManifest,
                                                 Collection<String> manifestDirs) {
        return findWorkspaceProjects(manifestDirs, PnpmOptions.workspacePackagePatterns(workspaceManifest));
    }

    /**
     * {@code findPackages}: the directories whose package.json the patterns match, deduplicated, by directory.
     */
    static List<String> findPackages(Collection<String> manifestDirs, @Nullable List<String> patterns,
                                     @Nullable List<String> ignorePatterns, boolean includeRoot) {
        List<String> ignore = ignorePatterns == null ? DEFAULT_IGNORE : ignorePatterns;
        List<String> manifestPaths = new ArrayList<>();
        for (String dir : manifestDirs) {
            manifestPaths.add(dir.isEmpty() ? "package.json" : dir + "/package.json");
        }
        List<String> paths = Tinyglobby.glob(normalizePatterns(patterns == null ? Arrays.asList(".", "**") :
                patterns), ignore, manifestPaths);
        if (includeRoot) {
            paths.addAll(Tinyglobby.glob(normalizePatterns(Collections.singletonList(".")), ignore, manifestPaths));
        }
        List<String> dirs = new ArrayList<>();
        for (String path : paths) {
            dirs.add(RootRelativePath.dirname(path));
        }
        dirs.sort(KeySorting::lexCompare);
        return new ArrayList<>(new LinkedHashSet<>(dirs));
    }

    private static List<String> normalizePatterns(List<String> patterns) {
        List<String> normalized = new ArrayList<>();
        for (String pattern : patterns) {
            normalized.add(pattern.replaceFirst("/?\\z", "/package.{json,yaml,json5}"));
        }
        return normalized;
    }

    /**
     * {@code getLockfileImporterId}: {@code .} for the lock's own directory.
     */
    static String getLockfileImporterId(String rootDir) {
        return rootDir.isEmpty() ? "." : rootDir;
    }
}
