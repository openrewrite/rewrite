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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EventLoop.Promise;
import org.openrewrite.javascript.internal.lock.PosixPath;

/**
 * Port of yarn src/resolvers/exotics/link-resolver.js (yarn 1.22.22): the linked directory's package.json, whose
 * dependencies are requested, or a bare entry when it has none.
 */
final class LinkResolver {

    static final String LINK_PROTOCOL_PREFIX = "link:";

    private final PackageRequest request;
    private final String loc;

    LinkResolver(PackageRequest request, String fragment) {
        this.request = request;
        this.loc = fragment.startsWith(LINK_PROTOCOL_PREFIX) ? fragment.substring(LINK_PROTOCOL_PREFIX.length()) :
                fragment;
    }

    Promise<Manifest> resolve() {
        PackageResolver resolver = request.resolver;
        String location = FileResolver.location(loc, LINK_PROTOCOL_PREFIX);
        String name = PosixPath.basename(location.isEmpty() ? "." : location);
        Io<@Nullable String> read = Io.issue(resolver.loop, request.io, () -> resolver.files.manifest(location));
        request.awaited(read);
        return read.promise.then(text -> {
            Manifest manifest;
            if (location.isEmpty() || text == null && resolver.files.isAbsent(location + "/package.json")) {
                manifest = new Manifest();
                manifest.put("_uid", "");
                manifest.put("name", name);
                manifest.put("version", "0.0.0");
                manifest.put("_registry", NpmResolver.NPM_REGISTRY_ID);
            } else if (text == null) {
                throw new CannotKnow("the package.json of " + LINK_PROTOCOL_PREFIX + loc + ", which the project's " +
                                     "manifests do not include");
            } else {
                manifest = Config.tryManifest(location, text, request.config.isLooseSemver(),
                        false, "");
            }
            PackageRemote remote = new PackageRemote();
            remote.type = "link";
            remote.registry = NpmResolver.NPM_REGISTRY_ID;
            remote.reference = location;
            manifest.remote = remote;
            manifest.put("_uid", manifest.getVersion());
            return Promise.resolved(resolver.loop, manifest);
        });
    }
}
