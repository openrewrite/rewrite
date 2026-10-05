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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of the synchronous normalize steps @npmcli/package-json 7 runs for {@code syncNormalize()}
 * ({@code _id}, {@code bundledDependencies}, {@code bundleDependencies}, {@code optionalDedupe},
 * {@code scripts}, {@code funding}), which arborist's {@code Node} applies to its package.
 */
final class PackageJson {

    private PackageJson() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> syncNormalize(Map<String, Object> data) {
        Object name = data.get("name");
        Object version = data.get("version");
        if (Node.truthy(name) && Node.truthy(version)) {
            data.put("_id", name + "@" + version);
        }

        if (!data.containsKey("bundleDependencies") && data.containsKey("bundledDependencies")) {
            data.put("bundleDependencies", data.get("bundledDependencies"));
        }
        data.remove("bundledDependencies");

        Object bd = data.get("bundleDependencies");
        if (Boolean.FALSE.equals(bd)) {
            data.put("bundleDependencies", new ArrayList<>());
        } else if (Boolean.TRUE.equals(bd)) {
            Object deps = data.get("dependencies");
            data.put("bundleDependencies", deps instanceof Map ? new ArrayList<>(((Map<String, Object>) deps).keySet()) : new ArrayList<>());
        } else if (bd instanceof Map) {
            data.put("bundleDependencies", new ArrayList<>(((Map<String, Object>) bd).keySet()));
        } else if (!(bd instanceof List) && data.containsKey("bundleDependencies")) {
            data.remove("bundleDependencies");
        }

        Object dependencies = data.get("dependencies");
        Object optionalDependencies = data.get("optionalDependencies");
        if (Node.truthy(dependencies) && optionalDependencies instanceof Map && dependencies instanceof Map) {
            Map<String, Object> prod = (Map<String, Object>) dependencies;
            for (String optional : ((Map<String, Object>) optionalDependencies).keySet()) {
                prod.remove(optional);
            }
            if (prod.isEmpty()) {
                data.remove("dependencies");
            }
        }

        if (data.containsKey("scripts") && data.get("scripts") != null) {
            Object scripts = data.get("scripts");
            if (scripts instanceof Map) {
                ((Map<String, Object>) scripts).values().removeIf(script -> !(script instanceof String));
            } else {
                data.remove("scripts");
            }
        }

        Object funding = data.get("funding");
        if (funding instanceof String && !((String) funding).isEmpty()) {
            Map<String, Object> url = new LinkedHashMap<>();
            url.put("url", funding);
            data.put("funding", url);
        }
        return data;
    }
}
