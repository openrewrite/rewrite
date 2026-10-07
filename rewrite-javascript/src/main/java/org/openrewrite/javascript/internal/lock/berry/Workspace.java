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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Port of @yarnpkg/core sources/Workspace.ts (Yarn 4.10.3): a directory with a package.json in the project, its
 * anchored descriptor and locator ({@code <name>@workspace:<relative cwd>}), the member directories its
 * {@code workspaces} patterns match, and the ranges it accepts.
 */
@Getter
final class Workspace {

    private final Project project;

    /**
     * The directory relative to the project's root, {@code .} for the root.
     */
    private final String relativeCwd;

    private final Manifest manifest;
    private final Descriptor anchoredDescriptor;
    private final Locator anchoredLocator;

    /**
     * The member directories, relative to the project's root, in the order the sorted glob results give.
     */
    private final Set<String> workspacesCwds = new LinkedHashSet<>();

    Workspace(Project project, String relativeCwd, @Nullable Manifest manifest) {
        this.project = project;
        this.relativeCwd = relativeCwd;
        this.manifest = manifest != null ? manifest : new Manifest();
        Ident ident = this.manifest.name != null ? this.manifest.name : StructUtils.makeIdent(null,
                computeCandidateName() + "-" + HashUtils.makeHash(relativeCwd).substring(0, 6));
        this.anchoredDescriptor = StructUtils.makeDescriptor(ident, WorkspaceResolver.PROTOCOL + relativeCwd);
        this.anchoredLocator = StructUtils.makeLocator(ident, WorkspaceResolver.PROTOCOL + relativeCwd);
    }

    /**
     * {@code setup}'s glob: the directories fast-glob reports for the patterns, sorted, that hold a package.json.
     *
     * @param directories every directory of the project known to hold a package.json, relative to the root
     */
    void setup(Collection<String> directories) {
        List<String> patterns = new ArrayList<>();
        for (Manifest.WorkspaceDefinition definition : manifest.workspaceDefinitions) {
            patterns.add(definition.getPattern());
        }
        if (patterns.isEmpty()) {
            return;
        }
        FastGlob fastGlob = new FastGlob(directories);
        List<String> relativeCwds = fastGlob.glob(patterns, relativeCwd);
        relativeCwds.sort(String::compareTo);
        for (String relative : relativeCwds) {
            String candidateCwd = FastGlob.resolve(relativeCwd, relative);
            if (fastGlob.exists(candidateCwd + "/package.json")) {
                workspacesCwds.add(candidateCwd);
            }
        }
    }

    /**
     * {@code accepts}: a {@code workspace:} range naming this directory, {@code *}, {@code ^} or {@code ~}, or a
     * semver range its version satisfies ({@code 0.0.0} without one), the latter without the protocol only under
     * {@code enableTransparentWorkspaces}.
     */
    boolean accepts(String range) {
        int protocolIndex = range.indexOf(':');
        String protocol = protocolIndex != -1 ? range.substring(0, protocolIndex + 1) : null;
        String pathname = protocolIndex != -1 ? range.substring(protocolIndex + 1) : range;
        if (WorkspaceResolver.PROTOCOL.equals(protocol) && PortablePath.normalize(pathname).equals(relativeCwd)) {
            return true;
        }
        if (WorkspaceResolver.PROTOCOL.equals(protocol) &&
            ("*".equals(pathname) || "^".equals(pathname) || "~".equals(pathname))) {
            return true;
        }
        SemverUtils.Range semverRange = SemverUtils.validRange(pathname);
        if (semverRange == null) {
            return false;
        }
        if (WorkspaceResolver.PROTOCOL.equals(protocol)) {
            return semverRange.test(manifest.version != null ? manifest.version : "0.0.0");
        }
        if (!project.configuration.getOptions().getBoolean("enableTransparentWorkspaces")) {
            return false;
        }
        if (manifest.version != null) {
            return semverRange.test(manifest.version);
        }
        return false;
    }

    private String computeCandidateName() {
        if (".".equals(relativeCwd)) {
            return "root-workspace";
        }
        String basename = relativeCwd.substring(relativeCwd.lastIndexOf('/') + 1);
        return basename.isEmpty() ? "unnamed-workspace" : basename;
    }
}
