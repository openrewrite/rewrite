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
import java.util.Map;

/**
 * Port of @pnpm/config.reader src/projectConfig.ts (pnpm 11.2.2): {@code packageConfigs} as each project's settings.
 */
final class ProjectConfigs {

    private static final List<String> PROJECT_CONFIG_FIELDS = Arrays.asList("hoist", "modulesDir", "overrides",
            "saveExact", "savePrefix");

    private ProjectConfigs() {
    }

    /**
     * {@code createProjectConfigRecord}: each project's settings by project name; null without packageConfigs.
     */
    static @Nullable Map<String, Map<String, @Nullable Object>> createProjectConfigRecord(
            @Nullable Object configSet) {
        if (configSet == null || configSet == Js.UNDEFINED) {
            return null;
        }
        if (!isObject(configSet) && !(configSet instanceof List)) {
            throw new PnpmError("PROJECT_CONFIGS_IS_NEITHER_OBJECT_NOR_ARRAY",
                    "Expecting packageConfigs to be either an object or an array but received " +
                    Js.jsonStringify(configSet));
        }
        Map<String, Map<String, @Nullable Object>> result = new LinkedHashMap<>();
        if (!(configSet instanceof List)) {
            Map<String, @Nullable Object> record = properties(configSet);
            for (String projectName : Js.keys(record)) {
                result.put(projectName, createProjectConfigFromRaw(record.get(projectName)));
            }
            return result;
        }
        for (Object item : (List<?>) configSet) {
            if (!isObject(item)) {
                throw new PnpmError("PROJECT_CONFIGS_ARRAY_ITEM_IS_NOT_AN_OBJECT",
                        "Expecting a packageConfigs item to be an object but received " + Js.jsonStringify(item));
            }
            Map<String, @Nullable Object> map = properties(item);
            if (!map.containsKey("match")) {
                throw new PnpmError("PROJECT_CONFIGS_ARRAY_ITEM_MATCH_IS_NOT_DEFINED",
                        "A packageConfigs match is not defined");
            }
            Object match = map.get("match");
            if (!(match instanceof List)) {
                throw new PnpmError("PROJECT_CONFIGS_ARRAY_ITEM_MATCH_IS_NOT_AN_ARRAY",
                        "Expecting a packageConfigs match to be an array but received " + Js.jsonStringify(match));
            }
            Map<String, @Nullable Object> withoutMatch = new LinkedHashMap<>(map);
            withoutMatch.remove("match");
            Map<String, @Nullable Object> projectConfig = createProjectConfigFromRaw(withoutMatch);
            for (Object projectName : (List<?>) match) {
                if (!(projectName instanceof String)) {
                    throw new PnpmError("PROJECT_CONFIGS_MATCH_ITEM_IS_NOT_A_STRING",
                            "Expecting a match item to be a string but received " + Js.jsonStringify(projectName));
                }
                result.put((String) projectName, projectConfig);
            }
        }
        return result;
    }

    private static Map<String, @Nullable Object> createProjectConfigFromRaw(@Nullable Object config) {
        if (!isObject(config)) {
            throw new PnpmError("PROJECT_CONFIG_NOT_AN_OBJECT",
                    "Expecting project-specific config to be an object, but received " + Js.jsonStringify(config));
        }
        Map<String, @Nullable Object> map = properties(config);
        checkType(map, "hoist", "boolean");
        checkType(map, "modulesDir", "string");
        checkType(map, "saveExact", "boolean");
        checkType(map, "savePrefix", "string");
        if (isDefined(map, "overrides") && !isObject(map.get("overrides"))) {
            throw invalidValueType("object", map.get("overrides"));
        }
        for (String key : Js.keys(map)) {
            if (isDefined(map, key) && !PROJECT_CONFIG_FIELDS.contains(key)) {
                throw new PnpmError("PROJECT_CONFIG_UNSUPPORTED_FIELD",
                        "Field " + key + " is not supported but was specified");
            }
        }
        if (Boolean.FALSE.equals(map.get("hoist"))) {
            Map<String, @Nullable Object> result = new LinkedHashMap<>(map);
            result.put("hoistPattern", Js.UNDEFINED);
            return result;
        }
        return map;
    }

    // typeof 'object', not null and not an array: a mapping, or the Date js-yaml makes of a timestamp
    static boolean isObject(@Nullable Object value) {
        return value instanceof Map || value instanceof JsYaml.Timestamp;
    }

    // a Date's own enumerable properties: none
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> properties(@Nullable Object object) {
        return object instanceof Map ? (Map<String, @Nullable Object>) object : new LinkedHashMap<>();
    }

    private static void checkType(Map<String, @Nullable Object> config, String field, String expectedType) {
        if (isDefined(config, field) && !expectedType.equals(Js.typeOf(config.get(field)))) {
            throw invalidValueType(expectedType, config.get(field));
        }
    }

    private static boolean isDefined(Map<String, @Nullable Object> config, String field) {
        return config.containsKey(field) && config.get(field) != Js.UNDEFINED;
    }

    private static PnpmError invalidValueType(String expectedType, @Nullable Object actualValue) {
        return new PnpmError("PROJECT_CONFIG_INVALID_VALUE_TYPE", "Expecting a value of type " + expectedType +
                " but received a value of type " + Js.typeOf(actualValue) + ": " + Js.jsonStringify(actualValue));
    }

    /**
     * recursive.ts {@code getProjectConfig}: the settings packageConfigs gives the project of this name.
     */
    static @Nullable Map<String, @Nullable Object> getProjectConfig(
            @Nullable Map<String, Map<String, @Nullable Object>> record, @Nullable Object name) {
        if (record == null || !Js.truthy(name)) {
            return null;
        }
        return record.get(Js.string(name));
    }
}
