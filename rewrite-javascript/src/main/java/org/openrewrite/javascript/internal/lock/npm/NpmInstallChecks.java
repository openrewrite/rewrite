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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of npm-install-checks 8.0.0 ({@code checkEngine}, {@code checkPlatform}, {@code checkDevEngines} and
 * {@code currentEnv.devEngines}), and of the releases npm bundled before it where they read libc otherwise. The
 * machine's own values come from a {@link Host}; an unknown host satisfies every check.
 */
final class NpmInstallChecks {

    /**
     * A value of the environment the host does not say: the comparison that reads it counts as satisfied.
     */
    static final Object UNKNOWN = new Object() {
        @Override
        public String toString() {
            return "unknown";
        }
    };

    private static final List<String> RECOGNIZED_ON_FAIL = Arrays.asList("ignore", "warn", "error", "download");
    private static final List<String> RECOGNIZED_PROPERTIES = Arrays.asList("name", "version", "onFail");
    private static final List<String> RECOGNIZED_ENGINES = Arrays.asList("packageManager", "runtime", "cpu", "libc",
            "os");

    /**
     * {@code EBADENGINE}
     */
    @Getter
    static final class UnsupportedEngine extends IllegalStateException implements NpmError {
        private final String pkgid;
        private final Map<String, @Nullable String> current;
        private final Object required;

        UnsupportedEngine(String pkgid, Map<String, @Nullable String> current, Object required) {
            super("Unsupported engine");
            this.pkgid = pkgid;
            this.current = current;
            this.required = required;
        }

        @Override
        public String getCode() {
            return "EBADENGINE";
        }
    }

    /**
     * {@code EBADPLATFORM}
     */
    @Getter
    static final class UnsupportedPlatform extends IllegalStateException implements NpmError {
        private final String pkgid;
        private final Map<String, @Nullable String> current;
        private final Map<String, @Nullable Object> required;

        UnsupportedPlatform(String pkgid, Map<String, @Nullable String> current,
                Map<String, @Nullable Object> required) {
            super("Unsupported platform");
            this.pkgid = pkgid;
            this.current = current;
            this.required = required;
        }

        @Override
        public String getCode() {
            return "EBADPLATFORM";
        }
    }

    /**
     * A {@code devEngines} entry no alternative of which the environment satisfies: the Error {@code checkDevEngines}
     * returns for it, with the properties it assigns.
     */
    @Value
    static class InvalidDevEngine {
        String message;

        /**
         * What each alternative does not match, in their order.
         */
        List<String> errors;

        String engine;

        /**
         * {@code isWarn} and {@code isError}, by the {@code onFail} of the last alternative; neither under
         * {@code ignore}.
         */
        boolean warn;
        boolean error;

        Object current;

        @Nullable
        Object required;
    }

    private NpmInstallChecks() {
    }

    /**
     * @param release the npm that bundles the library, for the node-semver it reads an engine's range with
     */
    static void checkEngine(Map<String, Object> target, @Nullable String npmVer, @Nullable String nodeVer,
                            boolean force, NpmRelease release) {
        String nodev = force ? null : nodeVer;
        Object eng = target.get("engines");
        if (!truthy(eng)) {
            return;
        }
        boolean nodeFail = truthy(nodev) && truthy(Js.get(eng, "node")) &&
                           !satisfies(nodev, Js.get(eng, "node"), release);
        boolean npmFail = truthy(npmVer) && truthy(Js.get(eng, "npm")) &&
                          !satisfies(npmVer, Js.get(eng, "npm"), release);
        if (nodeFail || npmFail) {
            Map<String, @Nullable String> current = new LinkedHashMap<>();
            current.put("node", nodeVer);
            current.put("npm", npmVer);
            throw new UnsupportedEngine(Js.string(target.get("_id")), current, eng);
        }
    }

    private static boolean satisfies(String version, @Nullable Object range, NpmRelease release) {
        return NodeSemver.satisfies(version, range, NodeSemver.Options.INCLUDE_PRERELEASE.release(release.semver()));
    }

    /**
     * @param environment the {@code os}, {@code cpu} and {@code libc} the caller overrides, if any
     * @param current     the machine's own
     * @param release     the npm that bundles the library, for whether it reads {@code libc}
     */
    static void checkPlatform(Map<String, Object> target, boolean force, Host environment, Host current,
                              NpmRelease release) {
        if (force) {
            return;
        }
        String os = truthy(environment.getOs()) ? environment.getOs() : current.getOs();
        String cpu = truthy(environment.getCpu()) ? environment.getCpu() : current.getCpu();
        // currentEnv.libc(os) detects a libc on linux only
        String libc = truthy(environment.getLibc()) ? environment.getLibc() :
                "linux".equals(os) ? current.getLibc() : null;
        if ("glibc".equals(libc) && !truthy(environment.getLibc()) && !release.detectsGlibc()) {
            // 6.1.0 reads report.header.glibcRuntimeVersion, which no process report has
            libc = null;
        }

        boolean osOk = os == null || !truthy(target.get("os")) || checkList(os, target.get("os"));
        boolean cpuOk = cpu == null || !truthy(target.get("cpu")) || checkList(cpu, target.get("cpu"));
        // a machine that reports an os but no libc has none; an unknown machine's libc is unknown
        boolean libcOk = !release.checksLibc() || !truthy(target.get("libc")) ||
                         (libc != null ? checkList(libc, target.get("libc")) : current.getOs() == null);

        if (!osOk || !cpuOk || !libcOk) {
            Map<String, @Nullable String> currentValues = new LinkedHashMap<>();
            currentValues.put("os", os);
            currentValues.put("cpu", cpu);
            Map<String, @Nullable Object> required = new LinkedHashMap<>();
            required.put("os", target.get("os"));
            required.put("cpu", target.get("cpu"));
            if (release.checksLibc()) {
                currentValues.put("libc", libc);
                required.put("libc", target.get("libc"));
            }
            throw new UnsupportedPlatform(Js.string(target.get("_id")), currentValues, required);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean checkList(String value, Object list) {
        List<Object> entries;
        if (list instanceof String) {
            entries = Collections.singletonList(list);
        } else if (list instanceof List) {
            entries = (List<Object>) list;
        } else {
            throw new IllegalArgumentException("list is not iterable");
        }
        if (entries.size() == 1 && "any".equals(entries.get(0))) {
            return true;
        }
        int negated = 0;
        boolean match = false;
        for (Object entry : entries) {
            if (entry == null || entry == Js.UNDEFINED) {
                throw new IllegalArgumentException("Cannot read properties of " + entry + " (reading 'charAt')");
            }
            if (!(entry instanceof String)) {
                throw new IllegalArgumentException("entry.charAt is not a function");
            }
            String s = (String) entry;
            boolean negate = s.startsWith("!");
            String test = negate ? s.substring(1) : s;
            if (negate) {
                negated++;
                if (value.equals(test)) {
                    return false;
                }
            } else {
                match = match || value.equals(test);
            }
        }
        return match || negated == entries.size();
    }

    /**
     * current-env.js {@code devEngines}: the environment npm runs in, by engine, with {@link #UNKNOWN} where the host
     * does not say; an os has the release the host never gives, and only linux a libc.
     *
     * @param npmVersion the version of the npm that runs
     */
    static Map<String, Object> devEngines(Host host, String npmVersion) {
        String osName = host.getOs();
        // libc(osName): none but on linux, where a machine that reports no libc has none
        Object libc = osName == null ? UNKNOWN : "linux".equals(osName) && host.getLibc() != null ? host.getLibc() :
                Js.UNDEFINED;
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("cpu", currentEngine(host.getCpu() == null ? UNKNOWN : host.getCpu(), Js.UNDEFINED));
        current.put("libc", currentEngine(libc, Js.UNDEFINED));
        current.put("os", currentEngine(osName == null ? UNKNOWN : osName, UNKNOWN));
        current.put("packageManager", currentEngine("npm", npmVersion));
        current.put("runtime", currentEngine("node", host.getNodeVersion() == null ? UNKNOWN : host.getNodeVersion()));
        return current;
    }

    private static Map<String, Object> currentEngine(Object name, Object version) {
        Map<String, Object> engine = new LinkedHashMap<>();
        engine.put("name", name);
        if (version != Js.UNDEFINED) {
            engine.put("version", version);
        }
        return engine;
    }

    // dev-engines.js checkDependency: what one alternative of a devEngines entry does not match in the environment,
    // null where it is satisfied; a shape npm fails on is thrown. semver: the node-semver npm bundles
    private static @Nullable String checkDependency(@Nullable Object wanted, Object current, String engine,
                                                    NodeSemver.Options semver) {
        if (!(wanted instanceof Map)) {
            throw new JsError(JsError.ERROR, "Invalid non-object value for \"" + engine + "\"");
        }

        List<String> properties = Js.keys(wanted);

        for (String prop : properties) {
            if (!RECOGNIZED_PROPERTIES.contains(prop)) {
                throw new JsError(JsError.ERROR, "Invalid property \"" + prop + "\" for \"" + engine + "\"");
            }
        }

        if (!properties.contains("name")) {
            throw new JsError(JsError.ERROR, "Missing \"name\" property for \"" + engine + "\"");
        }

        Object wantedName = Js.get(wanted, "name");
        if (!(wantedName instanceof String)) {
            throw new JsError(JsError.ERROR, "Invalid non-string value for \"name\" within \"" + engine + "\"");
        }

        Object currentName = Js.get(current, "name");
        if (currentName != UNKNOWN && (!(currentName instanceof String) || ((String) currentName).isEmpty())) {
            throw new JsError(JsError.ERROR, "Unable to determine \"name\" for \"" + engine + "\"");
        }

        if (properties.contains("onFail")) {
            Object onFail = Js.get(wanted, "onFail");
            if (!(onFail instanceof String)) {
                throw new JsError(JsError.ERROR, "Invalid non-string value for \"onFail\" within \"" + engine + "\"");
            }
            if (!RECOGNIZED_ON_FAIL.contains(onFail)) {
                throw new JsError(JsError.ERROR, "Invalid onFail value \"" + onFail + "\" for \"" + engine + "\"");
            }
        }

        if (currentName != UNKNOWN && !wantedName.equals(currentName)) {
            return "Invalid name \"" + wantedName + "\" does not match \"" + currentName + "\" for \"" + engine + "\"";
        }

        if (properties.contains("version")) {
            Object wantedVersion = Js.get(wanted, "version");
            Object currentVersion = Js.get(current, "version");
            String fails = !(wantedVersion instanceof String) ?
                    "Invalid non-string value for \"version\" within \"" + engine + "\"" :
                    currentVersion != UNKNOWN &&
                    (!(currentVersion instanceof String) || ((String) currentVersion).isEmpty()) ?
                            "Unable to determine \"version\" for \"" + engine + "\" \"" + wantedName + "\"" : null;
            if (fails != null) {
                if (currentName == UNKNOWN) {
                    // npm fails so only where the name is the one wanted, and reports the other name elsewhere
                    throw new CannotKnow("the " + engine + " npm runs on: where it is " + wantedName + " npm fails (" +
                                         fails + "), and elsewhere devEngines." + engine + " does not match");
                }
                throw new JsError(JsError.ERROR, fails);
            }
            if (currentVersion == UNKNOWN) {
                return null;
            }
            String range = (String) wantedVersion;
            String version = (String) currentVersion;
            if (NodeSemver.validRange(range, semver) != null) {
                if (!NodeSemver.satisfies(version, range, semver)) {
                    return "Invalid semver version \"" + range + "\" does not match \"" + version + "\" for \"" +
                           engine + "\"";
                }
            } else if (!range.equals(version)) {
                return "Invalid version \"" + range + "\" does not match \"" + version + "\" for \"" + engine + "\"";
            }
        }
        return null;
    }

    /**
     * dev-engines.js {@code checkDevEngines}: the engines of a package's {@code devEngines} that {@code current}, the
     * environment by engine, does not satisfy, worded as {@code release} words them.
     *
     * @throws JsError    for a shape npm fails on, with its message
     * @throws CannotKnow where npm fails or not by a value the host does not say
     */
    static List<InvalidDevEngine> checkDevEngines(@Nullable Object wanted, Map<String, ?> current,
                                                  NpmRelease release) {
        if (!(wanted instanceof Map)) {
            throw new JsError(JsError.ERROR, "Invalid non-object value for " +
                                             (release.namesDevEnginesProperty() ? "\"devEngines\"" : "devEngines"));
        }

        List<InvalidDevEngine> errors = new ArrayList<>();

        for (String engine : Js.keys(wanted)) {
            if (!RECOGNIZED_ENGINES.contains(engine)) {
                throw new JsError(JsError.ERROR, "Invalid property \"" +
                                                 (release.namesDevEnginesProperty() ? "devEngines." : "") + engine +
                                                 "\"");
            }
            Object dependencyAsAuthored = ((Map<?, ?>) wanted).get(engine);
            List<Object> dependencies = new ArrayList<>();
            if (dependencyAsAuthored instanceof List) {
                for (Object dependency : (List<?>) dependencyAsAuthored) {
                    if (dependency != Js.HOLE) {
                        dependencies.add(dependency);
                    }
                }
            } else {
                dependencies.add(dependencyAsAuthored);
            }
            Object currentEngine = truthy(current.get(engine)) ? current.get(engine) : new LinkedHashMap<>();

            // this accounts for empty array eg { runtime: [] } and ignores it
            if (dependencies.isEmpty()) {
                continue;
            }

            List<String> depErrors = new ArrayList<>();
            for (Object dep : dependencies) {
                String result = checkDependency(dep, currentEngine, engine,
                        NodeSemver.Options.DEFAULT.release(release.semver()));
                if (result != null) {
                    depErrors.add(result);
                }
            }

            boolean invalid = depErrors.size() == dependencies.size();

            if (invalid) {
                Object lastDependency = dependencies.get(dependencies.size() - 1);
                Object onFail = truthy(Js.get(lastDependency, "onFail")) ? Js.get(lastDependency, "onFail") : "error";
                if ("download".equals(onFail)) {
                    onFail = "error";
                }

                errors.add(new InvalidDevEngine(release.namesDevEnginesProperty() ? "Invalid devEngines." + engine :
                        "Invalid engine \"" + engine + "\"", depErrors, engine, "warn".equals(onFail),
                        "error".equals(onFail), currentEngine, dependencyAsAuthored));
            }
        }
        return errors;
    }
}
