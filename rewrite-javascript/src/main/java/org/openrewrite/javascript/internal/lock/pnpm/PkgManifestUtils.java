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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Port of @pnpm/pkg-manifest.utils src/convertEnginesRuntimeToDependencies.ts (pnpm 11.2.2).
 */
final class PkgManifestUtils {

    static final List<String> RUNTIME_NAMES = Arrays.asList("node", "deno", "bun");

    private PkgManifestUtils() {
    }

    static void convertEnginesRuntimeToDependencies(Map<String, @Nullable Object> manifest, String enginesFieldName,
                                                    String dependenciesFieldName) {
        convertEnginesRuntimeToDependencies(manifest, enginesFieldName, dependenciesFieldName, PnpmRelease.PORTED);
    }

    /**
     * Adds {@code <runtime>: runtime:<version>} to the dependencies field for each runtime the engines field asks
     * pnpm to download: from 11.9.0 a version that is a string, trimmed ({@link PnpmRelease#trimsRuntimeVersions});
     * before, any truthy one as it is, asked where the two differ.
     */
    @SuppressWarnings("unchecked")
    static void convertEnginesRuntimeToDependencies(Map<String, @Nullable Object> manifest, String enginesFieldName,
                                                    String dependenciesFieldName, PnpmRelease release) {
        for (String runtimeName : RUNTIME_NAMES) {
            Object enginesFieldRuntime = property(manifest.get(enginesFieldName), "runtime");
            if (enginesFieldRuntime == null || enginesFieldRuntime == Js.UNDEFINED ||
                Js.truthy(property(manifest.get(dependenciesFieldName), runtimeName))) {
                continue;
            }
            List<@Nullable Object> runtimes = enginesFieldRuntime instanceof List ?
                    (List<@Nullable Object>) enginesFieldRuntime : Collections.singletonList(enginesFieldRuntime);
            Object runtime = find(runtimes, runtimeName);
            if (!"download".equals(property(runtime, "onFail"))) {
                continue;
            }
            Object version = property(runtime, "version");
            String asIs = Js.truthy(version) ? Js.string(version) : null;
            String trimmed = version instanceof String ? EcmaScript.trim((String) version) : null;
            String runtimeVersion = Objects.equals(asIs, trimmed) || !release.trimsRuntimeVersions() ? asIs : trimmed;
            if (runtimeVersion == null) {
                continue;
            }
            Object deps = manifest.get(dependenciesFieldName);
            if (deps == null || deps == Js.UNDEFINED) {
                deps = new LinkedHashMap<String, @Nullable Object>();
                manifest.put(dependenciesFieldName, deps);
            }
            if (!(deps instanceof Map)) {
                throw new CannotKnow("what pnpm makes of " + dependenciesFieldName + " (" + Js.typeOf(deps) +
                                     ", where pnpm defines a property)");
            }
            ((Map<String, @Nullable Object>) deps).put(runtimeName, "runtime:" + runtimeVersion);
        }
    }

    static void applyRuntimeOnFailOverride(Map<String, @Nullable Object> manifest, Object onFailOverride) {
        applyRuntimeOnFailOverride(manifest, onFailOverride, PnpmRelease.PORTED);
    }

    /**
     * The {@code runtimeOnFail} setting over every runtime of both engines fields.
     */
    @SuppressWarnings("unchecked")
    static void applyRuntimeOnFailOverride(Map<String, @Nullable Object> manifest, Object onFailOverride,
                                           PnpmRelease release) {
        for (String[] fields : new String[][]{{"devEngines", "devDependencies"}, {"engines", "dependencies"}}) {
            Object enginesFieldRuntime = property(manifest.get(fields[0]), "runtime");
            if (enginesFieldRuntime == null || enginesFieldRuntime == Js.UNDEFINED) {
                continue;
            }
            List<@Nullable Object> runtimes = enginesFieldRuntime instanceof List ?
                    (List<@Nullable Object>) enginesFieldRuntime : Collections.singletonList(enginesFieldRuntime);
            for (Object runtime : runtimes) {
                if (runtime instanceof Map) {
                    ((Map<String, @Nullable Object>) runtime).put("onFail", onFailOverride);
                } else if (!(runtime instanceof List)) {
                    // pnpm's bundle is an ES module, where a property set on a primitive throws
                    throw new JsError(JsError.ERROR, runtime == null || runtime == Js.UNDEFINED ?
                            "Cannot set properties of " + Js.string(runtime) + " (setting 'onFail')" :
                            "Cannot create property 'onFail' on " + Js.typeOf(runtime) + " '" + Js.string(runtime) +
                            "'");
                }
            }
            if (!"download".equals(onFailOverride)) {
                Object deps = manifest.get(fields[1]);
                if (Js.truthy(deps) && deps instanceof Map) {
                    for (String runtimeName : RUNTIME_NAMES) {
                        Object dep = ((Map<String, @Nullable Object>) deps).get(runtimeName);
                        // from 11.15.1 only a runtime the engines field names
                        // (PnpmRelease#dropsOnlyTheRuntimesTheEnginesFieldNames), asked of one it does not
                        if (dep instanceof String && ((String) dep).startsWith("runtime:") &&
                            (find(runtimes, runtimeName) != null ||
                             !release.dropsOnlyTheRuntimesTheEnginesFieldNames())) {
                            ((Map<String, @Nullable Object>) deps).remove(runtimeName);
                        }
                    }
                }
            } else {
                convertEnginesRuntimeToDependencies(manifest, fields[0], fields[1], release);
            }
        }
    }

    // runtimes.find((runtime) => runtime.name === runtimeName)
    private static @Nullable Object find(List<@Nullable Object> runtimes, String runtimeName) {
        for (Object runtime : runtimes) {
            if (runtime == null || runtime == Js.UNDEFINED) {
                throw new JsError(JsError.ERROR,
                        "Cannot read properties of " + Js.string(runtime) + " (reading 'name')");
            }
            if (runtimeName.equals(property(runtime, "name"))) {
                return runtime;
            }
        }
        return null;
    }

    // value?.[key] for the values a package.json holds
    @SuppressWarnings("unchecked")
    private static @Nullable Object property(@Nullable Object value, String key) {
        return value instanceof Map ? ((Map<String, @Nullable Object>) value).get(key) : null;
    }
}
