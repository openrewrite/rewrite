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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * js-yaml's {@code safeLoad(text, {schema: FAILSAFE_SCHEMA})}, the fallback yarn's lockfile parser takes: every
 * scalar a string, mappings and sequences as objects and arrays, a duplicated key an error. SnakeYAML parses the
 * text in js-yaml's place.
 */
final class JsYaml {

    private JsYaml() {
    }

    static @Nullable Object safeLoadFailsafe(String text) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setCodePointLimit(Integer.MAX_VALUE);
        DumperOptions dumper = new DumperOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options), new Representer(dumper), dumper, options, new Resolver() {
            @Override
            protected void addImplicitResolvers() {
            }
        });
        try {
            return failsafe(yaml.load(text));
        } catch (YAMLException e) {
            throw new JsError("YAMLException", String.valueOf(e.getMessage()));
        }
    }

    private static @Nullable Object failsafe(@Nullable Object value) {
        if (value instanceof Map) {
            Map<String, @Nullable Object> object = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                object.put(Js.string(failsafe(entry.getKey())), failsafe(entry.getValue()));
            }
            Map<String, @Nullable Object> ordered = new LinkedHashMap<>();
            for (String key : Js.keys(object)) {
                ordered.put(key, object.get(key));
            }
            return ordered;
        }
        if (value instanceof List) {
            List<@Nullable Object> array = new ArrayList<>();
            for (Object item : (List<?>) value) {
                array.add(failsafe(item));
            }
            return array;
        }
        return value == null ? null : value.toString();
    }
}
