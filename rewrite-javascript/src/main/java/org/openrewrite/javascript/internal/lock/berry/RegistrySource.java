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

import org.jspecify.annotations.Nullable;

/**
 * What a Yarn Berry install reads from the network: full packuments ({@code GET <registry>/<name>} with no
 * abbreviated Accept header) and tarballs.
 */
public interface RegistrySource {

    /**
     * The packument's text, null when the registry answers 404.
     */
    @Nullable
    String packument(String registry, String name);

    /**
     * The tarball at the url, which holds the package {@code name} at {@code version}; null when the server answers
     * 404.
     */
    byte @Nullable [] tarball(String url, String name, String version);
}
