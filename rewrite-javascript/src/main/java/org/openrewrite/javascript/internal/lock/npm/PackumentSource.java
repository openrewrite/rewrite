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
package org.openrewrite.javascript.internal.lock.npm;

import java.util.Map;

/**
 * The registry documents a build reads, parsed as {@code JSON.parse} would, once per document per build.
 */
public interface PackumentSource {

    /**
     * {@code GET /<name>}: the abbreviated packument, or the full one (with {@code time}) when {@code fullMetadata}.
     * @throws NpmRejection {@code E404} when the registry has no such package
     */
    Map<String, Object> packument(String registry, String name, boolean fullMetadata);

    /**
     * {@code GET /<name>/<version>}: the version document, which holds what the full packument's entry does
     * but in the registry's own key order.
     */
    Map<String, Object> manifest(String registry, String name, String version);
}
