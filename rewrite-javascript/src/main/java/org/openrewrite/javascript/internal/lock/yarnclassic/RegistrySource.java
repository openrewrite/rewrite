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

/**
 * What yarn requests from a registry: packuments, and the tarballs of the versions it picks anew.
 */
public interface RegistrySource {

    /**
     * {@code GET <registry><name>} with yarn's {@code Accept} header: the abbreviated packument, or null when the
     * registry has no such package.
     */
    @Nullable
    String packument(String registry, String name);

    /**
     * {@code GET} of a version's {@code dist.tarball}: its bytes, or null when the registry answers 404.
     */
    byte @Nullable [] tarball(String url, String name, String version);
}
