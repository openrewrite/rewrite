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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EcmaScript.JsonSyntaxError;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.StripBom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @pnpm/workspace.project-manifest-reader (pnpm 11.2.2): a package.json parsed as {@code JSON.parse} would
 * and converted as every read converts it.
 */
final class Manifests {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
    private static final ObjectReader MEMBER = MAPPER.readerFor(Object.class)
            .without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private Manifests() {
    }

    static Map<String, @Nullable Object> readProjectManifest(String text, String path) {
        return readProjectManifest(text, path, PnpmRelease.PORTED);
    }

    /**
     * {@code readProjectManifest} of a package.json as {@code release} reads it: {@code readJsonFile}, then
     * {@code convertManifestAfterRead}.
     */
    static Map<String, @Nullable Object> readProjectManifest(String text, String path, PnpmRelease release) {
        return convertManifestAfterRead(parse(text, path), release);
    }

    static Map<String, @Nullable Object> readPackageYaml(String text, String path) {
        return readPackageYaml(text, path, PnpmRelease.PORTED);
    }

    /**
     * {@code readExactProjectManifest} of a package.yaml: read-yaml-file's {@code readYamlFile}, its YAMLException
     * {@code ERR_PNPM_YAML_PARSE}, then {@code convertManifestAfterRead}.
     */
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> readPackageYaml(String text, String path, PnpmRelease release) {
        Object value;
        try {
            value = JsYaml.load(StripBom.stripBom(text));
        } catch (JsYaml.YamlException e) {
            throw new PnpmError("YAML_PARSE", e.getMessage() + "\nin " + path);
        }
        if (!(value instanceof Map)) {
            throw new CannotKnow("what pnpm makes of " + path + " (YAML, but not a mapping)");
        }
        return convertManifestAfterRead((Map<String, @Nullable Object>) value, release);
    }

    static Map<String, @Nullable Object> readJson5(String text, String path) {
        return readJson5(text, path, PnpmRelease.PORTED);
    }

    /**
     * project-manifest-reader's readJson5File: json5 2.2.3's parse, {@code ERR_PNPM_JSON5_PARSE} for text it rejects,
     * then {@code convertManifestAfterRead}.
     */
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> readJson5(String text, String path, PnpmRelease release) {
        Object value;
        try {
            value = Json5.parse(StripBom.stripBom(text));
        } catch (Json5.SyntaxError e) {
            throw new PnpmError("JSON5_PARSE", e.getMessage() + " in " + path);
        }
        if (!(value instanceof Map)) {
            throw new CannotKnow("what pnpm makes of " + path + " (JSON5, but not an object)");
        }
        return convertManifestAfterRead((Map<String, @Nullable Object>) value, release);
    }

    /**
     * project-manifest-reader's createManifestWriter, which normalizes the manifest it read for every project and
     * {@code file:} directory: from 11.3.0 a dependency field of {@code null} is read as any other value
     * ({@link PnpmRelease#readsANullDependencyField}); before, {@code Object.keys(null)} throws.
     */
    static Map<String, @Nullable Object> normalizedForTheWriter(Map<String, @Nullable Object> manifest,
                                                                PnpmRelease release) {
        for (String key : new String[]{"dependencies", "devDependencies", "optionalDependencies",
                "peerDependencies"}) {
            if (manifest.containsKey(key) && manifest.get(key) == null && !release.readsANullDependencyField()) {
                throw new JsError(JsError.TYPE_ERROR, "Cannot convert undefined or null to object");
            }
        }
        return manifest;
    }

    /**
     * What the manifest writer fails on, which a recursive install runs over each project it installs once the lock
     * is written: from 11.9.0 its readDependenciesField over devDependencies, then dependencies, a field that is null,
     * an array or no object {@code ERR_PNPM_INVALID_DEPENDENCIES_FIELD}
     * ({@link PnpmRelease#rejectsANonObjectDependenciesFieldOnWrite}); before, the writer reads no such field. Null
     * where the writer fails on nothing.
     */
    static @Nullable PnpmError invalidDependenciesField(Map<String, @Nullable Object> manifest, PnpmRelease release) {
        for (String field : new String[]{"devDependencies", "dependencies"}) {
            Object dependencies = manifest.containsKey(field) ? manifest.get(field) : Js.UNDEFINED;
            if (dependencies != Js.UNDEFINED && !(dependencies instanceof Map) &&
                release.rejectsANonObjectDependenciesFieldOnWrite()) {
                return new PnpmError("INVALID_DEPENDENCIES_FIELD", "The \"" + field + "\" field must be an object.");
            }
        }
        return null;
    }

    static Map<String, @Nullable Object> convertManifestAfterRead(Map<String, @Nullable Object> manifest,
                                                                  PnpmRelease release) {
        PkgManifestUtils.convertEnginesRuntimeToDependencies(manifest, "devEngines", "devDependencies", release);
        PkgManifestUtils.convertEnginesRuntimeToDependencies(manifest, "engines", "dependencies", release);
        return manifest;
    }

    /**
     * {@code readJsonFile}: {@code ERR_PNPM_JSON_PARSE} for text that is not JSON, cannot-know for JSON that is not an
     * object.
     */
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> parse(String text, String path) {
        Object value;
        try {
            value = Js.jsonParse(StripBom.stripBom(text));
        } catch (JsonSyntaxError e) {
            throw new PnpmError("JSON_PARSE", e.getMessage() + " in " + path);
        }
        if (!(value instanceof Map)) {
            throw new CannotKnow("what pnpm makes of " + path + " (JSON, but not an object)");
        }
        return (Map<String, @Nullable Object>) value;
    }

    /**
     * {@code JSON.parse} of any JSON text.
     */
    static @Nullable Object parseValue(String text, String path) {
        try {
            return Js.jsonParse(text);
        } catch (JsonSyntaxError e) {
            throw new PnpmError("JSON_PARSE", e.getMessage() + " in " + path);
        }
    }

    /**
     * {@code JSON.parse} of a registry document as far as the caller reads it: the members {@code whole} names, and of
     * those {@code keysOnly} names an object's keys alone (each {@code true}).
     */
    static @Nullable Map<String, @Nullable Object> parseMembers(String text, String path, Set<String> whole,
                                                                Set<String> keysOnly) {
        try (JsonParser parser = MAPPER.getFactory().createParser(text)) {
            Map<String, @Nullable Object> members = new LinkedHashMap<>();
            JsonToken token = parser.nextToken();
            if (token == JsonToken.VALUE_NULL) {
                members = null;
            } else if (token != JsonToken.START_OBJECT) {
                parser.skipChildren();
            } else {
                while (parser.nextToken() == JsonToken.FIELD_NAME) {
                    String name = parser.currentName();
                    JsonToken value = parser.nextToken();
                    if (keysOnly.contains(name) && value == JsonToken.START_OBJECT) {
                        Map<String, @Nullable Object> keys = new LinkedHashMap<>();
                        while (parser.nextToken() == JsonToken.FIELD_NAME) {
                            keys.put(parser.currentName(), Boolean.TRUE);
                            parser.nextToken();
                            parser.skipChildren();
                        }
                        members.put(name, Js.ordered(keys));
                    } else if (whole.contains(name) || keysOnly.contains(name)) {
                        members.put(name, Js.jsonValue(MEMBER.readValue(parser)));
                    } else {
                        parser.skipChildren();
                    }
                }
            }
            if (parser.nextToken() != null) {
                throw new PnpmError("JSON_PARSE", "Unexpected non-whitespace character after JSON in " + path);
            }
            return members;
        } catch (IOException e) {
            throw new PnpmError("JSON_PARSE", e.getMessage() + " in " + path);
        }
    }
}
