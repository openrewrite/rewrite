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
package org.openrewrite.javascript.internal.lock;

import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.Set;

/**
 * The registry surface dependency resolution needs: the published versions of a package and a single version's manifest.
 * Decouples the resolution algorithm from {@code NpmRegistryClient}/HTTP so graph builders unit-test against a
 * fake with no network.
 */
public interface Registry {

    /** Every published version of {@code name}. */
    Set<String> versions(String name);

    /** The manifest for a specific {@code name@version}. */
    VersionManifest manifest(String name, String version);
}
