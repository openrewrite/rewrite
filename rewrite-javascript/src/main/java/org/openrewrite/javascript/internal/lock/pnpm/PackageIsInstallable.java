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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
        return packageIsInstallable(pkgId, pkg, engineStrict, nodeVersion, optional, supportedArchitectures, host,
                PnpmRelease.PORTED);
    }

    /**
     * {@link #packageIsInstallable(String, Map, boolean, Object, boolean, Object, Host)} as {@code release} checks.
     */
    static @Nullable Boolean packageIsInstallable(String pkgId, Map<String, @Nullable Object> pkg, boolean engineStrict,
                                                  @Nullable Object nodeVersion, boolean optional,
                                                  @Nullable Object supportedArchitectures, Host host,
                                                  PnpmRelease release) {
        PnpmError warn = checkPackage(pkgId, pkg, nodeVersion, null, supportedArchitectures, host, release);
        // from 11.6.0 an optional package that does not declare its whole platform takes the rest from its name
        // (PnpmRelease#infersAnOptionalPackagesPlatformFromItsName), asked where that makes it installable otherwise
        Map<String, @Nullable Object> effective = optional ? effectivePlatform(pkg) : null;
        if (effective != null) {
            PnpmError inferred = checkPackage(pkgId, effective, nodeVersion, null, supportedArchitectures, host,
                    release);
            if ((inferred == null) != (warn == null) && release.infersAnOptionalPackagesPlatformFromItsName()) {
                warn = inferred;
            }
        }
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
     * index.ts {@code effectivePlatform} of 11.6.0 for an optional package: its engines, and the os, cpu and libc it
     * does not declare from {@link #inferPlatformFromPackageName}; null where that changes nothing.
     */
    static @Nullable Map<String, @Nullable Object> effectivePlatform(Map<String, @Nullable Object> pkg) {
        Object os = pkg.get("os");
        Object cpu = pkg.get("cpu");
        Object libc = pkg.get("libc");
        if (isPresent(os) && isPresent(cpu) && isPresent(libc)) {
            return null;
        }
        Map<String, List<String>> inferred = inferPlatformFromPackageName(pkg.get("name"));
        if (inferred == null || !isPresent(os) && !isPresent(cpu) && !isPresent(libc) &&
                                !inferred.containsKey("os")) {
            return null;
        }
        Map<String, @Nullable Object> effective = new LinkedHashMap<>();
        effective.put("engines", pkg.get("engines"));
        effective.put("os", isPresent(os) ? os : inferred.get("os"));
        effective.put("cpu", isPresent(cpu) ? cpu : inferred.get("cpu"));
        effective.put("libc", isPresent(libc) ? libc : inferred.get("libc"));
        return effective;
    }

    // `value != null`, which undefined fails too
    private static boolean isPresent(@Nullable Object value) {
        return value != null && value != Js.UNDEFINED;
    }

    private static final Pattern NAME_TOKEN_SEPARATOR = Pattern.compile("[-_.]");
    private static final Map<String, String> OS_BY_TOKEN = new LinkedHashMap<>();
    private static final Map<String, String> CPU_BY_TOKEN = new LinkedHashMap<>();
    private static final Map<String, String> LIBC_BY_TOKEN = new LinkedHashMap<>();

    static {
        String[] os = {"aix", "aix", "android", "android", "darwin", "darwin", "macos", "darwin", "osx", "darwin",
                "freebsd", "freebsd", "linux", "linux", "netbsd", "netbsd", "openbsd", "openbsd", "openharmony",
                "openharmony", "sunos", "sunos", "win32", "win32", "windows", "win32"};
        String[] cpu = {"arm", "arm", "armv6", "arm", "armv7", "arm", "arm64", "arm64", "aarch64", "arm64", "ia32",
                "ia32", "loong64", "loong64", "mips64el", "mips64el", "ppc64", "ppc64", "ppc64le", "ppc64", "riscv64",
                "riscv64", "s390x", "s390x", "x64", "x64", "amd64", "x64", "wasm32", "wasm32"};
        String[] libc = {"glibc", "glibc", "gnu", "glibc", "gnueabihf", "glibc", "musl", "musl", "musleabihf",
                "musl"};
        for (int i = 0; i < os.length; i += 2) {
            OS_BY_TOKEN.put(os[i], os[i + 1]);
        }
        for (int i = 0; i < cpu.length; i += 2) {
            CPU_BY_TOKEN.put(cpu[i], cpu[i + 1]);
        }
        for (int i = 0; i < libc.length; i += 2) {
            LIBC_BY_TOKEN.put(libc[i], libc[i + 1]);
        }
    }

    /**
     * inferPlatformFromPackageName.ts (11.6.0): the os, cpu and libc the tokens of a package's unscoped name
     * ({@code -}, {@code _} or {@code .} apart) name; null where none does.
     */
    static @Nullable Map<String, List<String>> inferPlatformFromPackageName(@Nullable Object name) {
        if (!(name instanceof String)) {
            throw new JsError(JsError.TYPE_ERROR, name == null || name == Js.UNDEFINED ?
                    "Cannot read properties of undefined (reading 'includes')" : "name.includes is not a function");
        }
        String pkgName = (String) name;
        String nameWithoutScope = pkgName.contains("/") ? pkgName.substring(pkgName.indexOf('/') + 1) : pkgName;
        String[] tokens = NAME_TOKEN_SEPARATOR.split(nameWithoutScope.toLowerCase(Locale.ROOT), -1);
        Map<String, List<String>> inferred = new LinkedHashMap<>();
        pickTokenValues(tokens, OS_BY_TOKEN, "os", inferred);
        pickTokenValues(tokens, CPU_BY_TOKEN, "cpu", inferred);
        pickTokenValues(tokens, LIBC_BY_TOKEN, "libc", inferred);
        return inferred.isEmpty() ? null : inferred;
    }

    private static void pickTokenValues(String[] tokens, Map<String, String> valueByToken, String field,
                                        Map<String, List<String>> into) {
        Set<String> values = new LinkedHashSet<>();
        for (String token : tokens) {
            String value = valueByToken.get(token);
            if (value != null) {
                values.add(value);
            }
        }
        if (!values.isEmpty()) {
            into.put(field, new ArrayList<>(values));
        }
    }

    /**
     * {@link #projectIsInstallable(String, Map, boolean, Object, Object, Host, PnpmRelease)} with 11.2.2 installing.
     */
    static void projectIsInstallable(String pkgPath, Map<String, @Nullable Object> pkg, boolean engineStrict,
                                     @Nullable Object nodeVersion, @Nullable Object supportedArchitectures,
                                     Host host) {
        projectIsInstallable(pkgPath, pkg, engineStrict, nodeVersion, supportedArchitectures, host,
                PnpmRelease.PORTED);
    }

    /**
     * cli.utils' {@code packageIsInstallable} over a project's manifest: an engines.pnpm the release that installs
     * does not satisfy throws, and under engineStrict any engine; a platform never does.
     */
    static void projectIsInstallable(String pkgPath, Map<String, @Nullable Object> pkg, boolean engineStrict,
                                     @Nullable Object nodeVersion, @Nullable Object supportedArchitectures,
                                     Host host, PnpmRelease release) {
        // the release names its own version only where the manifest's engines ask for pnpm
        Object engines = pkg.get("engines");
        String pnpmVersion = engines instanceof Map && ((Map<?, ?>) engines).containsKey("pnpm") ?
                release.version() : PNPM_VERSION;
        PnpmError err = checkPackage(pkgPath, pkg, nodeVersion, pnpmVersion, supportedArchitectures, host, release);
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
                                            @Nullable Object supportedArchitectures, Host host,
                                            PnpmRelease release) {
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
        return checkEngine(pkgId, engines, nodeVersion != null ? nodeVersion : host.getNodeVersion(), pnpmVersion,
                release);
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
        return checkEngine(packageId, wantedEngine, node, pnpm, PnpmRelease.PORTED);
    }

    /**
     * {@link #checkEngine(String, Object, Object, String)} over the node-semver {@code release} bundles.
     */
    static @Nullable UnsupportedEngineError checkEngine(String packageId, @Nullable Object wantedEngine,
                                                        @Nullable Object node, @Nullable String pnpm,
                                                        PnpmRelease release) {
        if (!Js.truthy(wantedEngine)) {
            return null;
        }
        Map<String, @Nullable Object> unsatisfiedWanted = new LinkedHashMap<>();
        Object wantedNode = property(wantedEngine, "node");
        if (node != null && Js.truthy(wantedNode) && !satisfies(node, wantedNode, release)) {
            if (!(node instanceof String) || NodeSemver.valid((String) node, NodeSemver.Options.DEFAULT) == null) {
                throw new PnpmError("INVALID_NODE_VERSION", "The nodeVersion setting is \"" + Js.string(node) +
                                                            "\", which is not exact semver version");
            }
            unsatisfiedWanted.put("node", wantedNode);
        }
        Object wantedPnpm = property(wantedEngine, "pnpm");
        if (Js.truthy(pnpm) && Js.truthy(wantedPnpm) &&
            !release.underSemver(semver -> NodeSemver.satisfies(pnpm, wantedPnpm,
                    NodeSemver.Options.INCLUDE_PRERELEASE.release(semver)))) {
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
    private static boolean satisfies(Object version, Object range, PnpmRelease release) {
        if (!(range instanceof String)) {
            return false;
        }
        return release.underSemver(semver -> {
            try {
                return NodeSemver.satisfies(version, (String) range, NodeSemver.Options.INCLUDE_PRERELEASE
                        .release(semver));
            } catch (IllegalArgumentException e) {
                throw new JsError(JsError.ERROR, e.getMessage());
            }
        });
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
