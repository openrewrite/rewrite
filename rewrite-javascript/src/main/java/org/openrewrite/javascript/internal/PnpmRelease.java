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
import org.openrewrite.javascript.internal.lock.pnpm.Main;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmOptions;
import org.openrewrite.javascript.internal.lock.pnpm.WorkspaceManifestWriter;
import org.openrewrite.semver.NodeSemver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The pnpm release that installs a workspace root, as pnpm reads its manifest and pnpm-workspace.yaml; the recipes
 * write overrides, catalog entries and additions only for the releases whose writes are recorded.
 */
public final class PnpmRelease {

    // each range's first and last release recorded with the same writes
    private static final List<String> RECORDED = Arrays.asList("9.5.0 - 9.15.9", "10.32.1 - 10.34.6",
            "11.0.6 - 11.2.2");

    private final String version;
    private final int major;
    private final @Nullable String declined;

    private PnpmRelease(String version, int major, @Nullable String declined) {
        this.version = version;
        this.major = major;
        this.declined = declined;
    }

    /**
     * The release for the root manifest {@code rootManifest} under {@code workspace}, pnpm-workspace.yaml as pnpm
     * reads it (null without one): the one the root names when pnpm switches to it, the ported one otherwise.
     */
    public static PnpmRelease of(String rootManifest, @Nullable Map<String, @Nullable Object> workspace) {
        PnpmOptions.WantedPackageManager wanted;
        try {
            wanted = PnpmOptions.wantedPackageManager(rootManifest, workspace);
        } catch (RuntimeException e) {
            return new PnpmRelease("", -1, "pnpm cannot read the release the project names (" + e.getMessage() + ")");
        }
        // getConfig's pmOnFail: download and error switch to the release named, ignore and warn run this one
        boolean switches = wanted != null && "pnpm".equals(wanted.getName()) &&
                           wanted.getVersion() instanceof String &&
                           ("download".equals(wanted.getOnFail()) || "error".equals(wanted.getOnFail()));
        return named(switches ? (String) wanted.getVersion() : Main.PNPM_VERSION);
    }

    /**
     * The release {@code version} names, a version or a range whose pick the registry decides.
     */
    static PnpmRelease named(String version) {
        String exact = NodeSemver.valid(version, NodeSemver.Options.DEFAULT);
        for (String recorded : RECORDED) {
            if (exact != null ? NodeSemver.satisfies(exact, recorded, NodeSemver.Options.DEFAULT) :
                    NodeSemver.validRange(version, NodeSemver.Options.DEFAULT) != null &&
                    NodeSemver.subset(version, recorded, NodeSemver.Options.DEFAULT)) {
                return new PnpmRelease(version, Integer.parseInt(recorded.substring(0, recorded.indexOf('.'))), null);
            }
        }
        String ranges = String.join(", ", RECORDED);
        return new PnpmRelease(version, exact == null ? -1 : Integer.parseInt(exact.substring(0, exact.indexOf('.'))),
                exact != null ? "pnpm " + exact + " is not a release whose writes are recorded (" + ranges + ")" :
                        "the pnpm release the project names, " + version + ", is one the registry picks, not " +
                        "always a release whose writes are recorded (" + ranges + ")");
    }

    /**
     * The major of a release whose writes are recorded; for another, its major, or -1 for a range.
     */
    public int getMajor() {
        return major;
    }

    /**
     * Why the recipes write nothing for this release; null for one whose writes are recorded.
     */
    public @Nullable String declined() {
        return declined;
    }

    /**
     * The release's writer of pnpm-workspace.yaml; null when it is declined, or pnpm 9, which never writes the file.
     */
    public @Nullable WorkspaceManifestWriter writer() {
        return declined != null || major == 9 ? null : new WorkspaceManifestWriter(major);
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
        return major >= 10 && declined == null;
    }

    /**
     * Whether that cleanup keeps an entry an override refers to with {@code catalog:}, as pnpm 11 does and pnpm 10's
     * removePackagesFromWorkspaceCatalog, which reads the dependency fields only, does not.
     */
    public boolean cleanupKeepsOverrideReferences() {
        return major >= 11;
    }

    @Override
    public String toString() {
        return "pnpm " + version;
    }
}
