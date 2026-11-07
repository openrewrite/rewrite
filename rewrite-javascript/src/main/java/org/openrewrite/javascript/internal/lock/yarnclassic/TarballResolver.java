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

import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EventLoop.Promise;

/**
 * Port of yarn src/resolvers/exotics/tarball-resolver.js (yarn 1.22.22): a locked tarball url is reused, any other
 * needs the tarball.
 */
final class TarballResolver {

    private final PackageRequest request;
    private final String url;

    TarballResolver(PackageRequest request, String fragment) {
        this.request = request;
        this.url = fragment.split("#", -1)[0];
    }

    Promise<Manifest> resolve() {
        Manifest shrunk = request.getLocked("tarball");
        if (shrunk != null) {
            return Promise.resolved(request.resolver.loop, shrunk);
        }
        throw new CannotKnow("remote tarball " + url);
    }
}
