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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;

/**
 * Port of yarn src/resolvers/contextual/workspace-resolver.js (yarn 1.22.22): a pattern a workspace member
 * satisfies resolves to the member's own manifest, or, under {@code --focus}, to the registry's of its version.
 */
final class WorkspaceResolver {

    private final PackageRequest request;
    private final WorkspaceLayout workspaceLayout;

    WorkspaceResolver(PackageRequest request, @Nullable WorkspaceLayout workspaceLayout) {
        this.request = request;
        if (workspaceLayout == null) {
            throw new JsError(Js.INVARIANT_VIOLATION, "expected workspaceLayout");
        }
        this.workspaceLayout = workspaceLayout;
    }

    static boolean isWorkspace(String pattern, @Nullable WorkspaceLayout workspaceLayout) {
        return workspaceLayout != null && workspaceLayout.getManifestByPattern(pattern) != null;
    }

    Manifest resolve(@Nullable Manifest downloadedManifest) {
        WorkspaceLayout.Workspace workspace = workspaceLayout.getManifestByPattern(request.pattern);
        if (workspace == null) {
            throw new JsError(Js.INVARIANT_VIOLATION, "expected workspace");
        }
        Manifest manifest = workspace.getManifest();
        if (manifest.remote != null && manifest.remote.registryRemote != null) {
            return manifest;
        }
        Object registry = manifest.get("_registry");
        if (!Js.truthy(registry)) {
            throw new JsError(Js.INVARIANT_VIOLATION, "expected reference");
        }
        String hash = "";
        PackageRemote registryRemote = null;
        if (downloadedManifest != null && Js.strictEquals(manifest.getVersion(), downloadedManifest.getVersion())) {
            registryRemote = downloadedManifest.remote;
            if (registryRemote == null) {
                throw new JsError(Js.INVARIANT_VIOLATION, "missing remote info");
            }
            hash = registryRemote.hash;
            for (String key : new ArrayList<>(manifest.data.keySet())) {
                if (!key.startsWith("_")) {
                    manifest.data.remove(key);
                }
            }
            manifest.data.putAll(downloadedManifest.data);
            manifest.remote = downloadedManifest.remote;
        } else if (manifest.remote != null && Js.truthy(manifest.remote.hash)) {
            registryRemote = manifest.remote.registryRemote;
            hash = manifest.remote.hash;
        }
        if (registryRemote != null) {
            registryRemote = registryRemote.copy();
        }
        PackageRemote remote = manifest.remote == null ? new PackageRemote() : manifest.remote;
        remote.type = "workspace";
        remote.registryRemote = registryRemote;
        remote.registry = registry;
        remote.hash = hash;
        remote.reference = workspace.getLoc();
        manifest.remote = remote;
        manifest.put("_uid", manifest.getVersion());
        return manifest;
    }
}
