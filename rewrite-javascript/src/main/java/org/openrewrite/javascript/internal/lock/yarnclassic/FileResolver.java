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
 * Port of yarn src/resolvers/exotics/file-resolver.js (yarn 1.22.22): the directory's own package.json, copied, or
 * a link under {@code yarn-link-file-dependencies}.
 */
final class FileResolver {

    static final String FILE_PROTOCOL_PREFIX = "file:";

    private final PackageRequest request;
    private final String loc;

    FileResolver(PackageRequest request, String fragment) {
        this.request = request;
        this.loc = fragment.startsWith(FILE_PROTOCOL_PREFIX) ? fragment.substring(FILE_PROTOCOL_PREFIX.length()) :
                fragment;
    }

    Promise<Manifest> resolve() {
        PackageResolver resolver = request.resolver;
        String location = location(loc, "file:");
        if (request.config.isLinkFileDependencies()) {
            Manifest manifest = new Manifest();
            manifest.put("_uid", "");
            manifest.put("name", "");
            manifest.put("version", "0.0.0");
            manifest.put("_registry", NpmResolver.NPM_REGISTRY_ID);
            PackageRemote remote = new PackageRemote();
            remote.type = "link";
            remote.registry = NpmResolver.NPM_REGISTRY_ID;
            remote.reference = location;
            manifest.remote = remote;
            manifest.put("_uid", manifest.getVersion());
            return Promise.resolved(resolver.loop, manifest);
        }
        Io<@Nullable String> read = Io.issue(resolver.loop, request.io, () -> resolver.files.manifest(location));
        request.awaited(read);
        return read.promise.then(text -> {
            if (text == null && resolver.files.isMissing(location)) {
                // yarn prints the absolute path, relative to the root here
                throw new MessageError(Lang.format("Package $1 refers to a non-existing file '$0'.", location,
                        request.pattern.split("@", -1)[0]));
            }
            if (text == null) {
                throw new CannotKnow("the package.json of " + FILE_PROTOCOL_PREFIX + loc + ", which the project's " +
                                     "manifests do not include");
            }
            Manifest manifest = Config.tryManifest(location, text, request.config.isLooseSemver(),
                    false, "");
            PackageRemote remote = new PackageRemote();
            remote.type = "copy";
            remote.registry = manifest.get("_registry");
            remote.hash = "copy-" + resolver.nextCopyHash++;
            remote.reference = location;
            manifest.remote = remote;
            manifest.put("_uid", manifest.getVersion());
            return Promise.resolved(resolver.loop, manifest);
        });
    }

    /**
     * {@code path.resolve(config.lockfileFolder, loc)} as a location in the project.
     */
    static String location(String loc, String protocol) {
        if (PosixPath.isAbsolute(loc)) {
            throw new CannotKnow("where the project sits on disk, which the " + protocol + loc + " target is read " +
                                 "relative to");
        }
        String location = ProjectFiles.location(loc);
        if (location == null) {
            throw new CannotKnow("the package.json of " + protocol + loc + ", outside the project");
        }
        return location;
    }
}
