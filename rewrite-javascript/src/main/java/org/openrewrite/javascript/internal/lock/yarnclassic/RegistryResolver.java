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

import org.openrewrite.javascript.internal.lock.EventLoop.Promise;

/**
 * Port of yarn src/resolvers/exotics/registry-resolver.js (yarn 1.22.22): an {@code npm:} or {@code yarn:} alias,
 * resolved as the real name's range on the registry.
 */
final class RegistryResolver {

    private final PackageRequest request;
    private final ExoticResolvers.RegistryFragment fragment;

    RegistryResolver(PackageRequest request, String fragment) {
        this.request = request;
        this.fragment = ExoticResolvers.parseRegistryFragment(fragment);
    }

    Promise<Manifest> resolve() {
        return new NpmResolver(request, fragment.getName(), fragment.getRange()).resolve();
    }
}
