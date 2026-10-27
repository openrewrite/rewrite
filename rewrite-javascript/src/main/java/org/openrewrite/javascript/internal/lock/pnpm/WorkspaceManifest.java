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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsYaml;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Port of @pnpm/workspace.workspace-manifest-reader src/index.ts and src/catalogs.ts
 * (pnpm 11.2.2), with src/versioning.ts of 11.13.0.
 */
final class WorkspaceManifest {

    private WorkspaceManifest() {
    }

    /**
     * {@code readWorkspaceManifest}: null for an absent or empty file; pnpm's INVALID_WORKSPACE_CONFIGURATION for one
     * it rejects, a {@link JsYaml.YamlException} for one that is not YAML.
     */
    static @Nullable Map<String, @Nullable Object> readWorkspaceManifest(@Nullable String text) {
        return readWorkspaceManifest(text, PnpmRelease.PORTED);
    }

    /**
     * {@link #readWorkspaceManifest(String)} as {@code release} reads it.
     */
    static @Nullable Map<String, @Nullable Object> readWorkspaceManifest(@Nullable String text,
                                                                        PnpmRelease release) {
        if (text == null) {
            return null;
        }
        Object manifest = JsYaml.load(text);
        return validateWorkspaceManifest(manifest, release);
    }

    static @Nullable Map<String, @Nullable Object> validateWorkspaceManifest(@Nullable Object manifest) {
        return validateWorkspaceManifest(manifest, PnpmRelease.PORTED);
    }

    /**
     * {@link #validateWorkspaceManifest(Object)} as {@code release} validates it: from 11.13.0 the versioning settings
     * too ({@link PnpmRelease#validatesTheVersioningSettings}), asked where they are invalid.
     */
    @SuppressWarnings("unchecked")
    static @Nullable Map<String, @Nullable Object> validateWorkspaceManifest(@Nullable Object manifest,
                                                                            PnpmRelease release) {
        if (manifest == null || manifest == Js.UNDEFINED) {
            return null;
        }
        if (manifest instanceof List) {
            throw invalid("Expected object but found - array");
        }
        if (!(manifest instanceof Map)) {
            throw invalid("Expected object but found - " + Js.typeOf(manifest));
        }
        Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) manifest;
        if (map.isEmpty()) {
            return map;
        }
        assertValidWorkspaceManifestPackages(map);
        assertValidWorkspaceManifestCatalog(map);
        assertValidWorkspaceManifestCatalogs(map);
        PnpmError invalidVersioning = versioningError(map);
        if (invalidVersioning != null && release.validatesTheVersioningSettings()) {
            throw invalidVersioning;
        }
        return map;
    }

    private static final List<String> BUMP_TYPES = Arrays.asList("patch", "minor", "major");
    private static final List<String> CHANGELOG_STORAGE_MODES = Arrays.asList("registry", "repository");

    // versioning.ts' assertValidWorkspaceManifestVersioning (11.13.0): the error it throws, null where it throws none
    @SuppressWarnings("unchecked")
    private static @Nullable PnpmError versioningError(Map<String, @Nullable Object> manifest) {
        Object raw = manifest.get("versioning");
        if (!isSet(raw)) {
            return null;
        }
        try {
            Map<String, @Nullable Object> versioning = assertPlainObject(raw, "versioning");
            Object fixed = versioning.get("fixed");
            if (isSet(fixed)) {
                if (!(fixed instanceof List)) {
                    throw invalid("Expected versioning.fixed to be an array of arrays, but found - " +
                                  Js.typeOf(fixed));
                }
                for (Object group : (List<?>) fixed) {
                    if (!(group instanceof List) || !packageNames((List<?>) group)) {
                        throw invalid("Expected every versioning.fixed group to be an array of package names");
                    }
                }
            }
            Object epics = versioning.get("epics");
            if (isSet(epics)) {
                if (!(epics instanceof List)) {
                    throw invalid("Expected versioning.epics to be an array, but found - " + Js.typeOf(epics));
                }
                for (Object epic : (List<?>) epics) {
                    Map<String, @Nullable Object> entry = assertPlainObject(epic, "versioning.epics entry");
                    Object lead = entry.get("lead");
                    if (!(lead instanceof String) || ((String) lead).isEmpty()) {
                        throw invalid("Expected every versioning.epics entry to have a non-empty \"lead\" package " +
                                      "reference");
                    }
                    Object packages = entry.get("packages");
                    if (!(packages instanceof List) || ((List<?>) packages).isEmpty() ||
                        !packageNames((List<?>) packages)) {
                        throw invalid("Expected versioning.epics entry for \"" + lead + "\" to have a non-empty " +
                                      "\"packages\" array of selector strings");
                    }
                }
            }
            Object ignore = versioning.get("ignore");
            if (isSet(ignore) && (!(ignore instanceof List) || !packageNames((List<?>) ignore))) {
                throw invalid("Expected versioning.ignore to be an array of package names");
            }
            Object maxBump = versioning.get("maxBump");
            if (isSet(maxBump) && !BUMP_TYPES.contains(maxBump)) {
                throw invalid("Expected versioning.maxBump to be one of " + String.join(", ", BUMP_TYPES) +
                              ", but found - " + Js.string(maxBump));
            }
            Object lanes = versioning.get("lanes");
            if (isSet(lanes)) {
                for (Map.Entry<String, @Nullable Object> lane :
                        Js.entries(assertPlainObject(lanes, "versioning.lanes"))) {
                    if (!(lane.getValue() instanceof String) || ((String) lane.getValue()).isEmpty()) {
                        throw invalid("Expected versioning.lanes entry for " + lane.getKey() + " to be a non-empty " +
                                      "lane name");
                    }
                    if ("main".equals(((String) lane.getValue()).toLowerCase(Locale.ROOT))) {
                        throw invalid("Invalid versioning.lanes entry for " + lane.getKey() + ": \"main\" is the " +
                                      "reserved default lane. Remove the entry instead.");
                    }
                }
            }
            Object changelog = versioning.get("changelog");
            if (isSet(changelog)) {
                Map<String, @Nullable Object> settings = assertPlainObject(changelog, "versioning.changelog");
                Object format = settings.get("format");
                if (isSet(format) && !(format instanceof String)) {
                    throw invalid("Expected versioning.changelog.format to be a string, but found - " +
                                  Js.typeOf(format));
                }
                Object storage = settings.get("storage");
                if (isSet(storage) && !CHANGELOG_STORAGE_MODES.contains(storage)) {
                    throw invalid("Expected versioning.changelog.storage to be one of " +
                                  String.join(", ", CHANGELOG_STORAGE_MODES) + ", but found - " + Js.string(storage));
                }
            }
            return null;
        } catch (PnpmError e) {
            return e;
        }
    }

    // value != null
    private static boolean isSet(@Nullable Object value) {
        return value != null && value != Js.UNDEFINED;
    }

    // every item a non-empty string
    private static boolean packageNames(List<?> items) {
        for (Object name : items) {
            if (!(name instanceof String) || ((String) name).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // versioning.ts' assertPlainObject: an object, a date one without keys
    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> assertPlainObject(@Nullable Object value, String fieldName) {
        if (value instanceof List) {
            throw invalid("Expected " + fieldName + " field to be an object, but found - array");
        }
        if (value == null || !"object".equals(Js.typeOf(value))) {
            throw invalid("Expected " + fieldName + " field to be an object, but found - " +
                          (value == null ? "null" : Js.typeOf(value)));
        }
        return value instanceof Map ? (Map<String, @Nullable Object>) value : new LinkedHashMap<>();
    }

    private static void assertValidWorkspaceManifestPackages(Map<String, @Nullable Object> manifest) {
        Object packages = manifest.get("packages");
        if (!Js.truthy(packages)) {
            return;
        }
        if (!(packages instanceof List)) {
            throw invalid("packages field is not an array");
        }
        for (Object pkg : (List<?>) packages) {
            if (!Js.truthy(pkg)) {
                throw invalid("Missing or empty package");
            }
            if (!(pkg instanceof String)) {
                throw invalid("Invalid package type - " + Js.typeOf(pkg));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertValidWorkspaceManifestCatalog(Map<String, @Nullable Object> manifest) {
        Object catalog = manifest.get("catalog");
        if (catalog == null) {
            return;
        }
        if (catalog instanceof List) {
            throw invalid("Expected catalog field to be an object, but found - array");
        }
        if (!(catalog instanceof Map)) {
            throw invalid("Expected catalog field to be an object, but found - " + Js.typeOf(catalog));
        }
        for (Map.Entry<String, @Nullable Object> entry : ((Map<String, @Nullable Object>) catalog).entrySet()) {
            if (!(entry.getValue() instanceof String)) {
                throw invalid("Invalid catalog entry for " + entry.getKey() + ". Expected string, but found: " +
                              Js.typeOf(entry.getValue()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertValidWorkspaceManifestCatalogs(Map<String, @Nullable Object> manifest) {
        Object catalogs = manifest.get("catalogs");
        if (catalogs == null) {
            return;
        }
        if (catalogs instanceof List) {
            throw invalid("Expected catalogs field to be an object, but found - array");
        }
        if (!(catalogs instanceof Map)) {
            throw invalid("Expected catalogs field to be an object, but found - " + Js.typeOf(catalogs));
        }
        for (String catalogName : Js.keys((Map<String, @Nullable Object>) catalogs)) {
            Object catalog = ((Map<String, @Nullable Object>) catalogs).get(catalogName);
            if (catalog instanceof List) {
                throw invalid("Expected named catalog " + catalogName + " to be an object, but found - array");
            }
            if (catalog == null) {
                throw invalid("Expected named catalog " + catalogName + " to be an object, but found - null");
            }
            if (!(catalog instanceof Map)) {
                throw invalid("Expected named catalog " + catalogName + " to be an object, but found - " +
                              Js.typeOf(catalog));
            }
            for (Map.Entry<String, @Nullable Object> entry : ((Map<String, @Nullable Object>) catalog).entrySet()) {
                if (!(entry.getValue() instanceof String)) {
                    throw invalid("Catalog '" + catalogName + "' has invalid entry '" + entry.getKey() +
                                  "'. Expected string specifier, but found: " + Js.typeOf(entry.getValue()));
                }
            }
        }
    }

    private static PnpmError invalid(String message) {
        return new PnpmError("INVALID_WORKSPACE_CONFIGURATION", message);
    }
}
