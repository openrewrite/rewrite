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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.LocaleCompare;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/update-root-package-json.js (npm 7.18.1), with the package.json reify.js writes itself
 * before npm 7.0.15: the root package.json a plain install leaves, which npm before 7.14.0 writes back on every install
 * ({@code [_saveIdealTree]}), its dependency fields taken from the root's package as arborist read it. The next
 * install reads it.
 */
final class UpdateRootPackageJson {

    private static final List<String> DEP_TYPES = Arrays.asList(
            "dependencies", "optionalDependencies", "devDependencies", "peerDependencies");

    private UpdateRootPackageJson() {
    }

    /**
     * The text the install writes over the root package.json.
     *
     * @param pkg          the root's package as arborist read it
     * @param originalJson the root package.json the install read
     * @param indent       the indent {@code json-parse-even-better-errors} noted in it, null for none
     * @param newline      the newline it noted, null for none
     */
    static String updateRootPackageJson(Map<String, Object> pkg, String originalJson, @Nullable String indent,
                                        @Nullable String newline, NpmRelease release) {
        Map<String, Object> content;
        if (!release.updatesRootPackageJsonDependencies()) {
            // reify.js: orderDeps({ ...this.idealTree.package, _id: undefined })
            content = orderDeps(new LinkedHashMap<>(pkg));
            content.put("_id", Js.UNDEFINED);
        } else {
            Map<String, Object> originalContent = parseJsonSafe(originalJson);

            Map<String, Object> depsData = orderDeps(new LinkedHashMap<>(pkg));

            Object dependencies = depsData.get("dependencies");
            if (release.prunesRepeatedOptionalsOfRootPackageJson() && truthy(dependencies)) {
                Object optionalDependencies = depsData.get("optionalDependencies");
                if (truthy(optionalDependencies)) {
                    for (String name : Js.keys(optionalDependencies)) {
                        Js.deleteSloppy(dependencies, name);
                    }
                }
                if (Js.keys(dependencies).isEmpty()) {
                    depsData.remove("dependencies");
                }
            }

            content = originalContent == null ? depsData : new LinkedHashMap<>(originalContent);

            for (String type : DEP_TYPES) {
                content.put(type, depsData.containsKey(type) ? depsData.get(type) : Js.UNDEFINED);
            }

            if (release.copiesPeerSpecsOverDependencies() && originalContent != null) {
                Object origProd = Js.get(originalContent, "dependencies");
                Object origPeer = Js.get(originalContent, "peerDependencies");
                Object newPeer = Js.get(content, "peerDependencies");
                if (truthy(origProd) && truthy(origPeer) && truthy(newPeer)) {
                    for (String name : Js.keys(origPeer)) {
                        if (Js.get(origProd, name) != Js.UNDEFINED && Js.get(newPeer, name) != Js.UNDEFINED) {
                            Object deps = content.get("dependencies");
                            if (!truthy(deps)) {
                                deps = new LinkedHashMap<String, Object>();
                                content.put("dependencies", deps);
                            }
                            assign(deps, name, Js.get(newPeer, name));
                        }
                    }
                }
            }
        }

        String format = indent == null ? "  " : indent;
        String eol = newline == null ? "\n" : newline;
        return (Js.jsonStringify(content, null, format) + "\n").replace("\n", eol);
    }

    // each kind of dependency sorted by name (String.prototype.localeCompare in the default locale)
    private static Map<String, Object> orderDeps(Map<String, Object> pkg) {
        for (String type : DEP_TYPES) {
            Object deps = pkg.get(type);
            if (truthy(deps)) {
                List<String> names = Js.keys(deps);
                names.sort(LocaleCompare::en);
                Map<String, Object> sorted = new LinkedHashMap<>();
                for (String name : names) {
                    assign(sorted, name, Js.get(deps, name));
                }
                pkg.put(type, sorted);
            }
        }
        return pkg;
    }

    // object[key] = value on a plain object, where __proto__ sets the prototype and adds no property
    @SuppressWarnings("unchecked")
    private static void assign(Object object, String key, @Nullable Object value) {
        if (object instanceof Map && !"__proto__".equals(key)) {
            ((Map<String, Object>) object).put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, Object> parseJsonSafe(String json) {
        try {
            Object value = JsonParseEvenBetterErrors.parse(json).getValue();
            return value instanceof Map ? (Map<String, Object>) value : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
