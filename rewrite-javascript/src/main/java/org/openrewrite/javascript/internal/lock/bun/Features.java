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
package org.openrewrite.javascript.internal.lock.bun;

/**
 * Port of bun's src/install/install.zig {@code Features} (bun 1.3.10): which dependency groups and package.json
 * sections a package of a kind reads; with {@code is_workspace} (src/install_types/resolver_hooks.rs, 1.4.0).
 */
final class Features {

    static final Features MAIN = new Features(true, true, true, false, true, true, true, true, true, true);
    static final Features FOLDER = new Features(true, true, false, false, true, true, false, false, false, false);
    static final Features WORKSPACE = new Features(true, true, false, true, true, true, true, false, false, false);
    static final Features NPM = new Features(true, false, false, false, true, true, false, false, false, false);

    final boolean dependencies;
    final boolean devDependencies;
    final boolean isMain;

    /**
     * The package is a workspace, whose dependencies, as the root's, may reference catalogs and name workspaces (from
     * 1.4.0).
     */
    final boolean isWorkspace;

    final boolean optionalDependencies;
    final boolean peerDependencies;
    final boolean trustedDependencies;
    final boolean workspaces;
    final boolean patchedDependencies;
    final boolean checkForDuplicateDependencies;

    private Features(boolean dependencies, boolean devDependencies, boolean isMain, boolean isWorkspace,
                     boolean optionalDependencies, boolean peerDependencies, boolean trustedDependencies,
                     boolean workspaces, boolean patchedDependencies, boolean checkForDuplicateDependencies) {
        this.dependencies = dependencies;
        this.devDependencies = devDependencies;
        this.isMain = isMain;
        this.isWorkspace = isWorkspace;
        this.optionalDependencies = optionalDependencies;
        this.peerDependencies = peerDependencies;
        this.trustedDependencies = trustedDependencies;
        this.workspaces = workspaces;
        this.patchedDependencies = patchedDependencies;
        this.checkForDuplicateDependencies = checkForDuplicateDependencies;
    }
}
