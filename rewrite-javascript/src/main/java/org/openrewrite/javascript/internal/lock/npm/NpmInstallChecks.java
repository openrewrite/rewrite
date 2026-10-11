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
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of npm-install-checks 8.0.0 ({@code checkEngine}, {@code checkPlatform}). The machine's own
 * values come from a {@link Host}; an unknown host satisfies every check.
 */
final class NpmInstallChecks {

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

    private NpmInstallChecks() {
    }

    static void checkEngine(Map<String, Object> target, @Nullable String npmVer, @Nullable String nodeVer,
                            boolean force) {
        String nodev = force ? null : nodeVer;
        Object eng = target.get("engines");
        if (!truthy(eng)) {
            return;
        }
        boolean nodeFail = truthy(nodev) && truthy(Js.get(eng, "node")) && !satisfies(nodev, Js.get(eng, "node"));
        boolean npmFail = truthy(npmVer) && truthy(Js.get(eng, "npm")) && !satisfies(npmVer, Js.get(eng, "npm"));
        if (nodeFail || npmFail) {
            Map<String, @Nullable String> current = new LinkedHashMap<>();
            current.put("node", nodeVer);
            current.put("npm", npmVer);
            throw new UnsupportedEngine(Js.string(target.get("_id")), current, eng);
        }
    }

    private static boolean satisfies(String version, @Nullable Object range) {
        return NodeSemver.satisfies(version, range, NodeSemver.Options.INCLUDE_PRERELEASE);
    }

    /**
     * @param environment the {@code os}, {@code cpu} and {@code libc} the caller overrides, if any
     * @param current     the machine's own
     */
    static void checkPlatform(Map<String, Object> target, boolean force, Host environment, Host current) {
        if (force) {
            return;
        }
        String os = truthy(environment.getOs()) ? environment.getOs() : current.getOs();
        String cpu = truthy(environment.getCpu()) ? environment.getCpu() : current.getCpu();
        // currentEnv.libc(os) detects a libc on linux only
        String libc = truthy(environment.getLibc()) ? environment.getLibc() :
                "linux".equals(os) ? current.getLibc() : null;

        boolean osOk = os == null || !truthy(target.get("os")) || checkList(os, target.get("os"));
        boolean cpuOk = cpu == null || !truthy(target.get("cpu")) || checkList(cpu, target.get("cpu"));
        // a machine that reports an os but no libc has none; an unknown machine's libc is unknown
        boolean libcOk = !truthy(target.get("libc")) ||
                         (libc != null ? checkList(libc, target.get("libc")) : current.getOs() == null);

        if (!osOk || !cpuOk || !libcOk) {
            Map<String, @Nullable String> currentValues = new LinkedHashMap<>();
            currentValues.put("os", os);
            currentValues.put("cpu", cpu);
            currentValues.put("libc", libc);
            Map<String, @Nullable Object> required = new LinkedHashMap<>();
            required.put("os", target.get("os"));
            required.put("cpu", target.get("cpu"));
            required.put("libc", target.get("libc"));
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
}
