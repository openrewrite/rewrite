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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of yarn src/package-reference.js (yarn 1.22.22): one resolved package that the patterns resolving to it share,
 * with the requests that reached it, its dependencies' patterns and the permissions their lock entries carried.
 */
final class PackageReference {

    final PackageResolver resolver;
    final Lockfile lockfile;
    final List<PackageRequest> requests = new ArrayList<>();
    final @Nullable String hint;
    final @Nullable Object registry;
    final @Nullable Object version;
    final @Nullable Object name;
    final @Nullable Object uid;
    final PackageRemote remote;

    List<String> dependencies = new ArrayList<>();
    final Map<String, @Nullable Object> permissions = new LinkedHashMap<>();
    List<String> patterns = new ArrayList<>();
    @Nullable
    Boolean optional;
    double level = Double.POSITIVE_INFINITY;
    boolean fresh;
    boolean ignore;

    PackageReference(PackageRequest request, Manifest info, PackageRemote remote) {
        this.resolver = request.resolver;
        this.lockfile = request.lockfile;
        this.hint = request.hint;
        this.registry = remote.registry;
        this.version = info.getVersion();
        this.name = info.getName();
        this.uid = info.getUid();
        this.remote = remote;
        addRequest(request);
    }

    void setFresh(boolean fresh) {
        this.fresh = fresh;
    }

    void addRequest(PackageRequest request) {
        requests.add(request);
        level = Math.min(level, request.parentNames.size());
    }

    void prune() {
        for (String selfPattern : new ArrayList<>(patterns)) {
            resolver.removePattern(selfPattern);
        }
    }

    void addDependencies(List<String> deps) {
        List<String> concat = new ArrayList<>(dependencies);
        concat.addAll(deps);
        dependencies = concat;
    }

    void setPermission(String key, @Nullable Object val) {
        permissions.put(key, val);
    }

    void addPattern(String pattern, Manifest manifest) {
        resolver.addPattern(pattern, manifest);

        patterns.add(pattern);

        Map<String, @Nullable Object> shrunk = lockfile.getLocked(pattern);
        if (shrunk != null && Js.truthy(shrunk.get("permissions"))) {
            // misc.js entries: for..in, which walks a string's or an array's indices too
            Object permissions = shrunk.get("permissions");
            for (String key : Js.keys(permissions)) {
                setPermission(key, Js.get(permissions, key));
            }
        }
    }

    void addOptional(boolean optional) {
        if (this.optional == null) {
            this.optional = optional;
        } else if (!optional) {
            this.optional = false;
        }
    }
}
