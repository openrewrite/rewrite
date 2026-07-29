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
package org.openrewrite.javascript.internal.registry;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The full single-version manifest ({@code GET /<name>/<version>}) — the byte-exact surface a lock
 * entry is built from, including fields the abbreviated packument omits (notably {@code license}).
 */
@Value
public class VersionManifest {
    String name;
    String version;

    /**
     * Raw {@code license} node: a String, a legacy {@code {type,url}} object, or (via the legacy
     * top-level {@code licenses} array) absent here. Use {@link #licenseString} for a best-effort SPDX string.
     */
    @Nullable
    JsonNode license;

    @Nullable
    String licenseString;

    @Nullable
    Map<String, String> dependencies;

    @Nullable
    Map<String, String> optionalDependencies;

    @Nullable
    Map<String, String> peerDependencies;

    /** Raw {@code peerDependenciesMeta} node ({@code {name: {optional: bool}}}); npm copies it verbatim into the lock entry. */
    @Nullable
    JsonNode peerDependenciesMeta;

    /**
     * Raw {@code bin} node: a String or a {@code {name: path}} object.
     */
    @Nullable
    JsonNode bin;

    @Nullable
    Map<String, String> engines;

    @Nullable
    List<String> os;

    @Nullable
    List<String> cpu;

    @Nullable
    List<String> libc;

    /**
     * {@code hasInstallScript}, derived from {@link #scripts} when the manifest omits it.
     */
    @Nullable
    Boolean hasInstallScript;

    @Nullable
    Map<String, String> scripts;

    /**
     * Bundled dependencies (accepting both {@code bundleDependencies} and {@code bundledDependencies}).
     */
    @Nullable
    List<String> bundleDependencies;

    @Nullable
    String deprecated;

    @Nullable
    Boolean hasShrinkwrap;

    @Nullable
    Dist dist;

    /** Raw {@code funding} node (npm copies it verbatim into the lock entry); a String, object, or array. */
    @Nullable
    JsonNode funding;

    @Nullable
    Map<String, String> acceptDependencies;

    /** Raw {@code workspaces} node, if a published manifest carries one. */
    @Nullable
    JsonNode workspaces;

    @Value
    public static class Dist {
        @Nullable
        String tarball;

        @Nullable
        String shasum;

        @Nullable
        String integrity;
    }
}
