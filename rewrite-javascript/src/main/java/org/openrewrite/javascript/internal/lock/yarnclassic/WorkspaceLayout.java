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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.Map;

/**
 * Port of yarn src/workspace-layout.js (yarn 1.22.22): the workspace members by name, and the member a pattern names
 * when the member's version satisfies the pattern's range.
 */
final class WorkspaceLayout {

    @Value
    static class Workspace {
        String loc;
        Manifest manifest;
    }

    final Map<String, Workspace> workspaces;
    final Config config;
    @Nullable
    String virtualManifestName;

    WorkspaceLayout(Map<String, Workspace> workspaces, Config config) {
        this.workspaces = workspaces;
        this.config = config;
    }

    @Nullable
    Workspace getWorkspaceManifest(String key) {
        return workspaces.get(key);
    }

    @Nullable
    Workspace getManifestByPattern(String pattern) {
        NormalizedPattern normalized = NormalizedPattern.normalizePattern(pattern);
        Workspace workspace = getWorkspaceManifest(normalized.getName());
        if (workspace == null || !NodeSemver.satisfies(workspace.getManifest().getVersion(), normalized.getRange(),
                config.isLooseSemver() ? Config.LOOSE_SEMVER : Config.SEMVER)) {
            return null;
        }
        return workspace;
    }
}
