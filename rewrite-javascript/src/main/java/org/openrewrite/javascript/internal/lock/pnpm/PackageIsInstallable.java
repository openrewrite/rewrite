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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/config.package-is-installable (pnpm 11.2.2) and @pnpm/cli.utils src/packageIsInstallable.ts; a
 * platform or Node.js version the host does not report counts as satisfied.
 */
final class PackageIsInstallable {

    static final String PNPM_VERSION = Main.PNPM_VERSION;

    private PackageIsInstallable() {
    }

    @Getter
    static final class UnsupportedEngineError extends PnpmError {
        private final Map<String, @Nullable Object> wanted;
        private final Map<String, @Nullable Object> current;

        UnsupportedEngineError(String packageId, Map<String, @Nullable Object> wanted,
                               Map<String, @Nullable Object> current) {
            super("UNSUPPORTED_ENGINE", "Unsupported engine for " + packageId + ": wanted: " +
                                        Js.jsonStringify(wanted) + " (current: " + Js.jsonStringify(current) + ")");
            this.wanted = wanted;
            this.current = current;
        }
    }

    static final class UnsupportedPlatformError extends PnpmError {
        UnsupportedPlatformError(String packageId, Map<String, @Nullable Object> wanted,
                                 Map<String, @Nullable Object> current) {
            super("UNSUPPORTED_PLATFORM", "Unsupported platform for " + packageId + ": wanted " +
                                          Js.jsonStringify(wanted) + " (current: " + Js.jsonStringify(current) + ")");
        }
    }

    /**
     * index.ts {@code packageIsInstallable}: true, false for an optional package, null for a warning, or the error
     * under engineStrict.
     */
    static @Nullable Boolean packageIsInstallable(String pkgId, Map<String, @Nullable Object> pkg, boolean engineStrict,
                                                  @Nullable Object nodeVersion, boolean optional,
                                                  @Nullable Object supportedArchitectures, Host host) {
        PnpmError warn = checkPackage(pkgId, pkg, nodeVersion, null, supportedArchitectures, host);
        if (warn == null) {
            return true;
        }
        if (optional) {
            return false;
        }
        if (engineStrict) {
            throw warn;
        }
        return null;
    }

    /**
     * cli.utils' {@code packageIsInstallable} over a project's manifest: an engines.pnpm this release does not
     * satisfy throws, and under engineStrict any engine; a platform never does.
     */
    static void projectIsInstallable(String pkgPath, Map<String, @Nullable Object> pkg, boolean engineStrict,
                                     @Nullable Object nodeVersion, @Nullable Object supportedArchitectures,
                                     Host host) {
        PnpmError err = checkPackage(pkgPath, pkg, nodeVersion, PNPM_VERSION, supportedArchitectures, host);
        if (err instanceof UnsupportedEngineError &&
            (((UnsupportedEngineError) err).getWanted().containsKey("pnpm") || engineStrict)) {
            throw err;
        }
    }

    /**
     * index.ts {@code checkPackage}.
     */
    static @Nullable PnpmError checkPackage(String pkgId, Map<String, @Nullable Object> manifest,
                                            @Nullable Object nodeVersion, @Nullable String pnpmVersion,
                                            @Nullable Object supportedArchitectures, Host host) {
        Map<String, @Nullable Object> wantedPlatform = new LinkedHashMap<>();
        wantedPlatform.put("cpu", orAny(manifest.get("cpu")));
        wantedPlatform.put("os", orAny(manifest.get("os")));
        wantedPlatform.put("libc", orAny(manifest.get("libc")));
        PnpmError platformError = checkPlatform(pkgId, wantedPlatform, supportedArchitectures, host);
        if (platformError != null) {
            return platformError;
        }
        Object engines = manifest.get("engines");
        if (engines == null || engines == Js.UNDEFINED) {
            return null;
        }
        return checkEngine(pkgId, engines, nodeVersion != null ? nodeVersion : host.getNodeVersion(), pnpmVersion);
    }

    private static Object orAny(@Nullable Object value) {
        return value == null || value == Js.UNDEFINED ? Collections.singletonList("any") : value;
    }

    /**
     * checkEngine.ts {@code checkEngine}; a null {@code node} is a version the host does not report, and one that is
     * not a string is the nodeVersion setting as YAML gave it.
     */
    static @Nullable UnsupportedEngineError checkEngine(String packageId, @Nullable Object wantedEngine,
                                                        @Nullable Object node, @Nullable String pnpm) {
        if (!Js.truthy(wantedEngine)) {
            return null;
        }
        Map<String, @Nullable Object> unsatisfiedWanted = new LinkedHashMap<>();
        Object wantedNode = property(wantedEngine, "node");
        if (node != null && Js.truthy(wantedNode) && !satisfies(node, wantedNode)) {
            if (!(node instanceof String) || NodeSemver.valid((String) node, NodeSemver.Options.DEFAULT) == null) {
                throw new PnpmError("INVALID_NODE_VERSION", "The nodeVersion setting is \"" + Js.string(node) +
                                                            "\", which is not exact semver version");
            }
            unsatisfiedWanted.put("node", wantedNode);
        }
        Object wantedPnpm = property(wantedEngine, "pnpm");
        if (Js.truthy(pnpm) && Js.truthy(wantedPnpm) &&
            !NodeSemver.satisfies(pnpm, wantedPnpm, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            unsatisfiedWanted.put("pnpm", wantedPnpm);
        }
        if (unsatisfiedWanted.isEmpty()) {
            return null;
        }
        Map<String, @Nullable Object> current = new LinkedHashMap<>();
        current.put("node", node);
        if (pnpm != null) {
            current.put("pnpm", pnpm);
        }
        return new UnsupportedEngineError(packageId, unsatisfiedWanted, current);
    }

    // semver.satisfies: false for a range Range rejects; a version that is not a string throws semver's TypeError
    private static boolean satisfies(Object version, Object range) {
        if (!(range instanceof String)) {
            return false;
        }
        try {
            return NodeSemver.satisfies(version, (String) range, NodeSemver.Options.INCLUDE_PRERELEASE);
        } catch (IllegalArgumentException e) {
            throw new JsError(JsError.ERROR, e.getMessage());
        }
    }

    /**
     * checkPlatform.ts {@code checkPlatform}; the host's libc is detect-libc's family, null where it detects none.
     */
    static @Nullable UnsupportedPlatformError checkPlatform(String packageId, Map<String, @Nullable Object> wanted,
                                                            @Nullable Object supportedArchitectures, Host host) {
        String currentLibc = host.getLibc() == null ? "unknown" : host.getLibc();
        List<@Nullable Object> os = dedupeCurrent(host.getOs(), property(supportedArchitectures, "os"));
        List<@Nullable Object> cpu = dedupeCurrent(host.getCpu(), property(supportedArchitectures, "cpu"));
        List<@Nullable Object> libc = dedupeCurrent(currentLibc, property(supportedArchitectures, "libc"));
        boolean osOk = true;
        boolean cpuOk = true;
        boolean libcOk = true;
        if (Js.truthy(wanted.get("os"))) {
            osOk = os == null || checkList(os, wanted.get("os"));
        }
        if (Js.truthy(wanted.get("cpu"))) {
            cpuOk = cpu == null || checkList(cpu, wanted.get("cpu"));
        }
        if (Js.truthy(wanted.get("libc")) && !"unknown".equals(currentLibc)) {
            libcOk = libc == null || checkList(libc, wanted.get("libc"));
        }
        if (!osOk || !cpuOk || !libcOk) {
            Map<String, @Nullable Object> current = new LinkedHashMap<>();
            current.put("os", host.getOs());
            current.put("cpu", host.getCpu());
            current.put("libc", currentLibc);
            return new UnsupportedPlatformError(packageId, wanted, current);
        }
        return null;
    }

    private static boolean checkList(List<@Nullable Object> values, @Nullable Object wanted) {
        List<@Nullable Object> list;
        if (wanted instanceof String) {
            list = Collections.singletonList(wanted);
        } else if (wanted instanceof List) {
            @SuppressWarnings("unchecked")
            List<@Nullable Object> items = (List<@Nullable Object>) wanted;
            list = items;
        } else {
            throw new JsError(JsError.ERROR, "list.filter is not a function");
        }
        List<String> strings = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof String) {
                strings.add((String) entry);
            }
        }
        if (strings.size() == 1 && "any".equals(strings.get(0))) {
            return true;
        }
        boolean match = false;
        for (Object value : values) {
            for (String entry : strings) {
                if (entry.startsWith("!")) {
                    if (entry.substring(1).equals(value)) {
                        return false;
                    }
                } else {
                    match = match || entry.equals(value);
                }
            }
        }
        if (match) {
            return true;
        }
        for (String entry : strings) {
            if (!entry.startsWith("!")) {
                return false;
            }
        }
        return true;
    }

    // null when a `current` names what the host does not report
    private static @Nullable List<@Nullable Object> dedupeCurrent(@Nullable String current,
                                                                  @Nullable Object supported) {
        List<?> list = supported == null ? Collections.singletonList("current") : null;
        if (supported instanceof List) {
            list = (List<?>) supported;
        } else if (supported != null) {
            throw new JsError(JsError.ERROR, "supported.map is not a function");
        }
        List<@Nullable Object> values = new ArrayList<>();
        for (Object value : list) {
            if ("current".equals(value)) {
                if (current == null) {
                    return null;
                }
                values.add(current);
            } else {
                values.add(value);
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object property(@Nullable Object value, String key) {
        Object property = value instanceof Map ? ((Map<String, @Nullable Object>) value).get(key) : null;
        return property == Js.UNDEFINED ? null : property;
    }
}
