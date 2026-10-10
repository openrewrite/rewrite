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
package org.openrewrite.javascript.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmOptions;
import org.openrewrite.javascript.internal.lock.pnpm.WorkspaceManifestWriter;

import java.util.Map;

/**
 * The pnpm release that installs a workspace root, as pnpm reads its manifest and pnpm-workspace.yaml; the recipes
 * write overrides, catalog entries and additions for the releases recorded (9.15.9, 10.34.6, 11.2.2) only.
 */
public final class PnpmRelease {

    private final int major;

    private PnpmRelease(int major) {
        this.major = major;
    }

    /**
     * The release for the root manifest {@code rootManifest} under {@code workspace}, pnpm-workspace.yaml as pnpm
     * reads it (null without one).
     */
    public static PnpmRelease of(String rootManifest, @Nullable Map<String, @Nullable Object> workspace) {
        return new PnpmRelease(PnpmOptions.releaseMajor(rootManifest, workspace));
    }

    /**
     * The major; -1 when the range the root names is open between majors or pnpm cannot read what it names.
     */
    public int getMajor() {
        return major;
    }

    /**
     * Why the recipes write nothing for this release; null for a recorded one.
     */
    public @Nullable String declined() {
        if (major < 0) {
            return "the pnpm release the project names is not one major (a range the registry decides between " +
                   "majors, or a value pnpm cannot read)";
        }
        return major < 9 || major > 11 ? "pnpm " + major + ".x is not a release whose writes are recorded" : null;
    }

    /**
     * The release's writer of pnpm-workspace.yaml; null when it is declined, or pnpm 9, which never writes the file.
     */
    public @Nullable WorkspaceManifestWriter writer() {
        return declined() != null || major == 9 ? null : new WorkspaceManifestWriter(major);
    }

    /**
     * Whether the writer sorts every level of the document, packages first (pnpm 10's sortKeysByPriority).
     */
    public boolean sortsEveryLevel() {
        return major == 10;
    }

    /**
     * Whether the release's installs apply {@code cleanupUnusedCatalogs}, which pnpm 9 does not have.
     */
    public boolean cleansUpUnusedCatalogs() {
        return major >= 10 && declined() == null;
    }

    @Override
    public String toString() {
        return "pnpm " + major;
    }
}
